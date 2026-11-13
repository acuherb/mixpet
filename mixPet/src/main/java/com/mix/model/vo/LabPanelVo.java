package com.mix.model.vo;

import com.mix.model.entity.LabTest;
import lombok.Data;

import java.util.List;

/**
 * 套组联查：套组头 + 成员单项列表（用于开单选套组时展示所包含的项目）
 */
@Data
public class LabPanelVo {
    /** 套组头（lab_test，is_panel=1） */
    private LabTest panel;
    /** 成员单项（lab_test，is_panel=0），按 sort_order 排列 */
    private List<LabTest> members;
}
