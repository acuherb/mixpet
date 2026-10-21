package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mix.model.entity.PackageItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 套餐项目 Mapper
 */
@Mapper
public interface PackageItemMapper extends BaseMapper<PackageItem> {
}
