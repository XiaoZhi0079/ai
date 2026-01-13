package com.example.ai.utils;


import com.example.ai.enums.SSEMsgType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;



@Slf4j
public class SSEServer {

    // 存放所有用户的SseEmitter连接
    private static final Map<String, SseEmitter> sseClients = new ConcurrentHashMap<>();

    // 建立连接
    public static SseEmitter connect(String userId) {
        // 0L 表示不超时（注意：不超时就不会触发 onTimeout）
        SseEmitter sseEmitter = new SseEmitter(0L);

        sseEmitter.onTimeout(timeoutCallback(userId));         // Runnable
        sseEmitter.onCompletion(completionCallback(userId));   // Runnable
        sseEmitter.onError(errorCallback(userId));             // Consumer<Throwable>

        sseClients.put(userId, sseEmitter);
        log.info("SSE connect, userId: {}", userId);
        return sseEmitter;
    }

    // 发送消息
    public static void sendMsg(String userId, String message, SSEMsgType msgType) {
        if (CollectionUtils.isEmpty(sseClients)) return;

        SseEmitter sseEmitter = sseClients.get(userId);
        if (sseEmitter != null) {
            sendEmitterMessage(sseEmitter, userId, message, msgType);
        }
    }

    public static void sendMsgToAllUsers(String message) {
        if (CollectionUtils.isEmpty(sseClients)) return;

        sseClients.forEach((userId, sseEmitter) ->
                sendEmitterMessage(sseEmitter, userId, message, SSEMsgType.MESSAGE));
    }

    private static void sendEmitterMessage(SseEmitter sseEmitter,
                                           String userId,
                                           String message,
                                           SSEMsgType msgType) {

        SseEmitter.SseEventBuilder msgEvent = SseEmitter.event()
                .id(userId)
                .name(msgType.type)
                .data(message);

        try {
            sseEmitter.send(msgEvent);
        } catch (IOException e) {
            log.error("SSE send message error, userId: {}, error: {}", userId, e.getMessage());
            close(userId); // 发送异常，关闭连接并移除
        }
    }

    // 关闭连接
    public static void close(String userId) {
        SseEmitter emitter = sseClients.get(userId);
        if (emitter != null) {
            emitter.complete(); // 会触发 onCompletion
        }
    }

    // ======= 回调方法补齐（关键）=======

    private static Runnable timeoutCallback(String userId) {
        return () -> {
            log.warn("SSE timeout, userId: {}", userId);
            sseClients.remove(userId);
        };
    }

    private static Runnable completionCallback(String userId) {
        return () -> {
            log.info("SSE completed, userId: {}", userId);
            sseClients.remove(userId);
        };
    }

    private static Consumer<Throwable> errorCallback(String userId) {
        return (Throwable t) -> {
            log.error("SSE error, userId: {}, error: {}", userId, t.getMessage(), t);
            sseClients.remove(userId);
        };
    }
}
