package com.mix.model.vo;

import com.mix.model.dto.RecordDto;
import com.mix.model.entity.*;

import java.util.List;

/**
 * 病例全量档案（一个接口拿全一次就诊的全部信息）：
 * 病历 + 处方 + 套餐 + 化验单(含明细) + 影像单 + 住院 + 手术 + 免疫接种 + 随访 + 复诊链
 */
public record RecordFullVo(
        RecordDto record,
        List<Prescription> prescriptions,
        List<RecordPackage> packages,
        List<LabOrderFull> labOrders,
        List<ImageOrder> imageOrders,
        List<HospitalRecord> hospitals,
        List<Surgery> surgeries,
        List<ImmuneRecord> immunes,
        List<FollowUp> followUps,
        List<RecordDto> revisits
) {
    /** 化验单主表 + 明细 */
    public record LabOrderFull(LabOrder order, List<LabOrderItem> items) {
    }
}
