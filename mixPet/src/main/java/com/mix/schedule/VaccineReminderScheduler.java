package com.mix.schedule;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mix.common.SendMail;
import com.mix.mapper.ClientMapper;
import com.mix.mapper.ImmuneRecordMapper;
import com.mix.mapper.PetMapper;
import com.mix.model.entity.Client;
import com.mix.model.entity.ImmuneRecord;
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

/** 疫苗到期提醒：员工侧公告 + 顾客侧逐人邮件（只含本人宠物信息） */
@Component
public class VaccineReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(VaccineReminderScheduler.class);
    private static final int HORIZON_DAYS = 7;

    private final ImmuneRecordMapper immuneMapper;
    private final PetMapper petMapper;
    private final ClientMapper clientMapper;
    private final INoticeService noticeService;
    private final SendMail sendMail;
    private final Set<String> mailedDays = ConcurrentHashMap.newKeySet();

    public VaccineReminderScheduler(ImmuneRecordMapper immuneMapper, PetMapper petMapper,
                                    ClientMapper clientMapper, INoticeService noticeService,
                                    SendMail sendMail) {
        this.immuneMapper = immuneMapper;
        this.petMapper = petMapper;
        this.clientMapper = clientMapper;
        this.noticeService = noticeService;
        this.sendMail = sendMail;
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void remindDueVaccines() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime horizon = now.plusDays(HORIZON_DAYS);
            List<ImmuneRecord> due = immuneMapper.selectList(Wrappers.<ImmuneRecord>lambdaQuery()
                    .isNotNull(ImmuneRecord::getNextTime)
                    .ge(ImmuneRecord::getNextTime, now.minusDays(1))
                    .le(ImmuneRecord::getNextTime, horizon)
                    .eq(ImmuneRecord::getIsDel, 0));
            if (due.isEmpty()) return;

            String title = "疫苗到期提醒 " + LocalDate.now();
            boolean firstNoticeToday = noticeService.lambdaQuery()
                    .eq(Notice::getNoticeTitle, title).count() == 0;
            if (firstNoticeToday) {
                StringBuilder sb = new StringBuilder("以下宠物临近下一针疫苗接种时间，请相关客户及时安排：\n");
                for (ImmuneRecord r : due) {
                    Pet pet = petMapper.selectById(r.getPetId());
                    Client client = clientMapper.selectById(r.getClientId());
                    sb.append("- ").append(pet != null ? pet.getPetName() : "宠物#" + r.getPetId())
                            .append("（").append(client != null ? client.getClientName() : "客户#" + r.getClientId()).append("）")
                            .append("，建议接种时间：").append(r.getNextTime().toLocalDate()).append("\n");
                }
                noticeService.createSystemNotice(title, sb.toString());
                log.info("生成疫苗到期提醒公告，共{}条", due.size());
            }

            if (!mailedDays.add("vaccine:" + LocalDate.now())) return;
            Map<Integer, List<ImmuneRecord>> byClient = due.stream()
                    .filter(r -> r.getClientId() != null)
                    .collect(Collectors.groupingBy(ImmuneRecord::getClientId, HashMap::new, Collectors.toList()));
            int sent = 0;
            for (Map.Entry<Integer, List<ImmuneRecord>> e : byClient.entrySet()) {
                Client c = clientMapper.selectById(e.getKey());
                if (c == null || c.getClientEmail() == null) {
                    log.warn("客户#{} 未配置邮箱，跳过疫苗提醒邮件", e.getKey());
                    continue;
                }
                StringBuilder personal = new StringBuilder("您的宠物临近疫苗接种时间，请及时安排：\n");
                for (ImmuneRecord r : e.getValue()) {
                    Pet p = petMapper.selectById(r.getPetId());
                    personal.append("- ").append(p != null ? p.getPetName() : "宠物")
                            .append("，建议接种时间：").append(r.getNextTime().toLocalDate()).append("\n");
                }
                if (sendMail.sendReminderMail(c.getClientEmail(),
                        "疫苗到期提醒 " + LocalDate.now(), personal.toString())) sent++;
            }
            log.info("疫苗提醒邮件已发送 {} 位客户", sent);
        } catch (Exception e) {
            log.error("疫苗到期提醒任务异常", e);
        }
    }
}
