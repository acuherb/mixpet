package com.ship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ship.model.entity.SysDict;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通用字典 Mapper
 */
@Mapper
public interface SysDictMapper extends BaseMapper<SysDict> {
}
