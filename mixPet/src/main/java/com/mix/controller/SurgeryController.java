package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.common.Result;
import com.mix.model.dto.SurgeryDto;
import com.mix.model.entity.AnesthesiaRecord;
import com.mix.model.entity.Client;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.MedicalRecord;
import com.mix.model.entity.Surgery;
import com.mix.model.vo.SurgeryVo;
import com.mix.security.model.RoleConst;
import com.mix.service.IMedicalRecordService;
import com.mix.service.ISurgeryService;
import com.mix.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 手术/麻醉：排程、术中术后记录、麻醉记录
 */
@RestController
@RequestMapping("/surgery")
public class SurgeryController {
    private final ISurgeryService surgeryService;
    private final IMedicalRecordService recordService;

    @Autowired
    public SurgeryController(ISurgeryService surgeryService, IMedicalRecordService recordService) {
        this.surgeryService = surgeryService;
        this.recordService = recordService;
    }

    /** 新建手术单（排程+计费） */
    @PostMapping
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER})
    public Result create(@RequestBody SurgeryVo vo) {
        Doctor doctor = UserUtil.getUser() instanceof Doctor d ? d : null;
        return surgeryService.createOrder(vo, doctor);
    }

    /** 手术列表分页 */
    @GetMapping("/page")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.DOCTOR})
    public IPage<SurgeryDto> getPage(int numPage, int pageSize, Integer status) {
        return surgeryService.selectPage(status, numPage, pageSize);
    }

    /** 某病历的手术单：顾客只能查自己宠物 */
    @GetMapping("/record/{recordId}")
    public List<SurgeryDto> byRecord(@PathVariable Integer recordId) {
        if (UserUtil.getUser() instanceof Client client) {
            MedicalRecord record = recordService.getById(recordId);
            if (record == null || !client.getClientId().equals(record.getClientId())) return null;
        }
        return surgeryService.selectByRecord(recordId);
    }

    /** 手术单详情（含麻醉记录）：顾客只能看自己宠物的 */
    @GetMapping("/{surgeryId}")
    public SurgeryDto detail(@PathVariable Integer surgeryId) {
        SurgeryDto dto = surgeryService.selectDetail(surgeryId);
        if (dto == null) return null;
        if (UserUtil.getUser() instanceof Client client
                && !client.getClientId().equals(dto.getClientId())) return null;
        return dto;
    }

    /** 更新手术单（术前/术中/术后/状态/时间） */
    @PutMapping
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.DOCTOR})
    public Result update(@RequestBody Surgery surgery) {
        return surgeryService.update(surgery);
    }

    /** 保存/更新麻醉记录 */
    @PutMapping("/anesthesia")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.DOCTOR})
    public Result anesthesia(@RequestBody AnesthesiaRecord ar) {
        return surgeryService.saveAnesthesia(ar);
    }
}
