package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mix.model.entity.HospitalAdvice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 住院医嘱单 Mapper
 */
@Mapper
public interface HospitalAdviceMapper extends BaseMapper<HospitalAdvice> {
}
