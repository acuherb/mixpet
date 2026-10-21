package com.mix.model.dto;

import java.time.LocalDateTime;

/**
 * 消息类
 */
public record MsgDo(
        Integer msgId,
        String roleName,
        String rolePhoto,
        boolean isMine,
        String msgContent,
        boolean isImg,
        String msgType,
        String msgName,
        LocalDateTime createTime,
        boolean isRead
) {
}