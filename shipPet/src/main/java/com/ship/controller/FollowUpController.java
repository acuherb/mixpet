package com.ship.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ship.common.Result;
import com.ship.model.dto.FollowUpDto;
import com.ship.model.entity.Client;
import com.ship.model.entity.FollowUp;
import com.ship.model.entity.MedicalRecord;
import com.ship.security.model.RoleConst;
import com.ship.service.IFollowUpService;
import com.ship.service.IMedicalRecordService;
import com.ship.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 随访记录
 */
@RestController
@RequestMapping("/follow")
public class FollowUpController {
    private final IFollowUpService followService;
    private final IMedicalRecordService recordService;

    @Autowired
    public FollowUpController(IFollowUpService followService, IMedicalRecordService recordService) {
        this.followService = followService;
        this.recordService = recordService;
    }

    /** 创建随访计划 */
    @PostMapping
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE})
    public Result add(@RequestBody FollowUp followUp) {
        return followService.add(followUp);
    }

    /** 随访管理分页 */
    @GetMapping("/page")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.DOCTOR})
    public IPage<FollowUpDto> page(int numPage, int pageSize, Boolean status, String seaName) {
        return followService.selectPage(status, seaName, numPage, pageSize);
    }

    /** 某病历的随访记录：顾客只能看自己宠物的 */
    @GetMapping("/record/{recordId}")
    public List<FollowUp> byRecord(@PathVariable Integer recordId) {
        MedicalRecord r = recordService.getById(recordId);
        if (r == null) return List.of();
        if (UserUtil.getUser() instanceof Client client && !client.getClientId().equals(r.getClientId()))
            return List.of();
        return followService.listByRecord(recordId);
    }

    /** 完成随访 */
    @PutMapping("/done/{followId}")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE})
    public Result done(@PathVariable Integer followId, String result) {
        return followService.done(followId, result);
    }
}
