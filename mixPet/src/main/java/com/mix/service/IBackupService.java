package com.mix.service;

import com.mix.model.vo.BackupFileVo;

import java.util.List;

/**
 * 数据备份 / 还原 Service
 *
 * 安全设计（还原是毁库操作，四道闸）：
 * 1. 文件名白名单校验（仅 [A-Za-z0-9_.-] 且 .sql 结尾，禁路径字符）；
 * 2. 还原需二次输入文件名确认（confirmName 与 name 一致）；
 * 3. 还原前自动为当前数据创建快照（pre_restore_ 前缀），出事可回退；
 * 4. 备份/还原互斥锁，防并发；还原后返回当前表数供核对。
 */
public interface IBackupService {
    /** 备份文件列表（新的在前） */
    List<BackupFileVo> list();

    /** 立即备份（mysqldump 全库）；返回生成的文件名 */
    String create(String prefix);

    /** 受控还原：name 目标备份 + confirmName 二次确认；返回结果说明（含自动快照名） */
    String restore(String name, String confirmName);

    /** 删除指定备份（仅允许备份目录内的 .sql） */
    boolean delete(String name);

    /** 下载用：校验文件名合法且文件存在，返回磁盘文件（不存在返回 null） */
    java.io.File resolve(String name);
}
