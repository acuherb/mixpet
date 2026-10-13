package com.ship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ship.model.entity.ServicePackage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 套餐 Mapper
 */
@Mapper
public interface ServicePackageMapper extends BaseMapper<ServicePackage> {
}
