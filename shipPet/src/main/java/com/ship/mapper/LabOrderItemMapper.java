package com.ship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ship.model.entity.LabOrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 化验单明细 Mapper
 */
@Mapper
public interface LabOrderItemMapper extends BaseMapper<LabOrderItem> {
}