// Copyright (c) 2015 D1SM.net

package net.fs.client;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import net.fs.rudp.Route;
import net.fs.utils.MLog;
import net.fs.utils.Tools;
//import org.pcap4j.core.Pcaps;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.Properties;

public class ClientUI implements ClientUII {

	public static ClientUI ui;
	public boolean osx_fw_pf = false;
	public boolean osx_fw_ipfw = false;
	public boolean isVisible = true;
	MapClient mapClient;
	ClientConfig config = null;
	String name = "FinalSpeed";
	int serverVersion = -1;
	int localVersion = 5;
	boolean checkingUpdate = false;
	String domain = "";
	String homeUrl;
	boolean ky = true;
	boolean capSuccess = false;
	Exception capException = null;
	boolean b1 = false;
	boolean success_firewall_windows = true;
	boolean success_firewall_osx = true;
	String systemName = null;

	String updateUrl;

	boolean min = false;

	// boolean tcpEnable = true;

	{
		domain = "ip4a.com";
		homeUrl = "http://www.ip4a.com/?client_fs";
		updateUrl = "http://fs.d1sm.net/finalspeed/update.properties";
	}

	ClientUI() {

		systemName = System.getProperty("os.name").toLowerCase();
		MLog.info("System: " + systemName + " " + System.getProperty("os.version"));
		ui = this;
		// checkQuanxian();
		loadConfig();
		MLog.println("FinalSpeed 1.2");

		setMessage(" ");

		if (config.getRemoteAddress() != null && !config.getRemoteAddress().equals("") && config.getRemotePort() > 0) {
			String remoteAddressTxt = config.getRemoteAddress() + ":" + config.getRemotePort();
			MLog.println(remoteAddressTxt);
		}

		checkFireWallOn();

		try {
			mapClient = new MapClient(this);
		} catch (final Exception e1) {
			e1.printStackTrace();
			capException = e1;
			// System.exit(0);
		}

		mapClient.setUi(this);

		// mapClient.setMapServer(config.getServerAddress(),
		// config.getServerPort(), config.getRemotePort(), null, null,
		// config.isDirect_cn(), config.getProtocal().equals("tcp"), null);
		mapClient.setMapServer(config.getServerAddress(), config.getServerPort(), config.getRemotePort(), null, null,
				config.isDirect_cn(), null);

		Route.es.execute(new Runnable() {

			@Override
			public void run() {
				checkUpdate();
			}
		});
		setSpeed(config.getDownloadSpeed(), config.getUploadSpeed());
	}

	public static String readFileUtf8(String path) throws Exception {
		String str = null;
		FileInputStream fis = null;
		DataInputStream dis = null;
		try {
			File file = new File(path);

			int length = (int) file.length();
			byte[] data = new byte[length];

			fis = new FileInputStream(file);
			dis = new DataInputStream(fis);
			dis.readFully(data);
			str = new String(data, "utf-8");

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (dis != null) {
				try {
					dis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return str;
	}

	String getServerAddressFromConfig() {
		String server_addressTxt = config.getServerAddress();
		if (config.getServerAddress() != null && !config.getServerAddress().equals("")) {
			if (config.getServerPort() != 150 && config.getServerPort() != 0) {
				server_addressTxt += (":" + config.getServerPort());
			}
		}
		return server_addressTxt;
	}

	void checkFireWallOn() {
		if (systemName.contains("os x")) {
			String runFirewall = "ipfw";
			try {
				final Process p = Runtime.getRuntime().exec(runFirewall, null);
				osx_fw_ipfw = true;
			} catch (IOException e) {
				// e.printStackTrace();
			}
			runFirewall = "pfctl";
			try {
				final Process p = Runtime.getRuntime().exec(runFirewall, null);
				osx_fw_pf = true;
			} catch (IOException e) {
				// e.printStackTrace();
			}
			success_firewall_osx = osx_fw_ipfw | osx_fw_pf;
		} else if (systemName.contains("linux")) {
			String runFirewall = "service iptables start";
		} else if (systemName.contains("windows")) {
			String runFirewall = "netsh advfirewall set allprofiles state on";
			Thread standReadThread = null;
			Thread errorReadThread = null;
			try {
				final Process p = Runtime.getRuntime().exec(runFirewall, null);
				standReadThread = new Thread() {
					public void run() {
						InputStream is = p.getInputStream();
						BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
						while (true) {
							String line;
							try {
								line = localBufferedReader.readLine();
								if (line == null) {
									break;
								} else {
									if (line.contains("Windows")) {
										success_firewall_windows = false;
									}
								}
							} catch (IOException e) {
								e.printStackTrace();
								// error();
								exit();
								break;
							}
						}
					}
				};
				standReadThread.start();

				errorReadThread = new Thread() {
					public void run() {
						InputStream is = p.getErrorStream();
						BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
						while (true) {
							String line;
							try {
								line = localBufferedReader.readLine();
								if (line == null) {
									break;
								} else {
									System.out.println("error" + line);
								}
							} catch (IOException e) {
								e.printStackTrace();
								// error();
								exit();
								break;
							}
						}
					}
				};
				errorReadThread.start();
			} catch (IOException e) {
				e.printStackTrace();
				success_firewall_windows = false;
				// error();
			}

			if (standReadThread != null) {
				try {
					standReadThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (errorReadThread != null) {
				try {
					errorReadThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	void setSpeed(int downloadSpeed, int uploadSpeed) {
		config.setDownloadSpeed(downloadSpeed);
		config.setUploadSpeed(uploadSpeed);
		Route.localDownloadSpeed = downloadSpeed;
		Route.localUploadSpeed = config.getUploadSpeed();

	}

	void exit() {
		System.exit(0);
	}

	public void setMessage(String message) {
		MLog.println("状态: " + message);
	}

	ClientConfig loadConfig() {
		ClientConfig cfg = new ClientConfig();

		try {
			String content = readFileUtf8(cfg.getConfigFilePath());
			JSONObject json = JSONObject.parseObject(content);
			cfg.setServerAddress(json.getString("server_address"));
			cfg.setServerPort(json.getIntValue("server_port"));
			cfg.setRemotePort(json.getIntValue("remote_port"));
			cfg.setRemoteAddress(json.getString("remote_address"));
			if (json.containsKey("direct_cn")) {
				cfg.setDirect_cn(json.getBooleanValue("direct_cn"));
			}
			cfg.setDownloadSpeed(json.getIntValue("download_speed"));
			cfg.setUploadSpeed(json.getIntValue("upload_speed"));
			if (json.containsKey("socks5_port")) {
				cfg.setSocks5Port(json.getIntValue("socks5_port"));
			}
			// if (json.containsKey("protocal")) {
			// cfg.setProtocal(json.getString("protocal"));
			// cfg.setProtocal("udp");
			// }
			if (json.containsKey("auto_start")) {
				cfg.setAutoStart(json.getBooleanValue("auto_start"));
			}
			if (json.containsKey("recent_address_list")) {
				JSONArray list = json.getJSONArray("recent_address_list");
				for (int i = 0; i < list.size(); i++) {
					cfg.getRecentAddressList().add(list.get(i).toString());
				}
			}

			config = cfg;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cfg;
	}

	public void updateUISpeed(int conn, int downloadSpeed, int uploadSpeed) {
		String string = " 下载:" + Tools.getSizeStringKB(downloadSpeed) + "/s" + " 上传:"
				+ Tools.getSizeStringKB(uploadSpeed) + "/s";
		if (!Tools.getSizeStringKB(downloadSpeed).equals("0") & !Tools.getSizeStringKB(uploadSpeed).equals("0")) {
			MLog.println(string);
		}
	}

	boolean haveNewVersion() {
		return serverVersion > localVersion;
	}

	public void checkUpdate() {
		for (int i = 0; i < 3; i++) {
			checkingUpdate = true;
			try {
				Properties propServer = new Properties();
				HttpURLConnection uc = Tools.getConnection(updateUrl);
				uc.setUseCaches(false);
				InputStream in = uc.getInputStream();
				propServer.load(in);
				serverVersion = Integer.parseInt(propServer.getProperty("version"));
				break;
			} catch (Exception e) {
				e.printStackTrace();
				try {
					Thread.sleep(3 * 1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} finally {
				checkingUpdate = false;
			}
		}
		if (this.haveNewVersion()) {
			MLog.println("发现新版本,立即更新吗?");
			MLog.println(homeUrl);
		}

	}

	@Override
	public boolean login() {
		return false;
	}

	@Override
	public boolean updateNode(boolean testSpeed) {
		return true;

	}

	public boolean isOsx_fw_pf() {
		return osx_fw_pf;
	}

	public void setOsx_fw_pf(boolean osx_fw_pf) {
		this.osx_fw_pf = osx_fw_pf;
	}

	public boolean isOsx_fw_ipfw() {
		return osx_fw_ipfw;
	}

	public void setOsx_fw_ipfw(boolean osx_fw_ipfw) {
		this.osx_fw_ipfw = osx_fw_ipfw;
	}

	public void setVisible(boolean visible) {
		this.isVisible = visible;
	}
}
