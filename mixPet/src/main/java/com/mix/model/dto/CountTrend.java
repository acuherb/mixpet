package com.mix.model.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 按日计数趋势点（如近7日每日接诊数）
 */
@Data
public class CountTrend {
    private LocalDate date;
    private Long count;
}
