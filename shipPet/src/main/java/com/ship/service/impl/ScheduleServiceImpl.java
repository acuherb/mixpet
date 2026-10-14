package com.ship.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.common.Result;
import com.ship.mapper.DoctorScheduleMapper;
import com.ship.model.entity.DoctorSchedule;
import com.ship.service.IScheduleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 医生排班 Service 实现
 */
@Service
public class ScheduleServiceImpl extends ServiceImpl<DoctorScheduleMapper, DoctorSchedule> implements IScheduleService {

    @Override
    public List<DoctorSchedule> listByDoctor(Integer doctorId) {
        return this.lambdaQuery()
                .eq(DoctorSchedule::getDoctorId, doctorId)
                .orderByAsc(DoctorSchedule::getWeekday)
                .orderByAsc(DoctorSchedule::getPeriod)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result saveDoctorSchedule(List<DoctorSchedule> list) {
        if (list == null || list.isEmpty()) return Result.error("排班数据为空");
        Integer doctorId = list.get(0).getDoctorId();
        if (doctorId == null) return Result.error("缺少医生");
        // 覆盖式：先逻辑清空该医生原有排班
        this.lambdaUpdate()
                .eq(DoctorSchedule::getDoctorId, doctorId)
                .set(DoctorSchedule::getIsDel, System.currentTimeMillis())
                .update();
        // 仅写入启用的时段
        List<DoctorSchedule> active = list.stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive())
                        && s.getWeekday() != null && s.getPeriod() != null)
                .peek(s -> {
                    s.setScheduleId(null);
                    s.setDoctorId(doctorId);
                    if (s.getMaxCount() == null) s.setMaxCount(10);
                })
                .toList();
        if (!active.isEmpty()) this.saveBatch(active);
        return Result.success("排班已保存");
    }

    @Override
    public List<DoctorSchedule> listAll() {
        return this.lambdaQuery()
                .eq(DoctorSchedule::getActive, true)
                .orderByAsc(DoctorSchedule::getDoctorId)
                .orderByAsc(DoctorSchedule::getWeekday)
                .orderByAsc(DoctorSchedule::getPeriod)
                .list();
    }

    @Override
    public boolean isOnDuty(Integer doctorId, LocalDate date) {
        if (doctorId == null || date == null) return true;
        List<DoctorSchedule> rows = this.lambdaQuery()
                .eq(DoctorSchedule::getDoctorId, doctorId)
                .eq(DoctorSchedule::getActive, true)
                .list();
        // 未配置排班 → 不限制（向后兼容，避免影响既有挂号）
        if (rows.isEmpty()) return true;
        int weekday = date.getDayOfWeek().getValue(); // 1(Mon)..7(Sun)
        return rows.stream().anyMatch(s -> weekday == s.getWeekday());
    }
}
