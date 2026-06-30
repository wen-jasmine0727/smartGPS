package com.sky.logistics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.logistics.dto.GpsData;
import com.sky.logistics.entity.Vehicle;
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
            String plate = (vehicle != null) ? vehicle.getPlate() : vinTopic.replace("-", "·");

            // 校验 IMEI
            if (vehicle != null && gps.getImei() != null
                    && !gps.getImei().equals(vehicle.getDeviceImei())) {
                log.warn("IMEI 不匹配, 消息 imei={}, 车辆 imei={}", gps.getImei(), vehicle.getDeviceImei());
                // 不丢弃，仅记录
            }

            // 2. 写入 TimescaleDB gps_points 表
            Instant ts = (gps.getTs() != null)
                    ? Instant.ofEpochSecond(gps.getTs())
                    : Instant.now();

            // TODO: 查询当前 active cargo，目前暂用 vinTopic 映射
            String cargoId = null; // 后续关联 cargo_vehicle_binding 查询

            timescaleJdbc.update(
                "INSERT INTO gps_points (time, vehicle_id, cargo_id, imei, lat, lng, speed, heading, accuracy) "
                + "VALUES (?::timestamptz, ?, ?, ?, ?, ?, ?, ?, ?)",
                ts, vinTopic, cargoId, gps.getImei(),
                gps.getLat(), gps.getLng(),
                gps.getSpeed(), gps.getHeading(), gps.getAccuracy()
            );

            log.debug("GPS 已写入 TimescaleDB, vinTopic={}, lat={}, lng={}", vinTopic, gps.getLat(), gps.getLng());

            // 3. 更新 Redis 最新位置
            updateRedisLatest(plate, gps, ts);

        } catch (Exception e) {
            log.error("消费 GPS 消息失败: {}", e.getMessage(), e);
        }
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
