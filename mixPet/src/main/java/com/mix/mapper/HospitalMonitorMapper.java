package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mix.model.entity.HospitalMonitor;
import org.apache.ibatis.annotations.Mapper;

/**
 * 住院每日监护记录 Mapper
 */
@Mapper
public interface HospitalMonitorMapper extends BaseMapper<HospitalMonitor> {
}
