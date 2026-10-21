package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.common.Result;
import com.mix.model.dto.HospitalDto;
import com.mix.model.entity.Client;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.MedicalRecord;
import com.mix.model.vo.HospitalVo;
import com.mix.security.model.RoleConst;
import com.mix.service.IHospitalService;
import com.mix.service.IMedicalRecordService;
import com.mix.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 住院管理：入院登记、出院结算、住院列表与查看
 */
@RestController
@RequestMapping("/hospital")
public class HospitalController {
    private final IHospitalService hospitalService;
    private final IMedicalRecordService recordService;

    @Autowired
    public HospitalController(IHospitalService hospitalService, IMedicalRecordService recordService) {
        this.hospitalService = hospitalService;
        this.recordService = recordService;
    }

    /** 入院登记 */
    @PostMapping
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE})
    public Result saveIn(@RequestBody HospitalVo vo) {
        if (UserUtil.getUser() instanceof Doctor doctor) return hospitalService.saveIn(vo, doctor);
        return Result.error("请登录医生账号");
    }

    /** 出院结算（计住院费入账单） */
    @PutMapping("/discharge/{id}")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER})
    public Result discharge(@PathVariable Integer id) {
        return hospitalService.discharge(id);
    }

    /** 住院列表分页（后台/医生） */
    @GetMapping("/page")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.DOCTOR})
    public IPage<HospitalDto> getPage(int numPage, int pageSize, Boolean status) {
        return hospitalService.selectPage(status, numPage, pageSize);
    }

    /** 某病历的住院记录（含明细）：顾客只能查自己宠物 */
    @GetMapping("/record/{recordId}")
    public List<HospitalDto> selectByRecord(@PathVariable Integer recordId) {
        if (UserUtil.getUser() instanceof Client client) {
            MedicalRecord record = recordService.getById(recordId);
            if (record == null || !client.getClientId().equals(record.getClientId())) return null;
        }
        HospitalDto dto = hospitalService.lambdaQuery()
                .eq(com.mix.model.entity.HospitalRecord::getRecordId, recordId)
                .orderByDesc(com.mix.model.entity.HospitalRecord::getCreateTime)
                .list().stream()
                .map(h -> {
                    HospitalDto x = new HospitalDto();
                    x.setHospitalId(h.getHospitalId());
                    x.setRecordId(h.getRecordId());
                    x.setPetId(h.getPetId());
                    x.setClientId(h.getClientId());
                    x.setDoctorId(h.getDoctorId());
                    x.setRoomNo(h.getRoomNo());
                    x.setInTime(h.getInTime());
                    x.setOutTime(h.getOutTime());
                    x.setDailyFee(h.getDailyFee());
                    x.setDays(h.getDays());
                    x.setStatus(h.getStatus());
                    x.setNote(h.getNote());
                    x.setCreateTime(h.getCreateTime());
                    return x;
                }).findFirst().orElse(null);
        return dto == null ? java.util.List.of() : java.util.List.of(dto);
    }
}