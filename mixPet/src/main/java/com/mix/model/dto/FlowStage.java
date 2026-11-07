package com.mix.model.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 诊疗流中的一个环节（状态机的一个状态）。items 为该环节当前的患者卡片。
 */
public class FlowStage {
    public String key;
    public String title;
    public String desc;
    public String actionPath;   // 环节级"去处理"入口
    public String actionLabel;
    public int count;
    public List<FlowItem> items = new ArrayList<>();

    public FlowStage() {}

    public FlowStage(String key, String title, String desc, String actionPath, String actionLabel) {
        this.key = key; this.title = title; this.desc = desc; this.actionPath = actionPath; this.actionLabel = actionLabel;
    }

    public void add(FlowItem item) { items.add(item); count = items.size(); }
}
