package com.ship.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.common.Result;
import com.ship.mapper.HospitalMonitorMapper;
import com.ship.model.entity.Client;
import com.ship.model.entity.Doctor;
import com.ship.model.entity.HospitalMonitor;
import com.ship.model.entity.HospitalRecord;
import com.ship.service.IHospitalMonitorService;
import com.ship.service.IHospitalService;
import com.ship.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 住院每日监护 Service 实现
 */
@Service
public class HospitalMonitorServiceImpl extends ServiceImpl<HospitalMonitorMapper, HospitalMonitor> implements IHospitalMonitorService {
    private final IHospitalService hospitalService;

    @Autowired
    public HospitalMonitorServiceImpl(IHospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    private String currentOperator() {
        Object u = UserUtil.getUser();
        if (u instanceof Doctor d) return d.getDoctorName();
        if (u instanceof Client c) return c.getClientName();
        return "系统";
    }

    @Override
    public Result addMonitor(HospitalMonitor monitor) {
        if (monitor == null || monitor.getHospitalId() == null) return Result.error("缺少住院单id");
        HospitalRecord h = hospitalService.getById(monitor.getHospitalId());
        if (h == null) return Result.error("住院单不存在");
        if (Boolean.TRUE.equals(h.getStatus())) return Result.error("该宠物已出院，不能添加监护记录");
        monitor.setPetId(h.getPetId());
        if (monitor.getMonitorTime() == null) monitor.setMonitorTime(LocalDateTime.now());
        monitor.setRecorder(currentOperator());
        this.save(monitor);
        return Result.success("监护记录已添加");
    }

    @Override
    public List<HospitalMonitor> listByHospital(Integer hospitalId) {
        return this.lambdaQuery()
                .eq(HospitalMonitor::getHospitalId, hospitalId)
                .orderByDesc(HospitalMonitor::getMonitorTime)
                .list();
    }
}
