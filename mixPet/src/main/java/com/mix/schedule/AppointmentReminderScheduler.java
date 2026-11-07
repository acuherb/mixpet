package com.mix.schedule;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mix.common.SendMail;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 预约就诊提醒：每日 08:00 扫描今明两日待就诊的挂号。
 * 员工侧生成一条系统公告；顾客侧按人合并，逐人发送只含自己预约的邮件（真正触达）。
 */
@Component
public class AppointmentReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(AppointmentReminderScheduler.class);
    private static final int HORIZON_HOURS = 36;

    private final AppointmentMapper appointmentMapper;
    private final PetMapper petMapper;
    private final ClientMapper clientMapper;
    private final DoctorMapper doctorMapper;
    private final INoticeService noticeService;
    private final SendMail sendMail;
    /** 同一天只发一轮邮件，防服务重启重复打扰（单实例部署） */
    private final Set<String> mailedDays = ConcurrentHashMap.newKeySet();

    public AppointmentReminderScheduler(AppointmentMapper appointmentMapper, PetMapper petMapper,
                                        ClientMapper clientMapper, DoctorMapper doctorMapper,
                                        INoticeService noticeService, SendMail sendMail) {
        this.appointmentMapper = appointmentMapper;
        this.petMapper = petMapper;
        this.clientMapper = clientMapper;
        this.doctorMapper = doctorMapper;
        this.noticeService = noticeService;
        this.sendMail = sendMail;
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void remindUpcomingAppointments() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime horizon = now.plusHours(HORIZON_HOURS);
            List<Appointment> list = appointmentMapper.selectList(Wrappers.<Appointment>lambdaQuery()
                    .ge(Appointment::getAppointmentDate, now.minusHours(1))
                    .le(Appointment::getAppointmentDate, horizon)
                    .eq(Appointment::getIsSuc, false));
            if (list.isEmpty()) return;

            String title = "预约就诊提醒 " + LocalDate.now();
            boolean firstNoticeToday = noticeService.lambdaQuery()
                    .eq(Notice::getNoticeTitle, title).count() == 0;
            if (firstNoticeToday) {
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
            }

            if (!mailedDays.add("appt:" + LocalDate.now())) return;
            Map<Integer, List<Appointment>> byClient = list.stream()
                    .filter(a -> a.getClientId() != null)
                    .collect(Collectors.groupingBy(Appointment::getClientId, HashMap::new, Collectors.toList()));
            int sent = 0;
            for (Map.Entry<Integer, List<Appointment>> e : byClient.entrySet()) {
                Client c = clientMapper.selectById(e.getKey());
                if (c == null || c.getClientEmail() == null) {
                    log.warn("客户#{} 未配置邮箱，跳过预约提醒邮件", e.getKey());
                    continue;
                }
                StringBuilder personal = new StringBuilder("您有以下待就诊预约，请按时到院：\n");
                for (Appointment x : e.getValue()) {
                    Pet p = petMapper.selectById(x.getPetId());
                    Doctor d = doctorMapper.selectById(x.getDoctorId());
                    personal.append("- ").append(x.getAppointmentDate().toLocalDate())
                            .append(" ").append(String.format("%02d", x.getAppointmentDate().getHour())).append("时 ")
                            .append(p != null ? p.getPetName() : "宠物")
                            .append(" → ").append(d != null ? d.getDoctorName() : "医生").append("\n");
                }
                if (sendMail.sendReminderMail(c.getClientEmail(),
                        "就诊提醒 " + LocalDate.now(), personal.toString())) sent++;
            }
            log.info("预约提醒邮件已发送 {} 位客户", sent);
        } catch (Exception e) {
            log.error("预约就诊提醒任务异常", e);
        }
    }
}
