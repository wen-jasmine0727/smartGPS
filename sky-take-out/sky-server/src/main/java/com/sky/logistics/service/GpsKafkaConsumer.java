package com.sky.logistics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.logistics.dto.GpsData;
import com.sky.logistics.entity.Vehicle;
import com.sky.logistics.mapper.LogisticsCargoMapper;
import com.sky.logistics.mapper.LogisticsVehicleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GPS 数据 Kafka 消费者
 *
 * 消费链路：
 * Kafka gps-points → TimescaleDB gps_points → Redis latest position
 */
@Service
@Slf4j
public class GpsKafkaConsumer {
    @Autowired
    private LogisticsCargoMapper cargoMapper;
    @Autowired
    @Qualifier("timescaleJdbcTemplate")
    private JdbcTemplate timescaleJdbc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private LogisticsVehicleMapper vehicleMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 消费 gps-points 主题，写入 TimescaleDB 并更新 Redis
     */
    @KafkaListener(topics = "gps-points", groupId = "logistics-consumer-group")
    public void consume(String message) {
        try {
            GpsData gps = objectMapper.readValue(message, GpsData.class);
            String vinTopic = gps.getVinTopic();
            if (vinTopic == null) {
                log.warn("Kafka 消息缺少 vinTopic，丢弃");
                return;
            }

            // 1. 查找车辆信息
            Vehicle vehicle = vehicleMapper.findByVinTopic(vinTopic);
            if (vehicle == null) {
                log.warn("未找到 vinTopic 对应车辆，丢弃 GPS, vinTopic={}", vinTopic);
                return;
            }

            String plate = vehicle.getPlate();

            // 校验 IMEI
            if (gps.getImei() != null && !gps.getImei().equals(vehicle.getDeviceImei())) {
                log.warn("IMEI 不匹配, 消息 imei={}, 车辆 imei={}", gps.getImei(), vehicle.getDeviceImei());
                // 不丢弃，仅记录
            }

            // 2. 写入 TimescaleDB gps_points 表。入库时间使用服务端接收时间，避免测试消息复用固定 ts 导致轨迹时间不变化。
            Instant ts = Instant.now();

            // 一车多货时，同一个 GPS 点为每个正在绑定的货物写一条轨迹。
            List<String> cargoIds = cargoMapper.findActiveCargoIdsByVehicleId(vehicle.getId());

            if (cargoIds == null || cargoIds.isEmpty()) {
                insertGpsPoint(ts, vehicle.getId(), null, gps);
            } else {
                for (String cargoId : cargoIds) {
                    insertGpsPoint(ts, vehicle.getId(), cargoId, gps);
                }
            }

            log.debug("GPS 已写入 TimescaleDB, vinTopic={}, lat={}, lng={}", vinTopic, gps.getLat(), gps.getLng());

            // 3. 更新 Redis 最新位置
            updateRedisLatest(plate, gps, ts);

        } catch (Exception e) {
            log.error("消费 GPS 消息失败: {}", e.getMessage(), e);
        }
    }

    private void insertGpsPoint(Instant ts, Long vehicleId, String cargoId, GpsData gps) {
        timescaleJdbc.update(
                "INSERT INTO gps_points (time, vehicle_id, cargo_id, imei, lat, lng, speed, heading, accuracy) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                java.sql.Timestamp.from(ts),
                vehicleId,
                cargoId,
                gps.getImei(),
                gps.getLat(),
                gps.getLng(),
                gps.getSpeed(),
                gps.getHeading(),
                gps.getAccuracy()
        );
    }

    /**
     * 更新 Redis 中车辆最新位置
     * Key: logistics:vehicle:latest:{plate}
     */
    private void updateRedisLatest(String plate, GpsData gps, Instant ts) {
        try {
            Map<String, Object> position = new LinkedHashMap<>();
            position.put("plate", plate);
            position.put("lat", gps.getLat());
            position.put("lng", gps.getLng());
            position.put("speed", gps.getSpeed());
            position.put("heading", gps.getHeading());
            position.put("accuracy", gps.getAccuracy());
            position.put("updatedAt", ts.toString());

            String json = objectMapper.writeValueAsString(position);
            String key = "logistics:vehicle:latest:" + plate;

            redisTemplate.opsForValue().set(key, json, Duration.ofHours(24));

            log.debug("Redis 最新位置已更新, key={}", key);
        } catch (Exception e) {
            log.error("Redis 更新失败: {}", e.getMessage(), e);
        }
    }
}
