# finalspeed_client
为了和surge for mac兼容，去除FinalSpeed中的tcp模式

不再需要管理员权限
java -jar client.jar

client_config.json
```{
  "download_speed": 11200698,
  "server_address": "1.2.3.4",
  "server_port": 150,
  "socks5_port": 1083,
  "upload_speed": 357469
}
```
port_map.json
```
{
  "map_list": [
    {
      "dst_port": 443,
      "listen_port": 2000,
      "name": "ss"
    }
  ]
}
```

论坛 http://www.ip4a.com/c/131.html


