package com.mix.controller;

import com.mix.common.Result;
import com.mix.model.entity.Client;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.HospitalAdvice;
import com.mix.model.entity.MedicalRecord;
import com.mix.security.model.RoleConst;
import com.mix.service.IHospitalAdviceService;
import com.mix.service.IMedicalRecordService;
import com.mix.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 住院医嘱单：下达/停嘱/完成/查询（与处方、病历联动）
 */
@RestController
@RequestMapping("/hospitalAdvice")
public class HospitalAdviceController {
    private final IHospitalAdviceService adviceService;
    private final IMedicalRecordService recordService;

    @Autowired
    public HospitalAdviceController(IHospitalAdviceService adviceService, IMedicalRecordService recordService) {
        this.adviceService = adviceService;
        this.recordService = recordService;
    }

    /** 下达医嘱（住院中） */
    @PostMapping
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER})
    public Result add(@RequestBody HospitalAdvice advice) {
        if (advice.getHospitalId() == null || advice.getRecordId() == null)
            return Result.error("缺少住院/病历信息");
        if (advice.getItemName() == null || advice.getItemName().isBlank())
            return Result.error("请填写医嘱项目名称");
        return Result.choice("下达", adviceService.addAdvice(advice));
    }

    /** 批量下达（从本病历处方导入） */
    @PostMapping("/batch")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER})
    public Result addBatch(@RequestBody List<HospitalAdvice> list) {
        if (list == null || list.isEmpty()) return Result.error("没有要导入的医嘱");
        return Result.choice("导入", adviceService.addBatch(list));
    }

    /** 停嘱（原因必填，留痕不删除） */
    @PutMapping("/stop/{id}")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER})
    public Result stop(@PathVariable Integer id, String reason) {
        String err = adviceService.stop(id, reason);
        return err == null ? Result.success("已停嘱") : Result.error(err);
    }

    /** 标记完成 */
    @PutMapping("/done/{id}")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER})
    public Result done(@PathVariable Integer id) {
        String err = adviceService.done(id);
        return err == null ? Result.success("已完成") : Result.error(err);
    }

    /** 某次住院的医嘱列表 */
    @GetMapping("/hospital/{hospitalId}")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER})
    public List<HospitalAdvice> byHospital(@PathVariable Integer hospitalId) {
        return adviceService.listByHospital(hospitalId);
    }

    /** 某病历的医嘱（跨住院）：顾客只能查自己宠物的 */
    @GetMapping("/record/{recordId}")
    public List<HospitalAdvice> byRecord(@PathVariable Integer recordId) {
        if (UserUtil.getUser() instanceof Client client) {
            MedicalRecord record = recordService.getById(recordId);
            if (record == null || !client.getClientId().equals(record.getClientId()))
                return List.of();
        }
        return adviceService.listByRecord(recordId);
    }
}
