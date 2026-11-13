package com.mix.model.vo;

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
    /** 检验项（test_id 列表，独立单项） */
    private List<Integer> testIds;
    /** 套组（test_id 列表，is_panel=1）：每个套组只收一次费，其下成员只生成结果明细不重复计费 */
    private List<Integer> panelIds;
}