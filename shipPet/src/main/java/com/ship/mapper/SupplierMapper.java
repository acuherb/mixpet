package com.ship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ship.model.entity.Supplier;
import org.apache.ibatis.annotations.Mapper;

/**
 * 供应商 Mapper
 */
@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {
}
