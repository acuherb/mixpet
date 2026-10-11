package com.ship.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 宠物过户请求
 */
public record TransferVo(
        Integer petId,
        Integer toClientId,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime transferDate,
        String reason
) {
}
