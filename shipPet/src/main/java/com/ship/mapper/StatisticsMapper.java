package com.ship.mapper;

import com.ship.model.dto.NameCount;
import com.ship.model.dto.RevenueTrend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 统计看板聚合 Mapper（纯只读联表统计）
 */
@Mapper
public interface StatisticsMapper {
    /** 实收金额合计（pay_status=1 的账单汇总） */
    BigDecimal sumRevenue(@Param("start") LocalDateTime start);

    /** 近N日实收趋势 */
    List<RevenueTrend> revenueTrend(@Param("start") LocalDateTime start);

    /** 医生接诊数 top */
    List<NameCount> doctorTop(@Param("limit") int limit);
}