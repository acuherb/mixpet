package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mix.model.entity.Drug;
import org.apache.ibatis.annotations.Mapper;

/**
 * 药品 Mapper
 */
@Mapper
public interface DrugMapper extends BaseMapper<Drug> {
}
