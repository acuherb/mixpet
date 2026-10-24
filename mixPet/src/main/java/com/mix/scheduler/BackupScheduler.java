package com.mix.scheduler;

import com.mix.service.IBackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 每日凌晨自动备份（方案 C 的定时部分）
 *
 * 说明：
 * - 单机部署单实例，无需分布式锁；多实例部署时各实例都会触发（同一文件时间戳命名，
 *   极小概率重名时后者覆盖前者，不影响正确性）；
 * - 只清理「自动备份」（auto_ 前缀），手动备份与还原前快照（pre_restore_）永不自动删；
 * - 备份失败仅记日志，不影响其他定时任务（疫苗提醒/预约提醒）。
 */
@Component
public class BackupScheduler {
    private static final Logger log = LoggerFactory.getLogger(BackupScheduler.class);
    private final IBackupService backupService;

    @Value("${backup.dir:/home/debian/公共/mixpet/db-backup/runtime/}")
    private String backupDir;
    /** 自动备份开关（默认开） */
    @Value("${backup.auto-enabled:true}")
    private boolean autoEnabled;
    /** 自动备份保留份数（0=不清理；默认 7，约一周） */
    @Value("${backup.auto-keep:7}")
    private int autoKeep;

    public BackupScheduler(IBackupService backupService) {
        this.backupService = backupService;
    }

    /** 每天凌晨 02:00（与疫苗/预约提醒错峰） */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoBackup() {
        if (!autoEnabled) return;
        try {
            String name = backupService.create("auto_");
            log.info("定时自动备份成功：{}", name);
            keepAutoBackups();
        } catch (Exception e) {
            log.error("定时自动备份失败（不影响其他任务）", e);
        }
    }

    /** 保留策略：auto_ 前缀只留最近 N 份（新的在前） */
    private void keepAutoBackups() {
        if (autoKeep <= 0) return;
        File dir = new File(backupDir);
        File[] autos = dir.listFiles((d, n) -> n.startsWith("auto_") && n.endsWith(".sql"));
        if (autos == null || autos.length <= autoKeep) return;
        Arrays.sort(autos, Comparator.comparing(File::getName).reversed());
        for (int i = autoKeep; i < autos.length; i++) {
            if (autos[i].delete()) log.info("清理过期自动备份：{}", autos[i].getName());
        }
    }
}
