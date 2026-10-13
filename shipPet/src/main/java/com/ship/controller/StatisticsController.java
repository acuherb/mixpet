package com.ship.controller;

import com.ship.model.vo.StatOverviewVo;
import com.ship.security.model.RoleConst;
import com.ship.service.IStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计看板
 */
@RestController
@RequestMapping("/statistics")
public class StatisticsController {
    private final IStatisticsService statisticsService;

    @Autowired
    public StatisticsController(IStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/overview")
    @Secured({RoleConst.MANAGER, RoleConst.NURSE, RoleConst.DOCTOR})
    public StatOverviewVo overview() {
        return statisticsService.selectOverview();
    }
}