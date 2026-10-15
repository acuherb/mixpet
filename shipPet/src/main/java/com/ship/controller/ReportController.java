package com.ship.controller;

import com.ship.model.vo.FinanceReport;
import com.ship.security.model.RoleConst;
import com.ship.service.IReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 经营报表
 */
@RestController
@RequestMapping("/report")
public class ReportController {
    private final IReportService reportService;

    @Autowired
    public ReportController(IReportService reportService) {
        this.reportService = reportService;
    }

    /** 经营财务报表：[start, end] 闭区间 */
    @GetMapping("/finance")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public FinanceReport finance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        if (start == null) start = LocalDate.now().withDayOfMonth(1);
        if (end == null) end = LocalDate.now();
        if (end.isBefore(start)) { LocalDate t = start; start = end; end = t; }
        return reportService.finance(start, end);
    }
}
