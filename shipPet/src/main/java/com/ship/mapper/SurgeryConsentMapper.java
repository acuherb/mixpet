package com.ship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ship.model.entity.SurgeryConsent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 手术知情同意书 Mapper
 */
@Mapper
public interface SurgeryConsentMapper extends BaseMapper<SurgeryConsent> {
}
