package com.ship.model.dto;

import lombok.Data;

/**
 * 命名聚合统计（医生接诊数 top 等）
 */
@Data
public class NameCount {
    private String name;
    private Long count;
}