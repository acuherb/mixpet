package com.mix.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.common.BizException;
import com.mix.mapper.AppointmentMapper;
import com.mix.mapper.DoctorMapper;
import com.mix.model.dto.AppointmentDto;
import com.mix.model.dto.AppointmentDto2;
import com.mix.model.entity.Appointment;
import com.mix.model.entity.Client;
import com.mix.model.entity.Doctor;
import com.mix.service.IAppointmentService;
import com.mix.service.IClientService;
import com.mix.service.IScheduleService;
import com.mix.wx.WeChatTemplateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements IAppointmentService {
    private final DoctorMapper doctorMapper;
    private final IScheduleService scheduleService;
    private final IClientService clientService;
    private final WeChatTemplateService wxNotify;

    /** 每日硬上限（兜底），真实容量以 doctor_schedule.max_count 为准 */
    @Value("${appointment.max-per-day:20}")
    private int maxPerDay;

    public AppointmentServiceImpl(DoctorMapper doctorMapper, IScheduleService scheduleService,
                                  IClientService clientService, WeChatTemplateService wxNotify) {
        this.doctorMapper = doctorMapper;
        this.scheduleService = scheduleService;
        this.clientService = clientService;
        this.wxNotify = wxNotify;
    }

    @Override
    public boolean deleteById(Integer appointmentId) {
        return this.lambdaUpdate()
                .eq(Appointment::getAppointmentId, appointmentId)
                .set(Appointment::getIsDel, System.currentTimeMillis())
                .update();
    }

    @Override
    public boolean deleteByIds(List<Integer> idGroup) {
        return this.lambdaUpdate()
                .in(Appointment::getAppointmentId, idGroup)
                .set(Appointment::getIsDel, System.currentTimeMillis())
                .update();
    }

    /**
     * 挂号/改期唯一写入口：字段校验 → 医生行锁（FOR UPDATE）→ 出诊时段校验 →
     * 双计数（整日 + 时段）→ 同一事务内写入。
     * 行锁把同一医生的所有挂号串行化，根除 count+insert 的并发超卖。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reserve(Appointment appoint, boolean isNew) {
        LocalDateTime date = appoint.getAppointmentDate();
        if (date == null) throw new BizException("就诊时间不能为空");
        if (appoint.getClientId() == null || appoint.getClientId() == 0) throw new BizException("请选择用户");
        if (appoint.getPetId() == null || appoint.getPetId() == 0) throw new BizException("请选择宠物");
        if (appoint.getDepartmentId() == null || appoint.getDepartmentId() == 0) throw new BizException("请选择科室");
        if (appoint.getDoctorId() == null || appoint.getDoctorId() == 0) throw new BizException("请选择医生");
        if (appoint.getAppointmentInfo() != null
                && appoint.getAppointmentInfo().getBytes().length > 64)
            throw new BizException("挂号描述不能超过64个字符");

        // 悲观锁：复用 doctor 主键，零新表；同一医生的挂号/改期互斥
        doctorMapper.lockDoctor(appoint.getDoctorId());

        LocalDate day = date.toLocalDate();
        int weekday = day.getDayOfWeek().getValue();
        int period = date.getHour() < 12 ? 1 : 2;
        if (!scheduleService.isOnDuty(appoint.getDoctorId(), day, period))
            throw new BizException("该医生当日该时段未排班（不出诊），请改选其他日期或医生");

        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
        List<Appointment> sameDay = this.lambdaQuery()
                .eq(Appointment::getDoctorId, appoint.getDoctorId())
                .ge(Appointment::getAppointmentDate, dayStart)
                .lt(Appointment::getAppointmentDate, dayEnd)
                .ne(appoint.getAppointmentId() != null, Appointment::getAppointmentId, appoint.getAppointmentId())
                .and(w -> w.eq(Appointment::getIsDel, 0).or().isNull(Appointment::getIsDel))
                .list();
        long dayCnt = sameDay.size();
        long periodCnt = sameDay.stream()
                .filter(a -> a.getAppointmentDate() != null
                        && (a.getAppointmentDate().getHour() < 12 ? 1 : 2) == period)
                .count();

        int slotLimit = scheduleService.maxCountOf(appoint.getDoctorId(), weekday, period);
        if (slotLimit <= 0) slotLimit = maxPerDay; // 未配置排班 → 只受每日硬上限约束
        if (dayCnt + 1 > maxPerDay)
            throw new BizException("该医生当日号源已满（每日最多 " + maxPerDay + " 个），请改选其他日期或医生");
        if (periodCnt + 1 > slotLimit)
            throw new BizException("该医生该时段号源已满（最多 " + slotLimit + " 个），请改选其他时段");

        boolean ok = isNew ? this.save(appoint) : this.updateById(appoint);
        // 微信旁路挂点：挂号成功通知（enabled=false/未绑 openid 自动跳过，异常不影响挂号事务）
        if (ok && isNew) {
            try {
                Doctor d = doctorMapper.selectById(appoint.getDoctorId());
                Client c = clientService.getById(appoint.getClientId());
                wxNotify.sendAppointmentSuccess(appoint.getClientId(),
                        c == null ? null : c.getClientName(),
                        d == null ? null : d.getDoctorName(),
                        appoint.getAppointmentDate());
            } catch (Exception e) {
                // 微信旁路：吞异常，主流程继续
            }
        }
        return ok;
    }

    @Override
    public IPage<AppointmentDto> selectByPage(String seaName, int seaType, int numPage, int pageSize) {
        return baseMapper.selectAppointmentPage(new Page<>(numPage, pageSize), seaName, seaType);
    }

    @Override
    public List<AppointmentDto> getDoctor(Integer doctorId) {
        return baseMapper.getDoctor(doctorId);
    }

    @Override
    public List<AppointmentDto> getClient(Integer clientId) {
        return baseMapper.getClient(clientId);
    }

    @Override
    public List<AppointmentDto2> getDoctorLog(Integer doctorId) {
        return baseMapper.getDoctorLog(doctorId);
    }

    @Override
    public List<AppointmentDto2> getFutureAppoint(Integer doctorId) {
        // 明日零点由 JVM 计算（与 FlowService 的 LocalDate.now() 同口径），不依赖数据库时区
        return baseMapper.getFutureAppoint(doctorId, LocalDate.now().plusDays(1).atStartOfDay());
    }
}
