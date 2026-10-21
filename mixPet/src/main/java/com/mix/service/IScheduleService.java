package com.mix.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.common.Result;
import com.mix.model.entity.DoctorSchedule;

import java.time.LocalDate;
import java.util.List;

/**
 * 医生排班 Service
 */
public interface IScheduleService extends IService<DoctorSchedule> {
    /** 某医生的排班（含停用，按星期/时段排序，供编辑回显） */
    List<DoctorSchedule> listByDoctor(Integer doctorId);

    /** 保存某医生的一周排班（覆盖式：先清空再按启用项写入） */
    Result saveDoctorSchedule(List<DoctorSchedule> list);

    /** 全部启用排班（总览） */
    List<DoctorSchedule> listAll();

    /** 某医生当日是否出诊；未配置排班则默认放行(向后兼容) */
    boolean isOnDuty(Integer doctorId, LocalDate date);
}
