package com.sky.logistics.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * GPS 轨迹点实体，对应 TimescaleDB gps_points 超表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GpsPoint {

    /** UTC 时间戳 */
    private Instant time;

    /** 车辆 vinTopic（如 沪A-C0291） */
    private String vehicleId;

    /** 当前运输的货物 ID */
    private String cargoId;

    /** 设备 IMEI */
    private String imei;

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
}
