package com.ship.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.common.Result;
import com.ship.mapper.BillMapper;
import com.ship.mapper.DoctorMapper;
import com.ship.mapper.DrugMapper;
import com.ship.mapper.MedicalRecordMapper;
import com.ship.mapper.PetMapper;
import com.ship.mapper.PrescriptionMapper;
import com.ship.mapper.RecordPackageMapper;
import com.ship.model.dto.RecordDto;
import com.ship.model.entity.*;
import com.ship.model.vo.RecordVo;
import com.ship.model.vo.RxSheetVo;
import com.ship.service.IAppointmentService;
import com.ship.service.IMedicalRecordService;
import com.ship.service.IServicePackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 电子病历 Service 实现
 */
@Service
public class MedicalRecordServiceImpl extends ServiceImpl<MedicalRecordMapper, MedicalRecord> implements IMedicalRecordService {
    private final IAppointmentService appointmentService;
    private final DrugMapper drugMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final BillMapper billMapper;
    private final PetMapper petMapper;
    private final DoctorMapper doctorMapper;
    private final IServicePackageService packageService;
    private final RecordPackageMapper recordPackageMapper;

    @Autowired
    public MedicalRecordServiceImpl(IAppointmentService appointmentService, DrugMapper drugMapper,
                                    PrescriptionMapper prescriptionMapper, BillMapper billMapper,
                                    PetMapper petMapper, DoctorMapper doctorMapper,
                                    IServicePackageService packageService, RecordPackageMapper recordPackageMapper) {
        this.appointmentService = appointmentService;
        this.drugMapper = drugMapper;
        this.prescriptionMapper = prescriptionMapper;
        this.billMapper = billMapper;
        this.petMapper = petMapper;
        this.doctorMapper = doctorMapper;
        this.packageService = packageService;
        this.recordPackageMapper = recordPackageMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result saveRecord(RecordVo vo, Doctor doctor) {
        MedicalRecord record = vo == null ? null : vo.getRecord();
        if (record == null || record.getAppointmentId() == null) return Result.error("缺少挂号单信息");
        // 校验挂号单：存在、归属本人、未接诊过
        Appointment appoint = appointmentService.getById(record.getAppointmentId());
        if (appoint == null) return Result.error("挂号单不存在");
        if (!doctor.getDoctorId().equals(appoint.getDoctorId())) return Result.error("只能接诊自己的挂号单");
        if (this.lambdaQuery().eq(MedicalRecord::getAppointmentId, appoint.getAppointmentId()).count() > 0)
            return Result.error("该挂号单已接诊过，不能重复建档");
        // 归属信息以挂号单为准，不信任前端
        record.setRecordId(null);
        record.setPetId(appoint.getPetId());
        record.setClientId(appoint.getClientId());
        record.setDoctorId(doctor.getDoctorId());
        record.setDepartmentId(appoint.getDepartmentId());
        if (record.getFeeRegister() == null) record.setFeeRegister(BigDecimal.ZERO);
        if (record.getFeeTreat() == null) record.setFeeTreat(BigDecimal.ZERO);
        if (record.getFeeRegister().compareTo(BigDecimal.ZERO) < 0 || record.getFeeTreat().compareTo(BigDecimal.ZERO) < 0)
            return Result.error("费用不能为负数");
        // 处方：校验药品与库存，价格等以药品库为准做快照
        BigDecimal feeDrug = BigDecimal.ZERO;
        List<Prescription> prescriptions = new ArrayList<>();
        if (vo.getPrescriptions() != null) {
            for (Prescription p : vo.getPrescriptions()) {
                if (p == null || p.getDrugId() == null || p.getQuantity() == null || p.getQuantity() <= 0) continue;
                Drug drug = drugMapper.selectById(p.getDrugId());
                if (drug == null || Boolean.FALSE.equals(drug.getActive()))
                    return Result.error("药品不存在或已停用");
                if (drug.getDrugStock() == null || drug.getDrugStock() < p.getQuantity())
                    return Result.error("「" + drug.getDrugName() + "」库存不足（剩余 " + drug.getDrugStock() + "）");
                p.setPrescriptionId(null);
                p.setRecordId(null);
                p.setDrugName(drug.getDrugName());
                p.setDrugSpec(drug.getDrugSpec());
                p.setDrugUnit(drug.getDrugUnit());
                p.setDrugPrice(drug.getDrugPrice());
                feeDrug = feeDrug.add(drug.getDrugPrice().multiply(BigDecimal.valueOf(p.getQuantity())));
                prescriptions.add(p);
            }
        }
        this.save(record);
        for (Prescription p : prescriptions) {
            p.setRecordId(record.getRecordId());
            prescriptionMapper.insert(p);
        }
        // 套餐：校验 + 快照 + 记录 + 计费
        BigDecimal feePackage = BigDecimal.ZERO;
        if (vo.getPackages() != null) {
            for (ServicePackage pkg : vo.getPackages()) {
                if (pkg == null || pkg.getPackageId() == null) continue;
                ServicePackage real = packageService.getById(pkg.getPackageId());
                if (real == null || !Boolean.TRUE.equals(real.getActive()))
                    return Result.error("套餐不存在或已停用");
                RecordPackage rp = new RecordPackage();
                rp.setRecordId(record.getRecordId());
                rp.setPackageId(real.getPackageId());
                rp.setPackageName(real.getPackageName());
                rp.setPrice(real.getPrice());
                recordPackageMapper.insert(rp);
                if (real.getPrice() != null) feePackage = feePackage.add(real.getPrice());
            }
        }
        // 生成未付账单
        Bill bill = new Bill();
        bill.setClientId(record.getClientId());
        bill.setPetId(record.getPetId());
        bill.setRecordId(record.getRecordId());
        bill.setFeeRegister(record.getFeeRegister());
        bill.setFeeTreat(record.getFeeTreat());
        bill.setFeeDrug(feeDrug);
        bill.setFeePackage(feePackage);
        bill.setTotalMoney(record.getFeeRegister().add(record.getFeeTreat()).add(feeDrug).add(feePackage));
        bill.setPayStatus(false);
        bill.setPayType(false);
        billMapper.insert(bill);
        // 挂号单标记完成
        appointmentService.lambdaUpdate()
                .eq(Appointment::getAppointmentId, appoint.getAppointmentId())
                .set(Appointment::getIsSuc, true)
                .update();
        return Result.success("接诊完成，已生成收费账单");
    }

    @Override
    public IPage<RecordDto> selectPetRecords(Integer petId, int numPage, int pageSize) {
        return baseMapper.selectPetRecords(new Page<>(numPage, pageSize), petId);
    }

    @Override
    public RecordDto selectRecordOne(Integer recordId) {
        return baseMapper.selectRecordOne(recordId);
    }

    @Override
    public RecordDto selectByAppointment(Integer appointmentId) {
        MedicalRecord r = this.lambdaQuery().eq(MedicalRecord::getAppointmentId, appointmentId).one();
        return r == null ? null : baseMapper.selectRecordOne(r.getRecordId());
    }

    @Override
    public RxSheetVo selectRxSheet(Integer recordId) {
        RecordDto record = baseMapper.selectRecordOne(recordId);
        if (record == null) return null;
        RxSheetVo vo = new RxSheetVo();
        vo.setRecordId(record.getRecordId());
        vo.setRxNo("No." + record.getRecordId());
        vo.setCreateTime(record.getCreateTime());
        vo.setClientName(record.getClientName());
        vo.setPetName(record.getPetName());
        vo.setDiagnosis(record.getDiagnosis());
        vo.setDoctorName(record.getDoctorName());
        vo.setDepartmentName(record.getDepartmentName());
        // 宠物信息（品种/性别/生日/体重/芯片）
        Pet pet = petMapper.selectById(record.getPetId());
        if (pet != null) {
            vo.setPetVariety(pet.getPetVariety());
            vo.setPetSex(pet.getPetSex());
            vo.setPetBirthday(pet.getPetBirthday());
            vo.setPetWeight(pet.getPetWeight());
            vo.setPetChip(pet.getPetChip());
        }
        // 开方兽医执业资格证号
        Doctor doctor = doctorMapper.selectById(record.getDoctorId());
        if (doctor != null) vo.setDoctorCert(doctor.getDoctorCert());
        // 处方明细与药费合计
        List<Prescription> items = prescriptionMapper.selectByRecord(recordId);
        vo.setItems(items == null ? new ArrayList<>() : items);
        BigDecimal total = BigDecimal.ZERO;
        if (items != null) {
            for (Prescription p : items) {
                if (p.getDrugPrice() != null && p.getQuantity() != null) {
                    total = total.add(p.getDrugPrice().multiply(BigDecimal.valueOf(p.getQuantity())));
                }
            }
        }
        vo.setTotalAmount(total);
        return vo;
    }
}
