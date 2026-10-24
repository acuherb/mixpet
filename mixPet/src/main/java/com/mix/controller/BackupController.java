package com.mix.controller;

import com.mix.common.Result;
import com.mix.model.vo.BackupFileVo;
import com.mix.security.model.RoleConst;
import com.mix.service.IBackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 数据备份 / 还原（超管 ADMIN 专用；备份列表/下载对 MANAGER 开放）
 *
 * 还原是毁库操作：后端四道闸（文件名白名单 + 二次确认 + 还原前自动快照 + 互斥锁），
 * 且仅 ADMIN 角色可调用（MANAGER 只能备份/下载）。
 */
@RestController
@RequestMapping("/backup")
public class BackupController {
    private final IBackupService backupService;

    @Autowired
    public BackupController(IBackupService backupService) {
        this.backupService = backupService;
    }

    /** 备份文件列表 */
    @GetMapping("/list")
    @Secured({RoleConst.ADMIN, RoleConst.MANAGER})
    public List<BackupFileVo> list() {
        return backupService.list();
    }

    /** 立即备份（全库 mysqldump） */
    @PostMapping("/create")
    @Secured({RoleConst.ADMIN, RoleConst.MANAGER})
    public Result create() {
        try {
            String name = backupService.create("mixpet_");
            return Result.success("备份成功：" + name);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 下载备份文件（含全库数据，仅限管理者以上） */
    @GetMapping("/download")
    @Secured({RoleConst.ADMIN, RoleConst.MANAGER})
    public ResponseEntity<FileSystemResource> download(@RequestParam String name) {
        File f = backupService.resolve(name);
        if (f == null) return ResponseEntity.notFound().build();
        String encoded = URLEncoder.encode(f.getName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(f));
    }

    /** 删除备份文件 */
    @DeleteMapping("/{name}")
    @Secured({RoleConst.ADMIN, RoleConst.MANAGER})
    public Result delete(@PathVariable String name) {
        return Result.choice("删除", backupService.delete(name));
    }

    /** 受控还原（仅 ADMIN）：需二次输入文件名确认；还原前自动快照当前数据 */
    @PostMapping("/restore")
    @Secured(RoleConst.ADMIN)
    public Result restore(@RequestBody Map<String, String> body) {
        try {
            String msg = backupService.restore(body.get("name"), body.get("confirmName"));
            return msg.startsWith("还原成功") ? Result.success(msg) : Result.error(msg);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }
}
