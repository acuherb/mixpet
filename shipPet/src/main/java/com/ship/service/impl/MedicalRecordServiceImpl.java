package com.ship.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.common.Result;
import com.ship.mapper.BillMapper;
import com.ship.mapper.DrugMapper;
import com.ship.mapper.MedicalRecordMapper;
import com.ship.mapper.PrescriptionMapper;
import com.ship.model.dto.RecordDto;
import com.ship.model.entity.*;
import com.ship.model.vo.RecordVo;
import com.ship.service.IAppointmentService;
import com.ship.service.IMedicalRecordService;
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

    @Autowired
    public MedicalRecordServiceImpl(IAppointmentService appointmentService, DrugMapper drugMapper,
                                    PrescriptionMapper prescriptionMapper, BillMapper billMapper) {
        this.appointmentService = appointmentService;
        this.drugMapper = drugMapper;
        this.prescriptionMapper = prescriptionMapper;
        this.billMapper = billMapper;
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
        // 生成未付账单
        Bill bill = new Bill();
        bill.setClientId(record.getClientId());
        bill.setPetId(record.getPetId());
        bill.setRecordId(record.getRecordId());
        bill.setFeeRegister(record.getFeeRegister());
        bill.setFeeTreat(record.getFeeTreat());
        bill.setFeeDrug(feeDrug);
        bill.setTotalMoney(record.getFeeRegister().add(record.getFeeTreat()).add(feeDrug));
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
}
