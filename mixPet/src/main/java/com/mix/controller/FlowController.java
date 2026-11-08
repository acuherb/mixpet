package com.mix.controller;

import com.mix.common.Result;
import com.mix.model.dto.DoctorFlowVo;
import com.mix.security.model.RoleConst;
import com.mix.service.FlowService;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 诊疗流（闭环状态机）脊柱接口。三端共用同一条"流"的语言，
 * 各端快照由 {@link FlowService} 从业务表派生，权限在此按角色强制隔离。
 */
@RestController
@RequestMapping("/flow")
public class FlowController {

    private final FlowService flowService;

    public FlowController(FlowService flowService) {
        this.flowService = flowService;
    }

    /** 医生视角：待诊→接诊中→待结果→待结算→待随访（所有在岗员工，含临时工） */
    @GetMapping("/doctorPipeline")
    @Secured({RoleConst.LOSER, RoleConst.NURSE, RoleConst.DOCTOR, RoleConst.MANAGER})
    public Result doctorPipeline() {
        return Result.data(flowService.doctorPipeline());
    }

    /** 前台视角：今日预约→待收银→今日已诊（护士及以上，临时工不可见） */
    @GetMapping("/receptionPipeline")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER})
    public Result receptionPipeline() {
        return Result.data(flowService.receptionPipeline());
    }

    /** 管理视角：经营指标流（管理层及以上，普通医生/护士不可见） */
    @GetMapping("/managerPipeline")
    @Secured(RoleConst.MANAGER)
    public Result managerPipeline() {
        return Result.data(flowService.managerPipeline());
    }
}
