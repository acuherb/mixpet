package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.annotation.FlowChanged;
import com.mix.common.Result;
import com.mix.model.dto.FollowUpDto;
import com.mix.model.entity.Client;
import com.mix.model.entity.FollowUp;
import com.mix.model.entity.MedicalRecord;
import com.mix.security.model.RoleConst;
import com.mix.service.IFollowUpService;
import com.mix.service.IMedicalRecordService;
import com.mix.util.UserUtil;
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
    @FlowChanged(stage = "follow", text = "随访已完成，闭环结束")
    public Result done(@PathVariable Integer followId, String result) {
        return followService.done(followId, result);
    }
}
