package com.ship.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ship.common.Result;
import com.ship.model.dto.ImageOrderDto;
import com.ship.model.entity.Client;
import com.ship.model.entity.Doctor;
import com.ship.model.entity.MedicalRecord;
import com.ship.model.vo.ImageOrderVo;
import com.ship.model.vo.ImageReportVo;
import com.ship.security.model.RoleConst;
import com.ship.service.IImageOrderService;
import com.ship.service.IMedicalRecordService;
import com.ship.util.UserUtil;
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
    @Secured({RoleConst.NURSE, RoleConst.MANAGER})
    public Result saveReport(@RequestBody ImageReportVo vo) {
        return orderService.saveReport(vo);
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
}