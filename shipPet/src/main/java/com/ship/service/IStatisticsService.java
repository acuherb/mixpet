package com.ship.service;

import com.ship.model.vo.StatOverviewVo;

/**
 * 统计看板 Service
 */
public interface IStatisticsService {
    /** 经营看板聚合数据 */
    StatOverviewVo selectOverview();
}