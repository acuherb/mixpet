package com.mix.model.vo;

import com.mix.model.entity.LabTest;
import lombok.Data;

import java.util.List;

/**
 * 套组保存请求：套组头 + 成员单项 id 列表
 */
@Data
public class LabPanelSaveVo {
    /** 套组头（新增时 test_id 为空，修改时带 test_id） */
    private LabTest panel;
    /** 成员单项 test_id 列表（按顺序即 sort_order） */
    private List<Integer> memberIds;
}
