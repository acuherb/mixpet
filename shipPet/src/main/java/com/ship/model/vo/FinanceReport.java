package com.ship.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 经营财务报表（按接诊日期统计已收款、未退款账单）
 */
@Data
public class FinanceReport {
    private LocalDate startDate;
    private LocalDate endDate;

    /** 总收入（已收款未退款） */
    private BigDecimal totalRevenue;
    /** 订单数（已收款未退款） */
    private Integer orderCount;
    /** 客单价 */
    private BigDecimal avgOrder;
    /** 退款单数 / 退款金额 */
    private Integer refundCount;
    private BigDecimal refundAmount;

    /** 收入构成（按收费项目） */
    private BigDecimal feeRegister;
    private BigDecimal feeTreat;
    private BigDecimal feeDrug;
    private BigDecimal feeLab;
    private BigDecimal feeImage;
    private BigDecimal feePackage;
    private BigDecimal feeSurgery;
    private BigDecimal feeHospital;

    /** 药品成本/毛利（成本按当前进货价估算） */
    private BigDecimal drugCost;
    private BigDecimal drugProfit;

    /** 每日收入趋势 */
    private List<TrendItem> trend;
    /** 按科室 / 按医生 */
    private List<NameAmount> byDepartment;
    private List<NameAmount> byDoctor;

    @Data
    public static class TrendItem {
        private LocalDate date;
        private BigDecimal amount;

        public TrendItem(LocalDate date, BigDecimal amount) {
            this.date = date;
            this.amount = amount;
        }
    }

    @Data
    public static class NameAmount {
        private String name;
        private BigDecimal amount;

        public NameAmount(String name, BigDecimal amount) {
            this.name = name;
            this.amount = amount;
        }
    }
}
