# MQTT 本地调试指南

这份文档用于本地调试智慧物流项目的 MQTT 链路。重点是让你能看懂 EMQX 网页、用 MQTTX 模拟 GPS 设备发消息，并判断消息有没有进入后端、Kafka、TimescaleDB 和 Redis。

---

## 1. 角色说明

| 名称 | 作用 | 本地地址 |
|---|---|---|
| EMQX | MQTT 服务器，负责接收和转发设备消息 | `mqtt://localhost:1883` |
| EMQX Dashboard | EMQX 的网页管理后台 | `http://localhost:18083` |
| MQTTX | MQTT 客户端，用来模拟设备发 GPS/心跳 | 单独的软件 |
| Spring Boot 后端 | 订阅 MQTT，收到消息后写 Kafka | `localhost:8080` |
| Kafka | 消息队列，保存 GPS/心跳消息 | `localhost:9092` |
| TimescaleDB | 保存 GPS 历史轨迹点 | `localhost:5433/gps` |
| Redis | 保存车辆最新位置 | `localhost:6379` |

一句话：

```txt
MQTTX 模拟设备
  -> EMQX 接收消息
  -> Spring Boot 订阅消息
  -> Kafka 转发
  -> TimescaleDB/Redis 落库
```

---

## 2. 启动本地基础设施

在项目根目录执行：

```bash
cd /home/kaze123/Project/smartGPS
sudo docker compose up -d emqx kafka kafka-init redis timescaledb
```

查看容器：

```bash
sudo docker ps
```

检查端口：

```bash
ss -ltnp | grep -E '1883|18083|9092|6379|5433'
```

正常应该至少看到：

```txt
1883   MQTT 协议端口
18083  EMQX 网页端口
9092   Kafka
6379   Redis
5433   TimescaleDB
```

---

## 3. EMQX 网页怎么用

打开：

```txt
http://localhost:18083/#/dashboard/overview
```

默认账号：

```txt
admin
public
```

### 3.1 Overview

看整体状态：

```txt
Connections
Sessions
Subscriptions
Messages
```

如果 Spring Boot 后端已经连上 MQTT，`Connections` 应该至少有 `1`。

### 3.2 Clients

这里看当前连接到 EMQX 的客户端。

后端启动成功后，通常能看到类似：

```txt
backend-realtime-service-01-inbound
```

MQTTX 连接成功后，也会多一个你自己设置的 Client ID，例如：

```txt
mqttx-test-001
```

### 3.3 Subscriptions

这里看客户端订阅了哪些 Topic。

后端启动成功后，应该能看到：

```txt
vehicle/+/gps
vehicle/+/heartbeat
```

`+` 是 MQTT 单层通配符，所以这些 Topic 都能匹配：

```txt
vehicle/沪A-C0291/gps
vehicle/粤B-88888/gps
vehicle/test-car/heartbeat
```

---

## 4. MQTTX 怎么打开

MQTTX 是独立软件，不是在 EMQX 网页里打开。

先在系统应用菜单搜索：

```txt
MQTTX
```

也可以终端尝试：

```bash
mqttx
```

如果出现 Wayland 报错：

```txt
Failed to initialize Wayland platform
segmentation fault
```

用 X11 模式启动：

```bash
ELECTRON_OZONE_PLATFORM_HINT=x11 mqttx
```

如果还不行：

```bash
mqttx --ozone-platform=x11 --disable-gpu
```

AppImage 版本可以这样：

```bash
./MQTTX.AppImage --ozone-platform=x11 --disable-gpu --no-sandbox
```

---

## 5. MQTTX 新建连接

打开 MQTTX 后点击：

```txt
New Connection
```

填写：

| 字段 | 值 |
|---|---|
| Name | `smartGPS-local` |
| Client ID | `mqttx-test-001` |
| Host | `localhost` |
| Port | `1883` |
| Username | 留空 |
| Password | 留空 |
| SSL/TLS | 关闭 |

协议选择：

```txt
mqtt://
```

完整连接地址等价于：

```txt
mqtt://localhost:1883
```

点击 `Connect`。连接成功后，左侧连接会变绿。

---

## 6. 发布 GPS 消息

在 MQTTX 的消息发送区填写：

Topic：

```txt
vehicle/沪A-C0291/gps
```

Payload：

```json
{
  "imei": "861234567890123",
  "lat": 31.2304,
  "lng": 121.4737,
  "speed": 60,
  "heading": 90,
  "accuracy": 8,
  "ts": 1782867600
}
```

QoS 建议选：

```txt
1
```

点击：

```txt
Send
```

注意：

- Topic 里用 `沪A-C0291`，不要用展示车牌 `沪A·C0291`。
- 数据库 `vehicles.vin_topic` 必须能查到 `沪A-C0291`。
- `imei` 最好和车辆表里的 `device_imei` 一致。
- `ts` 是秒级时间戳，不是毫秒。

---

## 7. 发布心跳消息

Topic：

```txt
vehicle/沪A-C0291/heartbeat
```

Payload：

```json
{
  "imei": "861234567890123",
  "battery": 90,
  "signal": 85,
  "gnssSatellites": 12,
  "temp": 42,
  "ts": 1782867600
}
```

发送后，后端会把心跳消息转发到 Kafka：

```txt
vehicle-heartbeats
```

---

## 8. 后端日志怎么看

发送 GPS 后，Spring Boot 控制台应该看到：

```txt
MQTT 收到消息，主题: vehicle/沪A-C0291/gps
GPS 已发送 Kafka, vinTopic=沪A-C0291
```

如果是心跳：

```txt
MQTT 收到消息，主题: vehicle/沪A-C0291/heartbeat
心跳已发送 Kafka, vinTopic=沪A-C0291
```

如果看到：

```txt
收到没有 MQTT topic header 的消息
```

说明 MQTT 消息 header 没取到。当前代码已经兼容：

```txt
mqtt_receivedTopic
mqtt_topic
```

相关代码：

```txt
sky-take-out/sky-server/src/main/java/com/sky/logistics/service/MqttMessageReceiver.java
```

---

## 9. Kafka 怎么验证

查看 Topic：

```bash
sudo docker exec -it logistics-kafka kafka-topics --bootstrap-server localhost:29092 --list
```

应该看到：

```txt
gps-points
vehicle-heartbeats
```

查看 GPS 消息：

```bash
sudo docker exec -it logistics-kafka kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic gps-points \
  --from-beginning \
  --max-messages 1
```

查看心跳消息：

```bash
sudo docker exec -it logistics-kafka kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic vehicle-heartbeats \
  --from-beginning \
  --max-messages 1
```

---

## 10. TimescaleDB 怎么验证

进入 TimescaleDB：

```bash
sudo docker exec -it logistics-timescaledb psql -U postgres -d gps
```

查看 GPS 点：

```sql
SELECT time, vehicle_id, cargo_id, imei, lat, lng, speed
FROM gps_points
ORDER BY time DESC
LIMIT 5;
```

直接一条命令查看：

```bash
sudo docker exec -it logistics-timescaledb psql -U postgres -d gps \
  -c "SELECT time, vehicle_id, cargo_id, imei, lat, lng, speed FROM gps_points ORDER BY time DESC LIMIT 5;"
```

如果没有数据，按顺序检查：

```txt
1. 后端有没有收到 MQTT 消息。
2. Kafka gps-points 有没有消息。
3. GpsKafkaConsumer 有没有报错。
4. vinTopic 是否能在 vehicles 表查到。
5. TimescaleDB 是否已经创建 gps_points 表。
```

---

## 11. Redis 怎么验证

查看车辆最新位置：

```bash
sudo docker exec -it logistics-redis redis-cli get 'logistics:vehicle:latest:沪A·C0291'
```

如果有数据，会看到类似：

```json
{
  "plate": "沪A·C0291",
  "lat": 31.2304,
  "lng": 121.4737,
  "speed": 60,
  "heading": 90,
  "accuracy": 8,
  "updatedAt": "2026-07-01T02:50:00Z"
}
```

如果返回空：

```txt
(nil)
```

说明 GPS 消息还没有成功进入 Kafka Consumer，或者 Redis 更新逻辑没有执行。

---

## 12. 常见问题

### 12.1 MQTT 连接被拒绝

报错：

```txt
Connection refused
```

检查 EMQX 是否启动：

```bash
sudo docker compose up -d emqx
ss -ltnp | grep 1883
```

### 12.2 EMQX 页面能打开，但后端没有连接

检查后端配置：

```yaml
mqtt:
  broker-url: tcp://localhost:1883
  client-id: backend-realtime-service-01
```

然后重启 Spring Boot。

### 12.3 EMQX Subscriptions 看不到订阅

后端应该订阅：

```txt
vehicle/+/gps
vehicle/+/heartbeat
```

如果看不到，说明后端 MQTT 入站 Adapter 没启动或连接失败。

相关代码：

```txt
sky-take-out/sky-server/src/main/java/com/sky/config/MqttConfig.java
```

### 12.4 MQTTX 发了，但后端说未知 Topic

检查 Topic 是否写错。

正确：

```txt
vehicle/沪A-C0291/gps
vehicle/沪A-C0291/heartbeat
```

错误示例：

```txt
vehicles/沪A-C0291/gps
vehicle/沪A·C0291/gps
vehicle/沪A-C0291/location
```

### 12.5 后端收到 GPS，但丢弃

常见原因：

```txt
lat/lng 为空
lat 超过 -90 到 90
lng 超过 -180 到 180
JSON 格式错误
```

### 12.6 Kafka 连接失败

启动 Kafka：

```bash
sudo docker compose up -d kafka kafka-init
```

检查端口：

```bash
ss -ltnp | grep 9092
```

### 12.7 TimescaleDB jdbcUrl 报错

报错：

```txt
jdbcUrl is required with driverClassName
```

检查配置必须是：

```yaml
timescaledb:
  datasource:
    jdbc-url: jdbc:postgresql://localhost:5433/gps
```

不是：

```yaml
url: jdbc:postgresql://localhost:5433/gps
```

### 12.8 GPS 写入没有 cargoId

如果 `gps_points.cargo_id` 为空，说明后端还没有按车辆查询当前绑定货物，或者该车辆当前没有绑定货物。

需要先完成后端 1 的货物绑定：

```txt
POST /api/v1/cargo/bind
```

请求示例：

```json
{
  "cargoId": "SH-HZ-20260629-0291",
  "vehicleId": 1
}
```

---

## 13. 最小验收流程

按这个顺序做一遍：

```txt
1. sudo docker compose up -d emqx kafka kafka-init redis timescaledb
2. 启动 Spring Boot 后端
3. 打开 http://localhost:18083
4. Clients 看到 backend-realtime-service-01-inbound
5. Subscriptions 看到 vehicle/+/gps 和 vehicle/+/heartbeat
6. MQTTX 连接 mqtt://localhost:1883
7. MQTTX 发布 vehicle/沪A-C0291/gps
8. 后端日志看到 MQTT 收到消息
9. Kafka gps-points 能消费到消息
10. TimescaleDB gps_points 有数据
11. Redis latest key 有数据
```

能跑完这 11 步，说明本地 MQTT 主链路已经打通。
