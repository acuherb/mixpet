package com.ship.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ship.common.Result;
import com.ship.model.dto.RecordDto;
import com.ship.model.entity.Client;
import com.ship.model.entity.Doctor;
import com.ship.model.entity.Pet;
import com.ship.model.entity.Prescription;
import com.ship.mapper.PrescriptionMapper;
import com.ship.model.vo.RecordVo;
import com.ship.security.model.RoleConst;
import com.ship.service.IMedicalRecordService;
import com.ship.service.IPetService;
import com.ship.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 电子病历：医生接诊建档；顾客/员工查看就诊史
 */
@RestController
@RequestMapping("/record")
public class MedicalRecordController {
    private final IMedicalRecordService recordService;
    private final PrescriptionMapper prescriptionMapper;
    private final IPetService petService;

    @Autowired
    public MedicalRecordController(IMedicalRecordService recordService,
                                   PrescriptionMapper prescriptionMapper,
                                   IPetService petService) {
        this.recordService = recordService;
        this.prescriptionMapper = prescriptionMapper;
        this.petService = petService;
    }

    /**
     * 医生接诊：保存病历+处方，生成未付账单，挂号单标记完成
     */
    @PostMapping("/save")
    @Secured(RoleConst.DOCTOR)
    public Result save(@RequestBody RecordVo vo) {
        if (UserUtil.getUser() instanceof Doctor doctor) {
            return recordService.saveRecord(vo, doctor);
        } else return Result.error("请登录医生账号");
    }

    /**
     * 宠物的就诊史：顾客只能查自己名下宠物，员工可查任意
     */
    @GetMapping("/pet/{petId}")
    public IPage<RecordDto> selectPetRecords(@PathVariable Integer petId, int numPage, int pageSize) {
        if (UserUtil.getUser() instanceof Client client) {
            Pet pet = petService.getById(petId);
            if (pet == null || !client.getClientId().equals(pet.getClientId())) return null;
        }
        return recordService.selectPetRecords(petId, numPage, pageSize);
    }

    /**
     * 病历详情（含处方）：顾客只能看自己宠物的；内部备注仅员工接口返回（详情 SQL 查 internal_note，顾客端前端不展示）
     */
    @GetMapping("/one/{recordId}")
    public RecordDetailVo selectOne(@PathVariable Integer recordId) {
        RecordDto dto = recordService.selectRecordOne(recordId);
        if (dto == null) return null;
        if (UserUtil.getUser() instanceof Client client
                && !client.getClientId().equals(dto.getClientId())) return null;
        List<Prescription> prescriptions = prescriptionMapper.selectByRecord(recordId);
        return new RecordDetailVo(dto, prescriptions);
    }

    /**
     * 病历详情响应：病历 + 处方
     */
    public record RecordDetailVo(RecordDto record, List<Prescription> prescriptions) {
    }
}
