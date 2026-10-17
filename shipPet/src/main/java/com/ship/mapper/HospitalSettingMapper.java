package com.ship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ship.model.entity.HospitalSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 医院基础信息 Mapper（单行配置）
 */
@Mapper
public interface HospitalSettingMapper extends BaseMapper<HospitalSetting> {
}
