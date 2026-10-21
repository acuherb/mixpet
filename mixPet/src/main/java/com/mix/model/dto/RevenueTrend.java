package com.mix.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 营收趋势点
 */
@Data
public class RevenueTrend {
    private LocalDate date;
    private BigDecimal amount;
}