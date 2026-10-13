package com.ship.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ship.common.Result;
import com.ship.mapper.MedicalRecordMapper;
import com.ship.model.dto.LabOrderDto;
import com.ship.model.dto.RecordDto;
import com.ship.model.entity.Client;
import com.ship.model.entity.Doctor;
import com.ship.model.entity.MedicalRecord;
import com.ship.model.vo.LabOrderVo;
import com.ship.model.vo.LabResultVo;
import com.ship.model.vo.RequisitionVo;
import com.ship.security.model.MyUser;
import com.ship.security.model.RoleConst;
import com.ship.service.ILabOrderService;
import com.ship.service.IMedicalRecordService;
import com.ship.util.UserUtil;
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

    /** 录入化验结果 */
    @PutMapping("/result")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER})
    public Result saveResult(@RequestBody LabResultVo vo) {
        return labService.saveResult(vo, operatorName());
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