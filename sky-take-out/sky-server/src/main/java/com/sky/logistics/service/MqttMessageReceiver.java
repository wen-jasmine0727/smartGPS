package com.sky.logistics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.logistics.dto.GpsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * MQTT 消息接收器
 *
 * 核心链路：
 * MQTT 消息 → 解析 GPS JSON → 提取 vinTopic → 发送 Kafka gps-points
 */
@Service
@Slf4j
public class MqttMessageReceiver {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 监听 mqttInputChannel，处理所有到达的 MQTT 消息
     */
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
        // 1. 获取 MQTT 主题
        String topic = (String) message.getHeaders().get("mqtt_topic");
        if (topic == null) {
            log.warn("收到没有 mqtt_topic 的消息，丢弃");
            return;
        }
        log.info("MQTT 收到消息，主题: {}", topic);

        // 2. 获取消息体（字节数组）
        byte[] payload;
        Object body = message.getPayload();
        if (body instanceof byte[]) {
            payload = (byte[]) body;
        } else if (body instanceof String) {
            payload = ((String) body).getBytes(StandardCharsets.UTF_8);
        } else {
            log.warn("不支持的消息类型: {}", body.getClass().getName());
            return;
        }

        String json = new String(payload, StandardCharsets.UTF_8);
        log.debug("MQTT 消息内容: {}", json);

        // 3. 根据主题类型分发处理
        if (topic.endsWith("/gps")) {
            handleGps(topic, json);
        } else if (topic.endsWith("/heartbeat")) {
            handleHeartbeat(topic, json);
        } else {
            log.warn("未知的 MQTT 主题: {}", topic);
        }
    }

    /**
     * 处理 GPS 数据：解析 → 提取 vinTopic → 发 Kafka gps-points
     */
    private void handleGps(String topic, String json) {
        try {
            // 从主题 vehicle/沪A-C0291/gps 中提取 vinTopic
            String vinTopic = extractVin(topic);
            if (vinTopic == null) {
                log.warn("无法从主题中提取 vinTopic: {}", topic);
                return;
            }

            GpsData gps = objectMapper.readValue(json, GpsData.class);
            gps.setVinTopic(vinTopic);

            // 基础校验
            if (gps.getLat() == null || gps.getLng() == null) {
                log.warn("GPS 数据缺少经纬度，丢弃: {}", json);
                return;
            }
            if (gps.getLat() < -90 || gps.getLat() > 90 || gps.getLng() < -180 || gps.getLng() > 180) {
                log.warn("GPS 坐标越界, lat={}, lng={}", gps.getLat(), gps.getLng());
                return;
            }

            // 序列化后发送到 Kafka gps-points
            String kafkaValue = objectMapper.writeValueAsString(gps);
            // Key 使用 vinTopic，保证同一车辆的消息有序
            kafkaTemplate.send("gps-points", vinTopic, kafkaValue);

            log.info("GPS 已发送 Kafka, vinTopic={}, lat={}, lng={}, speed={}",
                    vinTopic, gps.getLat(), gps.getLng(), gps.getSpeed());

        } catch (Exception e) {
            log.error("处理 GPS 消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理心跳数据：解析 → 发 Kafka vehicle-heartbeats
     */
    private void handleHeartbeat(String topic, String json) {
        try {
            String vinTopic = extractVin(topic);
            if (vinTopic == null) return;

            // 心跳数据直接透传 JSON，由后端 3 消费处理
            kafkaTemplate.send("vehicle-heartbeats", vinTopic, json);

            log.debug("心跳已发送 Kafka, vinTopic={}", vinTopic);

        } catch (Exception e) {
            log.error("处理心跳消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从 MQTT 主题中提取 vinTopic
     * 输入: vehicle/沪A-C0291/gps → 输出: 沪A-C0291
     */
    private String extractVin(String topic) {
        if (topic == null) return null;
        String[] parts = topic.split("/");
        if (parts.length >= 2) {
            return parts[1];
        }
        return null;
    }
}
