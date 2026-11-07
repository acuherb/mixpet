package com.mix.model.dto;

/**
 * 诊疗流中的一个"患者卡片"——闭环状态机的可视单元。
 * key 在同一环节内唯一；actionPath 指向下一步该去的深页。
 */
public class FlowItem {
    public String key;
    public String petName;
    public String sub;
    public String flag;        // 角标，如"危急"
    public String cls;         // 卡片样式类，如"danger"
    public String stageKey;    // 当前所处环节
    public String nextLabel;   // 下一步动作文案
    public String actionPath;  // 下一步跳转路径
    public String clientName;

    public FlowItem() {}

    public FlowItem(String key, String petName, String sub, String flag, String cls,
                    String stageKey, String nextLabel, String actionPath, String clientName) {
        this.key = key; this.petName = petName; this.sub = sub; this.flag = flag; this.cls = cls;
        this.stageKey = stageKey; this.nextLabel = nextLabel; this.actionPath = actionPath; this.clientName = clientName;
    }
}
