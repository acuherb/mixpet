package com.mix.model.dto;

import java.util.List;

/**
 * 医生视角的诊疗流聚合视图（闭环状态机快照）。
 * 单一接口返回全部环节，前端无需再并发 6 个请求。
 */
public class DoctorFlowVo {
    public List<FlowStage> stages;
    public String serverTime;

    public DoctorFlowVo(List<FlowStage> stages, String serverTime) {
        this.stages = stages; this.serverTime = serverTime;
    }
}
