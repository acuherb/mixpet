package com.ship.service.impl;

import com.ship.common.Result;
import com.ship.mapper.HospitalSettingMapper;
import com.ship.model.entity.HospitalSetting;
import com.ship.service.IHospitalSettingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 医院基础信息服务实现：单行配置，只读/保存，不做增删与多实例
 */
@Service
public class HospitalSettingServiceImpl extends ServiceImpl<HospitalSettingMapper, HospitalSetting>
        implements IHospitalSettingService {

    @Override
    public HospitalSetting get() {
        HospitalSetting s = this.getById(1);
        return s == null ? new HospitalSetting() : s;
    }

    @Override
    public Result update(HospitalSetting setting) {
        if (setting == null) return Result.error("参数不能为空");
        setting.setId(1);
        return Result.choice("保存", this.saveOrUpdate(setting));
    }
}
