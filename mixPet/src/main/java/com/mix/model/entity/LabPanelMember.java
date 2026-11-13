package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 化验套组-成员映射：套组(panel_id)与其下成员单项(member_test_id)的多对多关系
 */
@Data
@TableName("lab_panel_member")
public class LabPanelMember implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 套组id(lab_test.test_id, is_panel=1)，复合主键之一 */
    @TableId(type = IdType.NONE)
    private Integer panelId;
    /** 成员单项id(lab_test.test_id, is_panel=0) */
    private Integer memberTestId;
    /** 成员排序 */
    private Integer sortOrder;
}
