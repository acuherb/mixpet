package com.mix.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 检验/影像申请单（国标风格，可打印）
 * 头部信息（畜主/宠物/医生/诊断）两者通用；检验为多项，影像为单项+检查部位
 */
@Data
public class RequisitionVo {
    /** 类型：检验申请单 / 影像申请单 */
    private String type;
    private Integer orderId;
    /** 单据编号 */
    private String orderNo;
    private LocalDateTime createTime;

    /** 畜主 */
    private String clientName;
    private String petName;
    private String petVariety;
    private Boolean petSex;
    private LocalDate petBirthday;
    private BigDecimal petWeight;
    private String petChip;

    /** 临床诊断 */
    private String diagnosis;
    /** 申请医生 */
    private String doctorName;
    private String doctorCert;

    /** 项目明细（检验多项；影像单项） */
    private List<ReqItem> items;
    /** 检查部位/目的（影像） */
    private String examPart;
    /** 检验目的/备注 */
    private String remark;
    /** 费用 */
    private BigDecimal fee;

    @Data
    public static class ReqItem {
        private String name;
        private String group;
        /** 参考范围（检验） */
        private String refRange;
        /** 单位（检验） */
        private String unit;
    }
}
