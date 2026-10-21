package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.common.Result;
import com.mix.model.dto.ImageOrderDto;
import com.mix.model.entity.Client;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.MedicalRecord;
import com.mix.model.vo.ImageOrderVo;
import com.mix.model.vo.ImageReportVo;
import com.mix.model.vo.RequisitionVo;
import com.mix.security.model.RoleConst;
import com.mix.service.IImageOrderService;
import com.mix.service.IMedicalRecordService;
import com.mix.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 影像检查：医生开单、后台报告录入、顾客查看
 */
@RestController
@RequestMapping("/imageOrder")
public class ImageOrderController {
    private final IImageOrderService orderService;
    private final IMedicalRecordService recordService;

    @Autowired
    public ImageOrderController(IImageOrderService orderService, IMedicalRecordService recordService) {
        this.orderService = orderService;
        this.recordService = recordService;
    }

    /** 医生开单 */
    @PostMapping
    @Secured(RoleConst.DOCTOR)
    public Result createOrder(@RequestBody ImageOrderVo vo) {
        if (UserUtil.getUser() instanceof Doctor doctor) return orderService.createOrder(vo, doctor);
        return Result.error("请登录医生账号");
    }

    /** 影像报告录入 */
    @PutMapping("/report")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER})
    public Result saveReport(@RequestBody ImageReportVo vo) {
        return orderService.saveReport(vo);
    }

    /** 作废申请单（未出报告）：冲减未付账单影像费 */
    @PutMapping("/cancel/{orderId}")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public Result cancel(@PathVariable Integer orderId) {
        return orderService.cancel(orderId);
    }

    /** 申请单分页 */
    @GetMapping("/page")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.DOCTOR})
    public IPage<ImageOrderDto> getPage(int numPage, int pageSize, Boolean status) {
        return orderService.selectPage(status, numPage, pageSize);
    }

    /** 某病历的申请单：顾客只能查本人 */
    @GetMapping("/record/{recordId}")
    public List<ImageOrderDto> selectByRecord(@PathVariable Integer recordId) {
        if (UserUtil.getUser() instanceof Client client) {
            MedicalRecord record = recordService.getById(recordId);
            if (record == null || !client.getClientId().equals(record.getClientId())) return null;
        }
        return orderService.selectByRecord(recordId);
    }

    /** 影像申请单（国标，可打印） */
    @GetMapping("/requisition/{orderId}")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.DOCTOR})
    public RequisitionVo requisition(@PathVariable Integer orderId) {
        return orderService.selectRequisition(orderId);
    }
}