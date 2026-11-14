package com.mix.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 接回结算入参。settleMoney 为空表示按后端「天数×单价」自动定价；
 * 有值时为人工改价，必须同时给 adjustNote 留痕，服务端只做范围校验不做静默丢弃。
 */
public record FosterBackVo(
        LocalDate backDate,
        BigDecimal settleMoney,
        String adjustNote
) {
}
