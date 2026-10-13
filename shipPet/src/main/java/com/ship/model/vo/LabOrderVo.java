package com.ship.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 医生开化验单请求
 */
@Data
public class LabOrderVo {
    /** 病历id（必填，归属以病历为准） */
    private Integer recordId;
    /** 检验目的/备注 */
    private String orderNote;
    /** 检验项（test_id 列表） */
    private List<Integer> testIds;
}