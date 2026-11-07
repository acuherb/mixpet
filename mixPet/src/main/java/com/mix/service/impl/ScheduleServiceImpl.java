package com.mix.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.common.BizException;
import com.mix.common.Result;
import com.mix.mapper.DoctorScheduleMapper;
import com.mix.model.entity.DoctorSchedule;
import com.mix.service.IScheduleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 医生排班 Service 实现
 */
@Service
public class ScheduleServiceImpl extends ServiceImpl<DoctorScheduleMapper, DoctorSchedule> implements IScheduleService {
    /** 与 application.yml 的 appointment.max-per-day 一致：任何单时段号源不得超过每日硬上限 */
    private static final int MAX_PER_DAY = 20;

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
        if (list == null || list.isEmpty()) throw new BizException("排班数据为空");
        Integer doctorId = list.get(0).getDoctorId();
        if (doctorId == null) throw new BizException("缺少医生");
        this.lambdaUpdate()
                .eq(DoctorSchedule::getDoctorId, doctorId)
                .set(DoctorSchedule::getIsDel, System.currentTimeMillis())
                .update();
        List<DoctorSchedule> active = list.stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive())
                        && s.getWeekday() != null && s.getPeriod() != null)
                .peek(s -> {
                    s.setScheduleId(null);
                    s.setDoctorId(doctorId);
                    if (s.getMaxCount() == null) s.setMaxCount(10);
                    if (s.getMaxCount() < 0 || s.getMaxCount() > MAX_PER_DAY)
                        throw new BizException("单时段号源数必须在 0~" + MAX_PER_DAY + " 之间");
                    s.setIsDel(0L);
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
        return isOnDuty(doctorId, date, null);
    }

    @Override
    public boolean isOnDuty(Integer doctorId, LocalDate date, Integer period) {
        if (doctorId == null || date == null) return true;
        List<DoctorSchedule> rows = activeRows(doctorId);
        if (rows.isEmpty()) return true; // 未配置排班 → 不限制（向后兼容）
        int weekday = date.getDayOfWeek().getValue();
        return rows.stream().anyMatch(s -> weekday == s.getWeekday()
                && (period == null || period.equals(s.getPeriod())));
    }

    @Override
    public int maxCountOf(Integer doctorId, int weekday, int period) {
        return activeRows(doctorId).stream()
                .filter(s -> weekday == s.getWeekday() && period == s.getPeriod())
                .mapToInt(s -> s.getMaxCount() == null ? 10 : s.getMaxCount())
                .max()
                .orElse(0);
    }

    private List<DoctorSchedule> activeRows(Integer doctorId) {
        return this.lambdaQuery()
                .eq(DoctorSchedule::getDoctorId, doctorId)
                .eq(DoctorSchedule::getActive, true)
                .and(w -> w.eq(DoctorSchedule::getIsDel, 0).or().isNull(DoctorSchedule::getIsDel))
                .list();
    }
}
