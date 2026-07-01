package com.sky.logistics.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务端 —— GPS 到达时推送 vehicle.position 给前端
 */
@Component
@ServerEndpoint("/api/v1/ws")
@Slf4j
public class LogisticsWebSocketServer {

    /** 已连接的客户端 session 集合，线程安全 */
    private static final Map<String, Session> CLIENTS = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        CLIENTS.put(session.getId(), session);
        log.info("WebSocket 客户端连接, session={}, 当前连接数={}", session.getId(), CLIENTS.size());
        send(session, "{\"channel\":\"system.connected\",\"data\":{\"status\":\"CONNECTED\"}}");
    }

    @OnClose
    public void onClose(Session session) {
        CLIENTS.remove(session.getId());
        log.info("WebSocket 客户端断开, session={}, 当前连接数={}", session.getId(), CLIENTS.size());
    }

    /**
     * 向所有已连接客户端广播消息
     */
    public static void broadcast(String channel, String jsonData) {
        String message = String.format("{\"channel\":\"%s\",\"data\":%s}", channel, jsonData);
        CLIENTS.values().forEach(session -> send(session, message));
    }

    private static void send(Session session, String message) {
        if (session != null && session.isOpen()) {
            synchronized (session) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    log.error("WebSocket 发送失败: {}", e.getMessage());
                }
            }
        }
    }
}
