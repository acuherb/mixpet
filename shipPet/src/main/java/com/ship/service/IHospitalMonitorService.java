package com.ship.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ship.common.Result;
import com.ship.model.entity.HospitalMonitor;

import java.util.List;

/**
 * 住院每日监护 Service
 */
public interface IHospitalMonitorService extends IService<HospitalMonitor> {
    /** 添加一条监护记录（住院中才可添加，自动带宠物id与记录人） */
    Result addMonitor(HospitalMonitor monitor);

    /** 某住院单的监护记录（按记录时间倒序） */
    List<HospitalMonitor> listByHospital(Integer hospitalId);
}
