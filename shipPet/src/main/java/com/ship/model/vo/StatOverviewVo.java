package com.ship.model.vo;

import com.ship.model.dto.NameCount;
import com.ship.model.dto.RevenueTrend;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 经营看板聚合数据
 */
@Data
public class StatOverviewVo {
    /** 今日营收(实收) */
    private BigDecimal todayRevenue;
    /** 本月营收(实收) */
    private BigDecimal monthRevenue;
    /** 待收费账单数 */
    private Long pendingBills;
    /** 在院宠物数 */
    private Long inHospital;
    /** 待检验化验单 */
    private Long pendingLab;
    /** 待检查影像单 */
    private Long pendingImage;
    /** 近30天应接种疫苗数 */
    private Long dueVaccine;
    /** 今日挂号数 */
    private Long todayAppoint;
    /** 今日接诊(新建病历)数 */
    private Long todayRecord;
    /** 客户总数 */
    private Long clientTotal;
    /** 待发药账单数（已收款未发药） */
    private Long pendingDispense;
    /** 低库存药品数 */
    private Long lowStock;
    /** 近效期药品数（90天内） */
    private Long expiringDrug;
    /** 近7日营收趋势 */
    private List<RevenueTrend> revenueTrend;
    /** 医生接诊数 top */
    private List<NameCount> doctorTop;
}