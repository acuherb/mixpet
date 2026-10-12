package com.ship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ship.model.entity.Drug;
import org.apache.ibatis.annotations.Mapper;

/**
 * 药品 Mapper
 */
@Mapper
public interface DrugMapper extends BaseMapper<Drug> {
}
