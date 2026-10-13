package com.ship.model.vo;

import lombok.Data;

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
    /** 各明细结果 */
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