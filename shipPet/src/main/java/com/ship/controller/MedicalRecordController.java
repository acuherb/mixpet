package com.ship.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ship.common.Result;
import com.ship.model.dto.RecordDto;
import com.ship.model.vo.PetHistoryVo;
import com.ship.model.vo.RecordFullVo;
import com.ship.model.vo.RecordQueryVo;
import com.ship.model.vo.RecordTodoVo;
import com.ship.model.entity.Client;
import com.ship.model.entity.MedicalRecord;
import com.ship.model.entity.Doctor;
import com.ship.model.entity.Pet;
import com.ship.model.entity.Prescription;
import com.ship.model.entity.RecordPackage;
import com.ship.mapper.PrescriptionMapper;
import com.ship.mapper.RecordPackageMapper;
import com.ship.model.vo.RecordVo;
import com.ship.model.vo.RecordPackageVo;
import com.ship.model.vo.RecordPrescriptionVo;
import com.ship.model.vo.RxSheetVo;
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
    private final RecordPackageMapper recordPackageMapper;

    @Autowired
    public MedicalRecordController(IMedicalRecordService recordService,
                                   PrescriptionMapper prescriptionMapper,
                                   IPetService petService,
                                   RecordPackageMapper recordPackageMapper) {
        this.recordService = recordService;
        this.prescriptionMapper = prescriptionMapper;
        this.petService = petService;
        this.recordPackageMapper = recordPackageMapper;
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
     * 完结就诊：有未完成事项（待出结果/待手术/待随访）时返回清单，
     * 前端确认后带 force=true 再次提交即强制完结
     */
    @PutMapping("/finish/{id}")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public Result finish(@PathVariable Integer id, boolean force) {
        return recordService.finishRecord(id, force);
    }

    /** 作废就诊（仅接诊中）：填写作废原因 */
    @PutMapping("/cancel/{id}")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public Result cancel(@PathVariable Integer id, String reason) {
        return recordService.cancelRecord(id, reason);
    }

    /** 未完绪病历修改（仅接诊中）：内容字段可改，费用不走这里 */
    @PutMapping
    @Secured(RoleConst.DOCTOR)
    public Result update(@RequestBody MedicalRecord record) {
        return recordService.updateRecord(record);
    }

    /**
     * 宠物病史速览（接诊侧栏）：档案+过敏/既往史+最近就诊+近期用药+检验异常；员工可查
     */
    @GetMapping("/petHistory/{petId}")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public PetHistoryVo petHistory(@PathVariable Integer petId) {
        return recordService.selectPetHistory(petId);
    }

    /**
     * 病例全量档案（一个接口拿全）：员工可查任意；顾客只能看自己宠物的
     */
    @GetMapping("/detail/{recordId}")
    public RecordFullVo detail(@PathVariable Integer recordId) {
        RecordFullVo full = recordService.selectRecordFull(recordId);
        if (full == null) return null;
        if (UserUtil.getUser() instanceof Client client
                && !client.getClientId().equals(full.record().getClientId())) return null;
        return full;
    }

    /**
     * 病历多条件检索（管理端/医生端病例中心）
     */
    @GetMapping("/page")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public IPage<RecordDto> page(int numPage, int pageSize, RecordQueryVo query) {
        return recordService.selectRecordPage(numPage, pageSize, query);
    }

    /**
     * 诊疗待办统计（工作台/看板）：接诊中/待出结果/待报告/待手术/待随访
     */
    @GetMapping("/todo")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public RecordTodoVo todo() {
        return recordService.selectRecordTodo();
    }

    /** 作废单条处方：冲减未付账单药费 */
    @PutMapping("/prescription/cancel/{id}")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public Result cancelPrescription(@PathVariable Integer id) {
        return recordService.cancelPrescription(id);
    }

    /** 某病历的复诊链（后续复诊记录）：顾客只能看自己宠物的 */
    @GetMapping("/revisits/{recordId}")
    public List<RecordDto> revisits(@PathVariable Integer recordId) {
        com.ship.model.entity.MedicalRecord record = recordService.getById(recordId);
        if (record == null) return java.util.List.of();
        if (UserUtil.getUser() instanceof Client client
                && !client.getClientId().equals(record.getClientId())) return java.util.List.of();
        return recordService.selectRevisits(recordId);
    }

    /** 往已有病历追加处方（医生看诊中陆续加药，未付款账单累加药费） */
    @PostMapping("/prescription")
    @Secured(RoleConst.DOCTOR)
    public Result addPrescription(@RequestBody RecordPrescriptionVo vo) {
        return recordService.addPrescriptions(vo.getRecordId(), vo.getPrescriptions());
    }

    /** 往已有病历追加套餐（未付款账单累加套餐费） */
    @PostMapping("/package")
    @Secured(RoleConst.DOCTOR)
    public Result addPackage(@RequestBody RecordPackageVo vo) {
        return recordService.addPackages(vo.getRecordId(), vo.getPackageIds());
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
        List<RecordPackage> packages = recordPackageMapper.selectList(
                Wrappers.<RecordPackage>lambdaQuery().eq(RecordPackage::getRecordId, recordId).orderByAsc(RecordPackage::getId));
        return new RecordDetailVo(dto, prescriptions, packages);
    }

    /**
     * 按挂号单查已生成病历（接诊后取 recordId；员工可用）
     */
    @GetMapping("/appointment/{appointmentId}")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER})
    public RecordDto selectByAppointment(@PathVariable Integer appointmentId) {
        return recordService.selectByAppointment(appointmentId);
    }

    /**
     * 兽医处方笺（国标）：顾客只能看自己宠物的
     */
    @GetMapping("/rx/{recordId}")
    public RxSheetVo selectRx(@PathVariable Integer recordId) {
        RecordDto dto = recordService.selectRecordOne(recordId);
        if (dto == null) return null;
        if (UserUtil.getUser() instanceof Client client
                && !client.getClientId().equals(dto.getClientId())) return null;
        return recordService.selectRxSheet(recordId);
    }

    /**
     * 病历详情响应：病历 + 处方 + 套餐
     */
    public record RecordDetailVo(RecordDto record, List<Prescription> prescriptions, List<RecordPackage> packages) {
    }
}
