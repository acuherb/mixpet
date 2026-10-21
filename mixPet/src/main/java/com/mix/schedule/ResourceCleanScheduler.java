package com.mix.schedule;

import com.mix.service.INoticeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 冗余资源定时清理
 * 清理 avatar/ 中不被 client/doctor/pet 引用的孤儿头像，
 * 以及冗余的公告文件、聊天图片（复用 NoticeService.delImg 既有逻辑）
 */
@Component
public class ResourceCleanScheduler {
    private static final Logger log = LoggerFactory.getLogger(ResourceCleanScheduler.class);
    private final INoticeService noticeService;

    public ResourceCleanScheduler(INoticeService noticeService) {
        this.noticeService = noticeService;
    }

    /**
     * 每天凌晨 4 点清理一次冗余资源
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanUnusedResources() {
        try {
            boolean cleaned = noticeService.delImg();
            if (cleaned) log.info("定时清理冗余资源：已删除无引用文件");
        } catch (Exception e) {
            log.warn("定时清理冗余资源异常", e);
        }
    }
}
