package com.sky.logistics.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.logistics.entity.CargoRecord;
import com.sky.logistics.mapper.LogisticsCargoMapper;
import com.sky.logistics.service.TrackingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TrackingServiceImpl implements TrackingService {

    private final LogisticsCargoMapper cargoMapper;
    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate timescaleJdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String REDIS_LATEST_PREFIX = "logistics:vehicle:latest:";
    private static final int TRAJECTORY_MAX_POINTS = 200;

    public TrackingServiceImpl(
            LogisticsCargoMapper cargoMapper,
            StringRedisTemplate redisTemplate,
            @Qualifier("timescaleJdbcTemplate") JdbcTemplate timescaleJdbc) {
        this.cargoMapper = cargoMapper;
        this.redisTemplate = redisTemplate;
        this.timescaleJdbc = timescaleJdbc;
    }

    @Override
    public Map<String, Object> getPosition(String cargoId) {
        CargoRecord cargo = requireCargo(cargoId);
        String plate = cargo.getVehiclePlate();

        if (plate == null) {
            return noBindingResult(cargoId);
        }

        // 从 Redis 读最新位置
        String json = redisTemplate.opsForValue().get(REDIS_LATEST_PREFIX + plate);
        if (json == null) {
            return noGpsYetResult(cargoId, plate);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> position = objectMapper.readValue(json, LinkedHashMap.class);
            position.put("cargoId", cargoId);
            position.put("source", "REDIS");
            // 保留 Redis 中的 updatedAt 而非覆盖
            if (!position.containsKey("vehiclePlate")) {
                position.put("vehiclePlate", plate);
            }
            if (cargo.getDriverName() != null) {
                position.put("driverName", cargo.getDriverName());
            }
            return position;
        } catch (Exception e) {
            log.error("解析 Redis 位置数据失败: {}", e.getMessage());
            return noGpsYetResult(cargoId, plate);
        }
    }

    @Override
    public Map<String, Object> getTrajectory(String cargoId) {
        CargoRecord cargo = requireCargo(cargoId);
        Long vehicleId = cargo.getVehicleId();

        if (vehicleId == null) {
            return noBindingResult(cargoId);
        }

        // 查 TimescaleDB 最近 200 个 GPS 点
        List<Map<String, Object>> points = timescaleJdbc.queryForList(
            "SELECT time, lat, lng, speed, heading, accuracy " +
            "FROM gps_points " +
            "WHERE vehicle_id = ? " +
            "ORDER BY time DESC " +
            "LIMIT ?",
            vehicleId, TRAJECTORY_MAX_POINTS
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cargoId", cargoId);
        result.put("vehicleId", vehicleId);
        result.put("vehiclePlate", cargo.getVehiclePlate());
        result.put("pointCount", points.size());
        result.put("points", points);
        return result;
    }

    @Override
    public Map<String, Object> getEta(String cargoId) {
        CargoRecord cargo = requireCargo(cargoId);
        String plate = cargo.getVehiclePlate();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cargoId", cargoId);

        if (plate == null) {
            result.put("trend", "NO_VEHICLE");
            result.put("remainingMinutes", -1);
            result.put("message", "货物未绑定车辆");
            return result;
        }

        // 查最新位置
        String json = redisTemplate.opsForValue().get(REDIS_LATEST_PREFIX + plate);
        if (json == null) {
            result.put("trend", "NO_GPS");
            result.put("remainingMinutes", -1);
            result.put("message", "暂无 GPS 数据");
            return result;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> pos = objectMapper.readValue(json, LinkedHashMap.class);

            double currentLat = toDouble(pos.get("lat"));
            double currentLng = toDouble(pos.get("lng"));
            int speed = toInt(pos.get("speed"), 60);

            // 计算到目的地的球面距离
            double destLat = cargo.getDestinationLat() != null ? cargo.getDestinationLat() : 0;
            double destLng = cargo.getDestinationLng() != null ? cargo.getDestinationLng() : 0;
            double distanceKm = haversineKm(currentLat, currentLng, destLat, destLng);

            // 有效速度至少 30 km/h（避免停车导致 ETA 无穷大）
            int effectiveSpeed = Math.max(speed, 30);

            // roadFactor：默认国道系数 1.2
            double roadFactor = 1.2;
            double remainingMinutes = (distanceKm / effectiveSpeed) * 60 * roadFactor;

            result.put("currentLat", currentLat);
            result.put("currentLng", currentLng);
            result.put("currentSpeed", speed);
            result.put("effectiveSpeed", effectiveSpeed);
            result.put("roadFactor", roadFactor);
            result.put("distanceKm", Math.round(distanceKm * 100.0) / 100.0);
            result.put("distanceRemaining", Math.round(distanceKm * 100.0) / 100.0);
            result.put("remainingMinutes", Math.round(remainingMinutes));
            result.put("remainingHours", String.format("%.1f", remainingMinutes / 60));

            // 进度：已行驶距离 / 总距离
            if (cargo.getOriginLat() != null && cargo.getOriginLng() != null) {
                double totalKm = haversineKm(
                    cargo.getOriginLat(), cargo.getOriginLng(), destLat, destLng);
                double travelledKm = haversineKm(
                    cargo.getOriginLat(), cargo.getOriginLng(), currentLat, currentLng);
                double progress = totalKm > 0 ? Math.min(travelledKm / totalKm, 0.99) : 0;
                result.put("totalDistanceKm", Math.round(totalKm * 100.0) / 100.0);
                result.put("progress", Math.round(progress * 100.0) / 100.0);
            }

            result.put("trend", speed > 5 ? "ON_TRACK" : "STOPPED");
            result.put("calculatedAt", Instant.now().toString());

        } catch (Exception e) {
            log.error("计算 ETA 失败: {}", e.getMessage(), e);
            result.put("trend", "ERROR");
            result.put("message", "计算失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getTimeline(String cargoId) {
        CargoRecord cargo = requireCargo(cargoId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cargoId", cargoId);
        result.put("cargoType", cargo.getCargoType());
        result.put("status", cargo.getStatus());
        result.put("vehiclePlate", cargo.getVehiclePlate());

        List<Map<String, Object>> events = new ArrayList<>();

        // 1. 创建事件
        if (cargo.getCreatedAt() != null) {
            events.add(event(cargo.getCreatedAt(), "CREATED", "货物创建"));
        }
        // 2. 装货事件
        if (cargo.getLoadedAt() != null) {
            events.add(event(cargo.getLoadedAt(), "LOADED", "货物装车"));
        }
        // 3. 运输中 - 从 TimescaleDB 取第一个 GPS 点时间
        if (cargo.getVehicleId() != null) {
            List<Map<String, Object>> firstPoint = timescaleJdbc.queryForList(
                "SELECT time FROM gps_points WHERE vehicle_id = ? ORDER BY time ASC LIMIT 1",
                cargo.getVehicleId()
            );
            if (!firstPoint.isEmpty()) {
                Object firstTime = firstPoint.get(0).get("time");
                events.add(event(firstTime, "IN_TRANSIT", "开始运输（首次 GPS 上报）"));
            }
        }
        // 4. 送达事件
        if (cargo.getDeliveredAt() != null) {
            events.add(event(cargo.getDeliveredAt(), "DELIVERED", "货物已送达"));
        }

        result.put("events", events);
        return result;
    }

    // ─── 工具方法 ──────────────────────────────────────

    private CargoRecord requireCargo(String cargoId) {
        CargoRecord cargo = cargoMapper.findByCargoId(cargoId);
        if (cargo == null) {
            throw new IllegalArgumentException("货物不存在: " + cargoId);
        }
        return cargo;
    }

    private Map<String, Object> noBindingResult(String cargoId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cargoId", cargoId);
        result.put("status", "NO_BINDING");
        result.put("message", "货物未绑定车辆");
        result.put("source", "NONE");
        return result;
    }

    private Map<String, Object> noGpsYetResult(String cargoId, String plate) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cargoId", cargoId);
        result.put("vehiclePlate", plate);
        result.put("status", "NO_GPS");
        result.put("message", "车辆尚未上报 GPS 数据");
        result.put("source", "NONE");
        return result;
    }

    private Map<String, Object> event(Object time, String type, String title) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("time", time instanceof OffsetDateTime ? ((OffsetDateTime) time).toInstant().toString() : String.valueOf(time));
        e.put("type", type);
        e.put("title", title);
        return e;
    }

    /**
     * 球面距离（Haversine 公式），返回公里
     */
    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) return Double.parseDouble((String) value);
        return 0.0;
    }

    private int toInt(Object value, int defaultVal) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) return Integer.parseInt((String) value);
        return defaultVal;
    }
}
