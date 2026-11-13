package com.mix.controller;

import com.mix.common.Result;
import com.mix.model.entity.DoctorSchedule;
import com.mix.security.model.RoleConst;
import com.mix.service.IScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 医生排班
 */
@RestController
@RequestMapping("/schedule")
public class ScheduleController {
    private final IScheduleService scheduleService;

    @Autowired
    public ScheduleController(IScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /** 某医生排班（编辑回显；顾客挂号时读时段与余号） */
    @GetMapping("/doctor/{doctorId}")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN, RoleConst.CLIENT})
    public List<DoctorSchedule> byDoctor(@PathVariable Integer doctorId) {
        return scheduleService.listByDoctor(doctorId);
    }

    /** 排班总览（全部启用项；挂号/接诊场景需要按周几过滤出「今日医生」） */
    @GetMapping("/all")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN, RoleConst.DOCTOR, RoleConst.NURSE})
    public List<DoctorSchedule> all() {
        return scheduleService.listAll();
    }

    /** 保存某医生一周排班（覆盖式） */
    @PostMapping
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public Result save(@RequestBody List<DoctorSchedule> list) {
        return scheduleService.saveDoctorSchedule(list);
    }
}
