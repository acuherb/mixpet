package com.mix.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 化验结果录入请求
 */
@Data
public class LabResultVo {
    /** 化验单id */
    private Integer orderId;
    /** 化验小结/结论 */
    private String resultNote;
    /** 报告文件(图片/PDF)路径，可选 */
    private String reportFile;
    /** 标本类型与采样/接收时间（临床时间轴） */
    private String specimenType;
    private LocalDateTime collectTime;
    private LocalDateTime receiveTime;
    /** 修订原因：已出结果后再次录入必填 */
    private String reviseReason;
    /** 各明细结果（isAbnormal 由服务端复算，前端传值仅作非数值结果的人工标记） */
    private List<LabResultItemVo> items;

    @Data
    public static class LabResultItemVo {
        private Integer itemId;
        /** 检验结果值 */
        private String resultValue;
        /** 是否异常 */
        private Boolean isAbnormal;
    }
}