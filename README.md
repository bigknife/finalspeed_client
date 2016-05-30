# finalspeed_client
为了和surge for mac兼容，去除FinalSpeed中的tcp模式

`java -jar client.jar`

## FinalSpeed 配置
finalspeed.json
```
{
  "map_list": [
    {
      "dst_port": 443,
      "listen_port": 2000,
      "name": "ss"
    }
  ],
  "download_speed": 10485760,
  "server_address": "1.2.3.4",
  "server_port": 150,
  "socks5_port": 1083,
  "upload_speed": 4766254
}
```

## surge for mac 配置
Proxy = custom,127.0.0.1,2000,method,password,...

## Finalspeed.app
mv Finalspeed.app /Applications
mv finalspeed.json ~/.finalspeed.json