package com.ship.model.dto;

/**
 * 科室医生人数统计（多对多关系表统计用）
 */
public record DepartCountDto(Integer departmentId, Integer cnt) {
}