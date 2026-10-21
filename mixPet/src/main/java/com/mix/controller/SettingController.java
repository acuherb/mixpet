package com.mix.controller;

import com.mix.common.Result;
import com.mix.model.entity.HospitalSetting;
import com.mix.security.model.RoleConst;
import com.mix.service.IHospitalSettingService;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

/**
 * 系统设置：医院基础信息（单行配置）
 * GET 公开（打印页/顾客端都要读抬头）；PUT 仅 MANAGER
 */
@RestController
@RequestMapping("/setting")
public class SettingController {
    private final IHospitalSettingService hospitalSettingService;

    public SettingController(IHospitalSettingService hospitalSettingService) {
        this.hospitalSettingService = hospitalSettingService;
    }

    /** 医院信息（公开读取） */
    @GetMapping("/hospital")
    public HospitalSetting hospital() {
        return hospitalSettingService.get();
    }

    /** 保存医院信息（仅管理员） */
    @PutMapping("/hospital")
    @Secured(RoleConst.MANAGER)
    public Result saveHospital(@RequestBody HospitalSetting setting) {
        return hospitalSettingService.update(setting);
    }
}
