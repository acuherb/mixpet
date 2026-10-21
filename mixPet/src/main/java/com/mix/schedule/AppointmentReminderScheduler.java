package com.mix.schedule;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mix.mapper.AppointmentMapper;
import com.mix.mapper.ClientMapper;
import com.mix.mapper.DoctorMapper;
import com.mix.mapper.PetMapper;
import com.mix.model.entity.Appointment;
import com.mix.model.entity.Client;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.Notice;
import com.mix.model.entity.Pet;
import com.mix.service.INoticeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约就诊提醒调度：每日 08:00 扫描今明两日待就诊的挂号，自动生成系统公告（医护提前准备）
 */
@Component
public class AppointmentReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(AppointmentReminderScheduler.class);
    /** 提醒窗口：未来多少小时内到诊 */
    private static final int HORIZON_HOURS = 36;

    private final AppointmentMapper appointmentMapper;
    private final PetMapper petMapper;
    private final ClientMapper clientMapper;
    private final DoctorMapper doctorMapper;
    private final INoticeService noticeService;

    public AppointmentReminderScheduler(AppointmentMapper appointmentMapper, PetMapper petMapper,
                                        ClientMapper clientMapper, DoctorMapper doctorMapper,
                                        INoticeService noticeService) {
        this.appointmentMapper = appointmentMapper;
        this.petMapper = petMapper;
        this.clientMapper = clientMapper;
        this.doctorMapper = doctorMapper;
        this.noticeService = noticeService;
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void remindUpcomingAppointments() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime horizon = now.plusHours(HORIZON_HOURS);
            // 待就诊(isSuc=false)且就诊时间在(now-1h, horizon]内的挂号（MP 逻辑删除自动过滤 is_del=0）
            List<Appointment> list = appointmentMapper.selectList(Wrappers.<Appointment>lambdaQuery()
                    .ge(Appointment::getAppointmentDate, now.minusHours(1))
                    .le(Appointment::getAppointmentDate, horizon)
                    .eq(Appointment::getIsSuc, false));
            if (list.isEmpty()) return;
            // 每日仅生成一条，避免重复刷屏
            String title = "预约就诊提醒 " + LocalDate.now();
            if (noticeService.lambdaQuery().eq(Notice::getNoticeTitle, title).count() > 0) return;
            StringBuilder sb = new StringBuilder("今明两日有以下待就诊预约，请提前准备：\n");
            for (Appointment a : list) {
                Pet pet = petMapper.selectById(a.getPetId());
                Client client = clientMapper.selectById(a.getClientId());
                Doctor doctor = doctorMapper.selectById(a.getDoctorId());
                sb.append("- ").append(a.getAppointmentDate().toLocalDate())
                        .append(" ").append(String.format("%02d", a.getAppointmentDate().getHour())).append("时 ")
                        .append(pet != null ? pet.getPetName() : "宠物#" + a.getPetId())
                        .append("（").append(client != null ? client.getClientName() : "客户#" + a.getClientId()).append("）")
                        .append(" → ").append(doctor != null ? doctor.getDoctorName() : "医生#" + a.getDoctorId())
                        .append("，备注：").append(a.getAppointmentInfo() == null || a.getAppointmentInfo().isBlank()
                                ? "无" : a.getAppointmentInfo())
                        .append("\n");
            }
            noticeService.createSystemNotice(title, sb.toString());
            log.info("生成预约就诊提醒公告，共{}条", list.size());
        } catch (Exception e) {
            log.error("预约就诊提醒任务异常", e);
        }
    }
}
