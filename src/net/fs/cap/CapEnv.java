// Copyright (c) 2015 D1SM.net

package net.fs.cap;

import java.net.Inet4Address;
import java.util.HashMap;
import java.util.Random;

import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.util.MacAddress;

public class CapEnv {

	public MacAddress gateway_mac;

	public MacAddress local_mac;

	Inet4Address local_ipv4;

	public PcapHandle sendHandle;

	VDatagramSocket vDatagramSocket;

	String testIp_tcp = "";

	String testIp_udp = "5.5.5.5";

	String selectedInterfaceName = null;

	String selectedInterfaceDes = "";

	PcapNetworkInterface nif;

	HashMap<Integer, TCPTun> tunTable = new HashMap<Integer, TCPTun>();

	Random random = new Random();

	boolean client = false;

	short listenPort;

	TunManager tcpManager = null;

	CapEnv capEnv;

	Thread versinMonThread;

	public boolean fwSuccess = true;

	boolean ppp = false;

	{
		capEnv = this;
	}

	public CapEnv(boolean isClient, boolean fwSuccess) {
		this.client = isClient;
		this.fwSuccess = fwSuccess;
		tcpManager = new TunManager(this);
	}

	public void createTcpTun_Client(String dstAddress, short dstPort) throws Exception {
		Inet4Address serverAddress = (Inet4Address) Inet4Address.getByName(dstAddress);
		TCPTun conn = new TCPTun(this, serverAddress, dstPort, local_mac, gateway_mac);
		tcpManager.addConnection_Client(conn);
		boolean success = false;
		for (int i = 0; i < 6; i++) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (conn.preDataReady) {
				success = true;
				break;
			}
		}
		if (success) {
			tcpManager.setDefaultTcpTun(conn);
		} else {
			tcpManager.removeTun(conn);
			tcpManager.setDefaultTcpTun(null);
			throw new Exception("创建隧道失败!");
		}
	}

	public static int toUnsigned(short s) {
		return s & 0x0FFFF;
	}

}
