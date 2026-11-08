package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.annotation.AuditLog;
import com.mix.annotation.FlowChanged;
import com.mix.common.Result;
import com.mix.mapper.MedicalRecordMapper;
import com.mix.model.dto.LabOrderDto;
import com.mix.model.dto.RecordDto;
import com.mix.model.entity.Client;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.MedicalRecord;
import com.mix.model.vo.LabOrderVo;
import com.mix.model.vo.LabResultVo;
import com.mix.model.vo.RequisitionVo;
import com.mix.security.model.MyUser;
import com.mix.security.model.RoleConst;
import com.mix.service.ILabOrderService;
import com.mix.service.IMedicalRecordService;
import com.mix.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 化验单：医生开单、化验室录入结果、顾客查看结果
 */
@RestController
@RequestMapping("/lab")
public class LabOrderController {
    private final ILabOrderService labService;
    private final IMedicalRecordService recordService;
    private final MedicalRecordMapper recordMapper;

    @Autowired
    public LabOrderController(ILabOrderService labService, IMedicalRecordService recordService, MedicalRecordMapper recordMapper) {
        this.labService = labService;
        this.recordService = recordService;
        this.recordMapper = recordMapper;
    }

    /** 医生开化验单（含计费） */
    @PostMapping("/order")
    @Secured(RoleConst.DOCTOR)
    @FlowChanged(stage = "result", text = "新开化验单，已流入待结果")
    public Result createOrder(@RequestBody LabOrderVo vo) {
        if (UserUtil.getUser() instanceof Doctor doctor) return labService.createOrder(vo, doctor);
        return Result.error("请登录医生账号");
    }

    /** 化验单列表（后台录入 / 医生查看） */
    @GetMapping("/page")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.DOCTOR})
    public IPage<LabOrderDto> getPage(int numPage, int pageSize, Integer status) {
        return labService.selectPage(status, numPage, pageSize);
    }

    /** 某病历的化验单（含明细）：顾客只能查自己宠物 */
    @GetMapping("/record/{recordId}")
    public List<LabOrderDto> selectByRecord(@PathVariable Integer recordId) {
        if (UserUtil.getUser() instanceof Client client) {
            MedicalRecord record = recordService.getById(recordId);
            if (record == null || !client.getClientId().equals(record.getClientId())) return null;
        }
        return labService.selectByRecord(recordId);
    }

    /** 某医生的接诊病历（开化验单时选病历） */
    @GetMapping("/records")
    @Secured(RoleConst.DOCTOR)
    public List<RecordDto> selectDoctorRecords() {
        if (UserUtil.getUser() instanceof Doctor doctor) {
            return recordMapper.selectDoctorRecords(doctor.getDoctorId(), 50);
        }
        return null;
    }

    /** 录入化验结果（含修订留痕与服务端异常判定） */
    @PutMapping("/result")
    @AuditLog(module = "lab", action = "saveResult", target = "录入检验结果")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER})
    @FlowChanged(stage = "result", text = "化验结果已回报，可复核/结算")
    public Result saveResult(@RequestBody LabResultVo vo) {
        return labService.saveResult(vo, operatorName());
    }

    /** 复核检验报告：录入人与审核人分离 */
    @PutMapping("/result/review/{orderId}")
    @AuditLog(module = "lab", action = "reviewResult", target = "检验报告复核")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER})
    public Result reviewResult(@PathVariable Integer orderId) {
        return labService.reviewResult(orderId, operatorName());
    }

    /** 危急值处置闭环：标记已处置并记录处置人 */
    @PutMapping("/result/critical/{orderId}")
    @AuditLog(module = "lab", action = "handleCritical", target = "危急值处置")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER})
    public Result handleCritical(@PathVariable Integer orderId) {
        return labService.handleCritical(orderId, operatorName());
    }

    /** 撤销复核（管理层）：复核锁解除后可再次修订，全程留痕 */
    @PutMapping("/result/revoke/{orderId}")
    @AuditLog(module = "lab", action = "revokeReview", target = "撤销化验复核")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public Result revokeReview(@PathVariable Integer orderId) {
        return labService.revokeReview(orderId, operatorName());
    }

    /** 作废化验单（未出结果）：冲减未付账单检验费 */
    @PutMapping("/cancel/{orderId}")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public Result cancel(@PathVariable Integer orderId) {
        return labService.cancel(orderId);
    }

    /** 检验申请单（国标，可打印） */
    @GetMapping("/requisition/{orderId}")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.DOCTOR})
    public RequisitionVo requisition(@PathVariable Integer orderId) {
        return labService.selectRequisition(orderId);
    }

    /** 当前操作人名称（写 result_by） */
    private String operatorName() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (p instanceof MyUser myUser) {
            Object role = myUser.role();
            if (role instanceof Doctor d && d.getDoctorName() != null) return d.getDoctorName();
            if (role instanceof Client c && c.getClientName() != null) return c.getClientName();
            return myUser.getUsername();
        }
        return "管理员";
    }
}