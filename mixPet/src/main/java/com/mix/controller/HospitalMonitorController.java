package com.mix.controller;

import com.mix.common.Result;
import com.mix.model.entity.Client;
import com.mix.model.entity.HospitalMonitor;
import com.mix.model.entity.HospitalRecord;
import com.mix.model.entity.MedicalRecord;
import com.mix.security.model.RoleConst;
import com.mix.service.IHospitalMonitorService;
import com.mix.service.IHospitalService;
import com.mix.service.IMedicalRecordService;
import com.mix.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 住院每日监护记录
 */
@RestController
@RequestMapping("/hospital/monitor")
public class HospitalMonitorController {
    private final IHospitalMonitorService monitorService;
    private final IHospitalService hospitalService;
    private final IMedicalRecordService recordService;

    @Autowired
    public HospitalMonitorController(IHospitalMonitorService monitorService,
                                     IHospitalService hospitalService,
                                     IMedicalRecordService recordService) {
        this.monitorService = monitorService;
        this.hospitalService = hospitalService;
        this.recordService = recordService;
    }

    /** 添加监护记录（住院中） */
    @PostMapping
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE})
    public Result add(@RequestBody HospitalMonitor monitor) {
        return monitorService.addMonitor(monitor);
    }

    /** 某住院单的监护记录：顾客只能看自己宠物的 */
    @GetMapping("/{hospitalId}")
    public List<HospitalMonitor> list(@PathVariable Integer hospitalId) {
        if (UserUtil.getUser() instanceof Client client) {
            HospitalRecord h = hospitalService.getById(hospitalId);
            if (h == null) return List.of();
            MedicalRecord r = recordService.getById(h.getRecordId());
            if (r == null || !client.getClientId().equals(r.getClientId())) return List.of();
        }
        return monitorService.listByHospital(hospitalId);
    }
}
