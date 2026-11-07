package com.mix.controller;

import com.mix.model.dto.DoctorFlowVo;
import com.mix.common.Result;
import com.mix.service.FlowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 诊疗流（闭环状态机）脊柱接口。后续可扩展 /flow/receptionPipeline、/flow/managerPipeline，
 * 让前台、管理端共用同一条"流"的语言。
 */
@RestController
@RequestMapping("/flow")
public class FlowController {

    private final FlowService flowService;

    public FlowController(FlowService flowService) {
        this.flowService = flowService;
    }

    /** 医生视角的诊疗流快照：待诊→接诊中→待结果→待结算→待随访 */
    @GetMapping("/doctorPipeline")
    public Result doctorPipeline() {
        return Result.data(flowService.doctorPipeline());
    }
}
