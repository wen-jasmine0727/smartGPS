# 后端 2 · MQTT、GPS、Kafka、TimescaleDB、Redis、WebSocket、ETA

## 1. 你的定位

你是实时链路负责人。项目能不能体现“智慧物流 IoT”，主要看你的链路是否真实打通。

核心链路：

```txt
MQTT GPS → Spring MQTT Consumer → Kafka gps-points → TimescaleDB gps_points → Redis latest position → API/WebSocket → 前端地图
```

---

## 2. 负责范围

### 必须负责

- EMQX 接入。
- Spring Integration MQTT 或 MQTT Client。
- 订阅 `vehicle/+/gps`。
- 订阅 `vehicle/+/heartbeat`，至少先接收并发给 Kafka。
- GPSData 校验。
- Kafka Producer：`gps-points`、`vehicle-heartbeats`。
- Kafka Consumer：GPS 入 TimescaleDB。
- Redis 保存车辆最新位置。
- `/cargo/{cargoId}/position`。
- `/cargo/{cargoId}/trajectory`。
- `/cargo/{cargoId}/eta`。
- `/cargo/{cargoId}/timeline`。
- WebSocket `/ws`。
- 推送 `vehicle.position`。
- 给后端 3 提供当前位置/速度/最后更新时间。

### 不负责

- 不负责车辆新增。
- 不负责货物绑定。
- 不负责告警处理状态。
- 不负责 RAG。
- 不负责 command 下发。

---

## 3. MQTT Topic

| Topic | 方向 | 你要做什么 |
|---|---|---|
| `vehicle/{vin}/gps` | Device → Cloud | 主负责：接收 GPS |
| `vehicle/{vin}/heartbeat` | Device → Cloud | 接收后写 Kafka/Redis，后端 3 用于在线状态 |

### GPSData

```json
{
  "imei": "861234567890123",
  "ts": 1719634500,
  "lat": 30.4219,
  "lng": 120.5738,
  "speed": 68,
  "heading": 225,
  "accuracy": 3.5
}
```

说明：`ts` 是设备上报时间戳，当前后端写入 TimescaleDB `gps_points.time` 时使用服务端接收时间，便于本地调试时每次发送都生成新的轨迹时间。

### Heartbeat

```json
{
  "imei": "861234567890123",
  "ts": 1719634500,
  "battery": 85,
  "signal": 4,
  "gnssSatellites": 12,
  "temp": 42
}
```

---

## 4. Kafka Topic

| Topic | 生产者 | 消费者 |
|---|---|---|
| `gps-points` | MQTT Consumer | GPS Persistence Consumer |
| `vehicle-heartbeats` | MQTT Consumer | Device Status Consumer / 后端 3 |

---

## 5. 你要实现的接口

| 方法 | 路径 | 说明 | 优先级 |
|---|---|---|---|
| GET | `/cargo/{cargoId}/position` | 最新位置，来自 Redis | P0 |
| GET | `/cargo/{cargoId}/trajectory` | 历史轨迹，来自 TimescaleDB | P0 |
| GET | `/cargo/{cargoId}/eta` | ETA 计算 | P0 |
| GET | `/cargo/{cargoId}/timeline` | 运输时间线 | P1 |
| WebSocket | `/ws` | 实时位置推送 | P0 |

---

## 6. 内部处理流程

### 6.1 GPS 消息处理

```txt
1. 收到 MQTT topic: vehicle/沪A-C0291/gps
2. 解析 vinTopic = 沪A-C0291
3. 调后端 1 service 查 vehicle
4. 校验 imei 是否一致
5. 查当前 active cargo
6. 组装 GpsPointEvent
7. 发送 Kafka gps-points
8. Consumer 写 TimescaleDB
9. Redis 更新 latest
10. WebSocket 推送 vehicle.position
11. 给后端 3 告警引擎提供输入
```

### 6.2 Redis 最新位置

Key：`logistics:vehicle:latest:{plate}`

```json
{
  "plate": "沪A·C0291",
  "cargoId": "SH-HZ-20260629-0291",
  "lat": 30.4219,
  "lng": 120.5738,
  "speed": 68,
  "heading": 225,
  "accuracy": 3.5,
  "updatedAt": "2026-06-29T06:15:00Z"
}
```

---

## 7. ETA 规则

6 天内不接复杂地图 API，真实数据来源是 GPS，ETA 公式可简化：

```txt
remainingMinutes = distanceRemaining / max(currentSpeed, 30) * 60 * roadFactor
```

| 路段 | roadFactor |
|---|---:|
| 高速 | 1.0 |
| 国道 | 1.2 |
| 城市道路 | 1.5 |

距离可用起终点与当前位置的球面距离近似计算。

---

## 8. 6 天任务

### Day 1

- EMQX 连接测试。
- Kafka topic 创建。
- TimescaleDB gps_points 建表。
- Redis 连接测试。
- MQTT Consumer 空壳。

验收：MQTTX 发消息，后端日志能看到原始 payload。

### Day 2

- GPS MQTT → Kafka → TimescaleDB。
- Redis 最新位置。
- `/position`。

验收：MQTTX 发 GPS 后，接口返回该坐标。

### Day 3

- `/trajectory`。
- `/eta`。
- WebSocket 推送。

验收：连续发 3 个 GPS 点，前端地图实时移动，轨迹有 3 个点。

### Day 4

- `/timeline`。
- 给后端 3 暴露当前位置、速度、停留时间查询。
- heartbeat 数据稳定传给后端 3。

验收：设备在线和偏航告警可以使用你的数据。

### Day 5

- 稳定性处理：重复消息、非法坐标、Kafka 断连、Redis 空值。
- 和前端 1 联调地图。

### Day 6

- 不加新功能。
- 准备 MQTTX GPS 演示 payload。
- 保障主链路连续跑通 3 遍。

---

## 9. 完成标准

- EMQX 能真实接收 GPS。
- 后端能真实订阅 MQTT。
- Kafka 有真实 gps-points。
- TimescaleDB 有真实 gps_points。
- Redis 有最新位置。
- `/position`、`/trajectory`、`/eta` 可用。
- WebSocket 能推给前端。

---

## 10. 避坑

1. 不要再写“模拟 GPS 定时器”当主链路，真实链路必须先通。
2. MQTT Topic 里的车牌不要用 `·`，统一 `-`。
3. GPS 时间戳 `ts` 是秒，需要转 UTC 时间。
4. Kafka 消费重复时要能幂等或至少不崩。
5. Redis 没数据时，要从 TimescaleDB 查最后一个点兜底。
