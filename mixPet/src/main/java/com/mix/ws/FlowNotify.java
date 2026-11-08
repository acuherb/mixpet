package com.mix.ws;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 诊疗流状态变更广播。
 * <p>
 * 由 controller 在业务方法返回成功后调用——此时 service 的 @Transactional 已提交，
 * 前端收到事件再拉取流水线不会读到事务未提交的旧数据。
 * 事件走既有 /chat 长连接，但 type=FLOW_REFRESH 由前端 chatSocket 隔离分发，不进入聊天消息流。
 */
public final class FlowNotify {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FlowNotify() {}

    /** 广播一次流水线刷新（只推在线员工，不推顾客） */
    public static void fire(String stage, String text) {
        try {
            String json = MAPPER.writeValueAsString(Map.of(
                    "type", "FLOW_REFRESH",
                    "stage", stage,
                    "text", text,
                    "time", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
            ChatEndpoint.broadcastFlow(json);
        } catch (Exception ignored) {
            // 广播失败不影响业务结果：前端仍有轮询兜底
        }
    }
}
