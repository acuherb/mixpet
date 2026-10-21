package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mix.model.entity.DoctorSchedule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 医生排班 Mapper
 */
@Mapper
public interface DoctorScheduleMapper extends BaseMapper<DoctorSchedule> {
}
