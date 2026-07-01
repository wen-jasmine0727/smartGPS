package com.sky.logistics.service;

import java.util.Map;

/**
 * 货物追踪服务 —— 后端2 核心接口
 */
public interface TrackingService {

    /**
     * 获取货物当前位置（来自 Redis 最新 GPS）
     */
    Map<String, Object> getPosition(String cargoId);

    /**
     * 获取货物历史轨迹（来自 TimescaleDB gps_points）
     */
    Map<String, Object> getTrajectory(String cargoId);

    /**
     * 计算 ETA 预计到达时间
     */
    Map<String, Object> getEta(String cargoId);

    /**
     * 获取运输时间线
     */
    Map<String, Object> getTimeline(String cargoId);
}
