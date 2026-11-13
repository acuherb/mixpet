package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mix.model.entity.LabPanelMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 化验套组-成员映射 Mapper
 */
@Mapper
public interface LabPanelMemberMapper extends BaseMapper<LabPanelMember> {
}
