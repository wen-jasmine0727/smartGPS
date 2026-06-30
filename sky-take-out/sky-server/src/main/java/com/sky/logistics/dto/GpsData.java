package com.sky.logistics.dto;

import lombok.Data;

/**
 * MQTT GPS 数据 DTO，对应 vehicle/{vin}/gps 主题的 JSON 载荷
 *
 * <pre>
 * {
 *   "imei": "861234567890123",
 *   "ts": 1719634500,
 *   "lat": 30.4219,
 *   "lng": 120.5738,
 *   "speed": 68,
 *   "heading": 225,
 *   "accuracy": 3.5
 * }
 * </pre>
 */
@Data
public class GpsData {

    /** 设备 IMEI */
    private String imei;

    /** Unix 时间戳（秒） */
    private Long ts;

    /** 纬度 */
    private Double lat;

    /** 经度 */
    private Double lng;

    /** 速度 km/h */
    private Integer speed;

    /** 航向角 0-360 */
    private Integer heading;

    /** GPS 精度（米） */
    private Double accuracy;

    /**
     * MQTT 主题中解析出的 vinTopic，不在 JSON 中，由接收器填入
     */
    private String vinTopic;
}
