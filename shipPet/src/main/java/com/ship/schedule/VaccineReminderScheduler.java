package com.ship.schedule;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ship.mapper.ClientMapper;
import com.ship.mapper.ImmuneRecordMapper;
import com.ship.mapper.PetMapper;
import com.ship.model.entity.Client;
import com.ship.model.entity.ImmuneRecord;
import com.ship.model.entity.Notice;
import com.ship.model.entity.Pet;
import com.ship.service.INoticeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 疫苗到期提醒调度：每日 08:00 扫描临近下一针接种时间的免疫记录，自动生成系统公告
 */
@Component
public class VaccineReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(VaccineReminderScheduler.class);
    /** 提前提醒天数 */
    private static final int HORIZON_DAYS = 7;

    private final ImmuneRecordMapper immuneMapper;
    private final PetMapper petMapper;
    private final ClientMapper clientMapper;
    private final INoticeService noticeService;

    public VaccineReminderScheduler(ImmuneRecordMapper immuneMapper, PetMapper petMapper,
                                    ClientMapper clientMapper, INoticeService noticeService) {
        this.immuneMapper = immuneMapper;
        this.petMapper = petMapper;
        this.clientMapper = clientMapper;
        this.noticeService = noticeService;
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
            // 每日仅生成一条，避免重复刷屏
            String title = "疫苗到期提醒 " + LocalDate.now();
            if (noticeService.lambdaQuery().eq(Notice::getNoticeTitle, title).count() > 0) return;
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
        } catch (Exception e) {
            log.error("疫苗到期提醒任务异常", e);
        }
    }
}
