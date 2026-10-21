package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.model.dto.LabOrderDto;
import com.mix.model.entity.LabOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 化验单 Mapper
 */
@Mapper
public interface LabOrderMapper extends BaseMapper<LabOrder> {
    /** 化验单分页联查（后台/医生） */
    IPage<LabOrderDto> selectLabOrderPage(IPage<LabOrderDto> page, Integer status);
}