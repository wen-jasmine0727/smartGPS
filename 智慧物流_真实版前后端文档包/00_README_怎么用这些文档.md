# 智慧物流 IoT 平台 

---

## 1. 文档目录

```txt
智慧物流_真实版前后端文档包/
├── 00_README_怎么用这些文档.md
├── 01_接口文档.md
├── 02_开发排期.md
├── 03_数据库与消息中间件设计.md
├── 对接/
│   ├── 前后端接口负责人总表.md
│   ├── 前端页面接口清单.md
│   └── 联调验收清单.md
├── 后端/
│   ├── 后端1_基础业务登录车辆货.md
│   ├── 后端1_数据库表结构.md
│   ├── 后端2_MQTT_GPS_Kafka_TimescaleDB_WebSocket.md
│   ├── MQTT_本地调试指南.md
│   └── 后端3_告警调度设备在线RAG部署.md
└── 前端/
    ├── 前端1_货物追踪车辆调度WebSocket.md
    └── 前端2_登录告警车辆管理设备在线问答.md
```

---

## 2. 必须统一的规则

1. **接口字段不能各写各的**：一律按 `01_接口文档.md`。
2. **车牌格式统一**：页面展示用 `沪A·C0291`，MQTT Topic 用 `沪A-C0291`，后端保存两个字段：`plate` 和 `vinTopic`。
3. **时间统一 UTC ISO 8601**：例如 `2026-06-29T06:15:00Z`。
4. **坐标统一 WGS-84**：字段固定为 `lat`、`lng`。
5. **所有接口统一响应**：`code/message/data/timestamp/requestId`。
6. **Day 3 晚上必须打通 MQTT → Kafka → TimescaleDB → Redis → API**，否则暂停其他新功能。
7. **Day 6 不加新功能**，只修 bug、写 README、录屏/演示。

---

## 3. Swagger/Knife4j 网页调试

后端启动后打开：

```txt
http://localhost:8080/doc.html
```

页面应只显示 `智慧物流接口`。先调 `POST /api/v1/auth/login`，拿到 `data.accessToken` 后，在需要登录的接口里填写请求头：

```txt
Authorization: Bearer <accessToken>
```

如果页面打不开，优先检查：

```txt
1. 后端是否启动成功。
2. 是否重新加载 Maven 依赖。
3. application.yml 是否包含 spring.mvc.pathmatch.matching-strategy=ant_path_matcher。
4. 浏览器地址是否是 /doc.html，不是 /api/v1/doc.html。
5. 如果还看到 C 端/员工/分类接口，说明后端没有重启或浏览器缓存了旧页面，重启后端并强制刷新浏览器。
```

---

## 4. 真实版最小主链路

```txt
仓管登录
  → 新增车辆
  → 新建/绑定货物
  → GPS 设备通过 MQTT 上报 vehicle/{vin}/gps
  → EMQX 收到消息
  → Spring Boot MQTT Consumer 接收
  → 写入 Kafka gps-points
  → Kafka Consumer 写入 TimescaleDB gps_points
  → Redis 更新车辆最新位置
  → WebSocket 推给前端地图
  → 偏航/离线触发告警
  → 调度员下发 command
  → 后端通过 MQTT 发布 vehicle/{vin}/command
  → 设备回传 command/ack
  → 前端显示指令已执行
  → 管理员确认/关闭告警
  → 货主使用智能问答查询偏航处理
```

---

## 5. 5 人职责一句话

| 人员 | 一句话职责 |
|---|---|
| 后端 1 | 业务底座：登录、用户、车辆、货物、绑定、状态、业务库 |
| 后端 2 | 实时链路：MQTT GPS、Kafka、TimescaleDB、Redis、WebSocket、ETA |
| 后端 3 | 闭环能力：告警、调度指令、设备在线、MinIO、pgvector/RAG、部署 |
| 前端 1 | 追踪与调度：货物追踪页、车辆总览页、地图、WebSocket、指令弹窗 |
| 前端 2 | 管理与问答：登录、告警中心、车辆货物管理、设备在线、智能问答 |

---

## 6. 交付物

最终提交时至少包含：

```txt
- 前端项目源码
- 后端项目源码
- docker-compose.yml
- 数据库初始化 SQL
- MQTT 测试脚本或 MQTTX 测试说明
- README 启动说明
- 接口文档
- 演示账号
- 演示数据
- 5 分钟演示脚本
```
