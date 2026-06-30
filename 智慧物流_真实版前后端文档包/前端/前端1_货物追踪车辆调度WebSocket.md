# 前端 1 · 货物追踪、车辆调度、地图、WebSocket

## 1. 你的定位

你负责最核心的演示页面：货主看货物在哪里，调度员看全队车辆并下发指令。

负责页面：

```txt
01_shipper_cargo_tracking.html → Vue 页面：货物追踪工作台
02_dispatcher_fleet_overview.html → Vue 页面：车辆调度总览台
```

---

## 2. 技术栈

- Vue 3
- Vite
- TypeScript
- Naive UI
- Leaflet
- ECharts
- Pinia
- Axios
- native WebSocket

---

## 3. 货物追踪页功能

### 3.1 必须做

- 货物详情卡片。
- 当前车辆位置地图 marker。
- 车辆实时位置通过 WebSocket 刷新。
- ETA 卡片。
- 进度条。
- 轨迹折线。
- 运输时间线。
- 告警提示。

### 3.2 接口

| 功能 | 接口/协议 |
|---|---|
| 货物详情 | `GET /cargo/{cargoId}` |
| 实时位置兜底 | `GET /cargo/{cargoId}/position` |
| WebSocket 位置 | `vehicle.position` |
| 轨迹 | `GET /cargo/{cargoId}/trajectory` |
| ETA | `GET /cargo/{cargoId}/eta` |
| 时间线 | `GET /cargo/{cargoId}/timeline` |
| 告警推送 | `alert.triggered` |

### 3.3 页面数据结构

```ts
interface VehiclePosition {
  plate: string
  lat: number
  lng: number
  speed: number
  heading: number
  timestamp: string
}
```

---

## 4. 车辆调度总览页功能

### 4.1 必须做

- 全队车辆地图。
- 车辆列表。
- 状态筛选：全部、行驶、停靠、离线。
- 车牌/司机搜索。
- 点击车辆展示详情。
- 下发调度指令弹窗。
- 显示指令状态：SENT / RECEIVED / EXECUTED / REJECTED。

### 4.2 接口

| 功能 | 接口/协议 |
|---|---|
| 车辆列表 | `GET /vehicles` |
| 车辆详情 | `GET /vehicles/{id}` |
| 下发指令 | `POST /vehicles/{plate}/command` |
| 指令状态 | `GET /vehicles/{plate}/command/{commandId}` |
| WebSocket 指令回执 | `command.ack` |

---

## 5. WebSocket 处理

连接：

```ts
const ws = new WebSocket(`${WS_BASE}/ws?token=${token}`)
```

订阅车辆：

```json
{
  "action": "SUBSCRIBE",
  "channel": "vehicle.position",
  "plates": ["*"]
}
```

收到位置：

```json
{
  "channel": "vehicle.position",
  "data": {
    "plate": "沪A·C0291",
    "lat": 30.4210,
    "lng": 120.5723,
    "speed": 72,
    "heading": 225,
    "timestamp": "2026-06-29T06:15:30Z"
  }
}
```

前端处理要求：

```txt
1. 按 plate 更新地图 marker。
2. 如果当前追踪货物绑定的是该 plate，同步更新货物追踪页。
3. WebSocket 断开时，自动重连。
4. 重连期间用 /position 或 /vehicles 轮询兜底。
```

---

## 6. 6 天任务

### Day 1

- Vue 项目搭建。
- 路由建立。
- 迁移两个 HTML 页面结构。
- Axios 封装。
- Pinia store：auth、vehicle、tracking。

### Day 2

- 接 `/auth/login` 的 token 使用。
- 接 `/cargo/{cargoId}`、`/position`。
- 接 `/vehicles`。
- 页面先能展示真实接口数据。

### Day 3

- Leaflet 地图。
- WebSocket 接 `vehicle.position`。
- 轨迹折线。
- ETA 和时间线。

### Day 4

- 调度指令弹窗。
- 下发指令接口。
- command.ack 实时更新。
- 告警推送提示。

### Day 5

- 全页面联调。
- 加加载状态、错误提示、空状态。
- 地图 marker 优化。

### Day 6

- 不加新功能。
- 修演示问题。
- 和后端 2/3 连续跑 3 遍。

---

## 7. 完成标准

- MQTTX 发 GPS 后，地图车辆位置会变。
- 轨迹来自后端 TimescaleDB 查询结果。
- ETA 来自接口。
- 调度指令真实调用后端，状态由 ack 改变。
- WebSocket 断了能重连或轮询兜底。

---

## 8. 避坑

1. 不要把坐标写死在前端。
2. 不要自己模拟 WebSocket 数据。
3. 不要用 `latitude/longitude`，统一 `lat/lng`。
4. 车牌作为 marker key。
5. 后端字段没给时，先找后端，不要自己造字段。
