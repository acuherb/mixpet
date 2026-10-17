package com.ship.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ship.common.Result;
import com.ship.model.entity.HospitalSetting;

/**
 * 医院基础信息服务（单行配置）
 */
public interface IHospitalSettingService extends IService<HospitalSetting> {
    /** 读取医院信息（无配置返回空对象，不抛异常） */
    HospitalSetting get();

    /** 保存医院信息（固定 id=1） */
    Result update(HospitalSetting setting);
}
