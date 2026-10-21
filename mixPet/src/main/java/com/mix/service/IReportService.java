package com.mix.service;

import com.mix.model.vo.FinanceReport;

import java.time.LocalDate;

/**
 * 经营报表 Service
 */
public interface IReportService {
    /** 经营财务报表：[start, end] 闭区间，按接诊日期统计 */
    FinanceReport finance(LocalDate start, LocalDate end);
}
