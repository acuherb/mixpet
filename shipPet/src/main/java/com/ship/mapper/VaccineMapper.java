package com.ship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ship.model.entity.Vaccine;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VaccineMapper extends BaseMapper<Vaccine> {
}