package com.mix.controller;

import com.mix.common.Result;
import com.mix.model.dto.SurgeryDto;
import com.mix.model.entity.Client;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.SurgeryConsent;
import com.mix.model.vo.ConsentSheetVo;
import com.mix.model.vo.SurgeryConsentVo;
import com.mix.security.model.MyUser;
import com.mix.security.model.RoleConst;
import com.mix.service.ISurgeryConsentService;
import com.mix.service.ISurgeryService;
import com.mix.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 手术/麻醉知情同意书
 */
@RestController
@RequestMapping("/surgery/consent")
public class SurgeryConsentController {
    private final ISurgeryConsentService consentService;
    private final ISurgeryService surgeryService;

    @Autowired
    public SurgeryConsentController(ISurgeryConsentService consentService, ISurgeryService surgeryService) {
        this.consentService = consentService;
        this.surgeryService = surgeryService;
    }

    /** 签署/更新同意书 */
    @PostMapping
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE})
    public Result sign(@RequestBody SurgeryConsentVo vo) {
        return consentService.sign(vo, operatorName());
    }

    /** 某手术的同意书：顾客只能看自己宠物的 */
    @GetMapping("/{surgeryId}")
    public SurgeryConsent get(@PathVariable Integer surgeryId) {
        if (!owned(surgeryId)) return null;
        return consentService.getBySurgery(surgeryId);
    }

    /** 可打印的同意书数据：顾客只能看自己宠物的 */
    @GetMapping("/sheet/{surgeryId}")
    public ConsentSheetVo sheet(@PathVariable Integer surgeryId) {
        if (!owned(surgeryId)) return null;
        return consentService.consentSheet(surgeryId);
    }

    private boolean owned(Integer surgeryId) {
        SurgeryDto dto = surgeryService.selectDetail(surgeryId);
        if (dto == null) return false;
        if (UserUtil.getUser() instanceof Client client)
            return client.getClientId().equals(dto.getClientId());
        return true;
    }

    private String operatorName() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (p instanceof MyUser myUser) {
            Object role = myUser.role();
            if (role instanceof Doctor d && d.getDoctorName() != null) return d.getDoctorName();
            if (role instanceof Client c && c.getClientName() != null) return c.getClientName();
            return myUser.getUsername();
        }
        return "系统";
    }
}
