package com.ship.model.vo;

/**
 * 病历检索条件（管理端/医生端病例中心）
 */
public record RecordQueryVo(
        Integer recordId,
        String petName,
        String clientName,
        String doctorName,
        String diagnosis,
        /** 就诊状态：0接诊中 1已完结 2已作废 */
        Integer status,
        /** 起始时间 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss */
        String startTime,
        /** 截止时间 */
        String endTime
) {
}
