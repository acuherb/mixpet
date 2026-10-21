package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mix.model.entity.SysDict;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通用字典 Mapper
 */
@Mapper
public interface SysDictMapper extends BaseMapper<SysDict> {
}
