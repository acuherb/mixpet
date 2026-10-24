package com.mix.model.vo;

/**
 * 备份文件信息（列表展示）
 */
public class BackupFileVo {
    /** 文件名（mixpet_时间.sql 手动备份 / pre_restore_时间.sql 还原前自动快照） */
    private String name;
    /** 大小（字节） */
    private long size;
    /** 修改时间（yyyy-MM-dd HH:mm:ss） */
    private String time;
    /** 是否还原前自动快照（UI 标注，与手动备份区分） */
    private boolean snapshot;

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public long getSize() { return size; }

    public void setSize(long size) { this.size = size; }

    public String getTime() { return time; }

    public void setTime(String time) { this.time = time; }

    public boolean isSnapshot() { return snapshot; }

    public void setSnapshot(boolean snapshot) { this.snapshot = snapshot; }
}
