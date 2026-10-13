package com.ship.controller;

import com.ship.mapper.ImmuneRecordMapper;
import com.ship.mapper.MedicalRecordMapper;
import com.ship.model.vo.RemindVo;
import com.ship.security.model.RoleConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 随访/复诊提醒：应复诊病历 + 应接种疫苗（近30天）
 */
@RestController
@RequestMapping("/remind")
public class RemindController {
    private final MedicalRecordMapper recordMapper;
    private final ImmuneRecordMapper immuneMapper;

    @Autowired
    public RemindController(MedicalRecordMapper recordMapper, ImmuneRecordMapper immuneMapper) {
        this.recordMapper = recordMapper;
        this.immuneMapper = immuneMapper;
    }

    @GetMapping
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.DOCTOR})
    public RemindVo list() {
        RemindVo vo = new RemindVo();
        vo.setRevisits(recordMapper.selectDueRevisits());
        vo.setVaccines(immuneMapper.selectDueVaccines());
        return vo;
    }
}
