package com.mix.service.impl;

import com.mix.model.vo.BackupFileVo;
import com.mix.service.IBackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据备份 / 还原实现
 *
 * 命令走子进程（mysqldump / mysql），凭据经 MYSQL_PWD 环境变量传入
 * （不进命令行参数，防 ps 泄露）；库名从 JDBC URL 解析。
 */
@Service
public class BackupServiceImpl implements IBackupService {
    private static final Logger log = LoggerFactory.getLogger(BackupServiceImpl.class);
    /** 备份文件名安全白名单：仅字母数字下划线点横线，且 .sql 结尾 */
    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9_.-]+\\.sql");
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    /** 备份/还原互斥：任一进行中拒绝另一操作 */
    private final AtomicBoolean busy = new AtomicBoolean(false);

    @Value("${backup.dir:/home/debian/公共/mixpet/db-backup/runtime/}")
    private String backupDir;
    @Value("${spring.datasource.url:}")
    private String jdbcUrl;
    @Value("${spring.datasource.username:}")
    private String dbUser;
    @Value("${spring.datasource.password:}")
    private String dbPassword;

    /** 从 JDBC URL 解析库名：jdbc:mysql://host:3306/pet_mix?xxx → pet_mix */
    private String dbName() {
        Matcher m = Pattern.compile("jdbc:mysql://[^/]+/([A-Za-z0-9_]+)").matcher(jdbcUrl);
        return m.find() ? m.group(1) : "pet_mix";
    }

    private String host() {
        Matcher m = Pattern.compile("jdbc:mysql://([^:/]+)").matcher(jdbcUrl);
        return m.find() ? m.group(1) : "127.0.0.1";
    }

    private String port() {
        Matcher m = Pattern.compile("jdbc:mysql://[^:/]+:(\\d+)").matcher(jdbcUrl);
        return m.find() ? m.group(1) : "3306";
    }

    private File dir() {
        File d = new File(backupDir);
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private boolean nameOk(String name) {
        return name != null && SAFE_NAME.matcher(name).matches()
                && !name.contains("..") && !name.contains("/") && !name.contains("\\");
    }

    @Override
    public List<BackupFileVo> list() {
        File[] files = dir().listFiles((d, n) -> n.endsWith(".sql"));
        if (files == null) return List.of();
        return Arrays.stream(files)
                .map(f -> {
                    BackupFileVo vo = new BackupFileVo();
                    vo.setName(f.getName());
                    vo.setSize(f.length());
                    try {
                        FileTime t = Files.getLastModifiedTime(f.toPath());
                        vo.setTime(LocalDateTime.ofInstant(t.toInstant(), java.time.ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    } catch (IOException e) {
                        vo.setTime("");
                    }
                    vo.setSnapshot(f.getName().startsWith("pre_restore_"));
                    return vo;
                })
                .sorted(Comparator.comparing(BackupFileVo::getName).reversed())
                .toList();
    }

    @Override
    public String create(String prefix) {
        if (!busy.compareAndSet(false, true)) throw new IllegalStateException("已有备份/还原任务在进行，请稍候");
        try {
            return doCreate(prefix);
        } finally {
            busy.set(false);
        }
    }

    /** 无锁的备份主体：供还原流程在持锁状态下调用（避免自己抢自己的锁导致死锁） */
    private String doCreate(String prefix) {
        String name = (prefix == null ? "mixpet_" : prefix) + FILE_TS.format(LocalDateTime.now()) + ".sql";
        File out = new File(dir(), name);
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "mysqldump", "-h" + host(), "-P" + port(),
                    "--default-character-set=utf8mb4",
                    "--single-transaction", "--routines", "--triggers",
                    "--hex-blob",
                    "-u" + dbUser, dbName());
            // 凭据走环境变量，不出现在命令行（防 ps 泄露）
            pb.environment().put("MYSQL_PWD", dbPassword == null ? "" : dbPassword);
            pb.redirectOutput(out);
            pb.redirectErrorStream(false);
            Process p = pb.start();
            String err = new String(p.getErrorStream().readAllBytes());
            int code = p.waitFor();
            if (code != 0 || !out.exists() || out.length() == 0) {
                out.delete();
                log.error("备份失败 code={} err={}", code, err);
                throw new IllegalStateException("备份失败：" + err.lines().findFirst().orElse("未知错误"));
            }
            writeChecksum(out);
            log.info("数据备份成功：{}（{} 字节）", name, out.length());
            return name;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("备份被中断");
        } catch (IOException e) {
            throw new IllegalStateException("备份IO异常：" + e.getMessage());
        }
    }

    /** 生成 SHA-256 校验文件，还原前可验证备份未被损坏/篡改 */
    private void writeChecksum(File sql) throws IOException {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(Files.readAllBytes(sql.toPath()));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            Files.writeString(new File(sql.getParentFile(), sql.getName() + ".sha256").toPath(), hex.toString());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 不可用", e);
        }
    }

    /** 还原前校验（无校验文件的历史备份仍允许还原） */
    private void verifyChecksum(File sql) {
        File sum = new File(sql.getParentFile(), sql.getName() + ".sha256");
        if (!sum.exists()) return;
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(Files.readAllBytes(sql.toPath()));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            if (!hex.toString().equalsIgnoreCase(Files.readString(sum.toPath()).trim())) {
                throw new IllegalStateException("备份文件校验失败（SHA-256 不匹配），已中止还原");
            }
        } catch (IOException | java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("备份校验读取失败，已中止还原", e);
        }
    }

    @Override
    public String restore(String name, String confirmName) {
        if (name == null || !name.equals(confirmName))
            throw new IllegalArgumentException("二次确认的文件名不一致，已中止（防误操作）");
        if (!nameOk(name)) throw new IllegalArgumentException("非法备份文件名");
        File src = new File(dir(), name);
        if (!src.exists()) throw new IllegalArgumentException("备份文件不存在：" + name);
        verifyChecksum(src);
        if (!busy.compareAndSet(false, true)) throw new IllegalStateException("已有备份/还原任务在进行，请稍候");
        try {
            // 闸3：还原前自动快照当前数据（出事用它回退）。
            // 必须调用无锁的 doCreate：原实现调用 create() 会抢同一把 busy 锁，导致还原永远不执行。
            String snapshot = doCreate("pre_restore_");
            // 执行还原：mysql < backup.sql
            ProcessBuilder pb = new ProcessBuilder(
                    "mysql", "-h" + host(), "-P" + port(),
                    "--default-character-set=utf8mb4",
                    "-u" + dbUser, dbName());
            pb.environment().put("MYSQL_PWD", dbPassword == null ? "" : dbPassword);
            pb.redirectInput(src);
            pb.redirectErrorStream(false);
            Process p = pb.start();
            String err = new String(p.getErrorStream().readAllBytes());
            int code = p.waitFor();
            if (code != 0) {
                log.error("还原失败 code={} err={}", code, err);
                return "还原失败（当前数据未损坏，自动快照 " + snapshot + " 已保留）："
                        + err.lines().findFirst().orElse("未知错误");
            }
            int tables = countTables();
            log.info("数据还原成功：{}（当前 {} 张表）", name, tables);
            return "还原成功（" + name + "），当前库共 " + tables + " 张表；还原前自动快照：" + snapshot + "（确认无误后可删除）";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "还原被中断";
        } catch (IOException e) {
            return "还原IO异常：" + e.getMessage();
        } finally {
            busy.set(false);
        }
    }

    /** 还原后核对：当前库表数 */
    private int countTables() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "mysql", "-h" + host(), "-P" + port(), "-N", "-e",
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='" + dbName() + "'",
                    "-u" + dbUser);
            pb.environment().put("MYSQL_PWD", dbPassword == null ? "" : dbPassword);
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor();
            return Integer.parseInt(out);
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public boolean delete(String name) {
        if (!nameOk(name)) return false;
        File f = new File(dir(), name);
        return f.exists() && f.delete();
    }

    @Override
    public File resolve(String name) {
        if (!nameOk(name)) return null;
        File f = new File(dir(), name);
        return f.exists() ? f : null;
    }
}
