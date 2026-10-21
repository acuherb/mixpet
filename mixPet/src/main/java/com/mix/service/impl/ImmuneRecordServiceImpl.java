package com.mix.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mix.common.Result;
import com.mix.mapper.BillMapper;
import com.mix.mapper.DrugMapper;
import com.mix.mapper.ImmuneRecordMapper;
import com.mix.model.dto.ImmuneRecordDto;
import com.mix.model.entity.Bill;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.Drug;
import com.mix.model.entity.ImmuneRecord;
import com.mix.model.entity.MedicalRecord;
import com.mix.model.vo.ImmuneVo;
import com.mix.service.IImmuneRecordService;
import com.mix.service.IDrugService;
import com.mix.service.IMedicalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 接种档案 Service 实现
 */
@Service
public class ImmuneRecordServiceImpl extends ServiceImpl<ImmuneRecordMapper, ImmuneRecord> implements IImmuneRecordService {
    private final IMedicalRecordService recordService;
    private final DrugMapper drugMapper;
    private final BillMapper billMapper;
    private final IDrugService drugService;

    @Autowired
    public ImmuneRecordServiceImpl(IMedicalRecordService recordService, DrugMapper drugMapper,
                                   BillMapper billMapper, IDrugService drugService) {
        this.recordService = recordService;
        this.drugMapper = drugMapper;
        this.billMapper = billMapper;
        this.drugService = drugService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result saveImmune(ImmuneVo vo, Doctor doctor) {
        if (vo == null || vo.getDrugId() == null) return Result.error("请选择疫苗");
        if (vo.getPetId() == null && vo.getRecordId() == null) return Result.error("缺少宠物信息");
        Drug drug = drugMapper.selectById(vo.getDrugId());
        if (drug == null) return Result.error("疫苗不存在");
        boolean deleted = drug.getIsDel() != null && drug.getIsDel() != 0L;
        if (!"疫苗".equals(drug.getDrugType()) || !Boolean.TRUE.equals(drug.getActive()) || deleted)
            return Result.error("疫苗不存在或已停用");

        Integer petId = vo.getPetId();
        Integer clientId = null;
        if (vo.getRecordId() != null) {
            MedicalRecord record = recordService.getById(vo.getRecordId());
            if (record != null) {
                petId = record.getPetId();
                clientId = record.getClientId();
            }
        }
        if (petId == null) return Result.error("无法确定宠物");

        // 计算此针为第几针
        Long cnt = this.lambdaQuery().eq(ImmuneRecord::getPetId, petId)
                .eq(ImmuneRecord::getDrugId, vo.getDrugId()).count();
        int dose = (cnt == null ? 0 : cnt.intValue()) + 1;

        LocalDateTime vaccinate = vo.getVaccinateTime() == null ? LocalDateTime.now() : vo.getVaccinateTime();
        LocalDateTime next = null;
        if (drug.getIntervalDays() != null && drug.getIntervalDays() > 0) {
            next = vaccinate.plusDays(drug.getIntervalDays());
        }

        // 疫苗即时出库（接种即消耗）：原子扣减，库存 0 拒绝登记（防打了没药可记）
        int stock = drug.getDrugStock() == null ? 0 : drug.getDrugStock();
        int affected = drugMapper.update(null, Wrappers.<Drug>lambdaUpdate()
                .eq(Drug::getDrugId, drug.getDrugId())
                .ge(Drug::getDrugStock, 1)
                .setSql("drug_stock = drug_stock - 1"));
        if (affected == 0) return Result.error("「" + drug.getDrugName() + "」库存不足（剩余 " + stock + "），无法登记接种");
        drugService.logStockChange(drug.getDrugId(), drug.getDrugName(), 2, 1,
                stock, stock - 1, "免疫接种出库");

        // 计费：疫苗按药品定价计入该病历账单（未付账单累加/无则新建，模式同检验开单）
        BigDecimal fee = drug.getDrugPrice() == null ? BigDecimal.ZERO : drug.getDrugPrice();
        if (vo.getRecordId() != null && fee.compareTo(BigDecimal.ZERO) > 0) {
            Bill unpaid = billMapper.selectOne(Wrappers.<Bill>lambdaQuery()
                    .eq(Bill::getRecordId, vo.getRecordId())
                    .eq(Bill::getPayStatus, false)
                    .last("limit 1"));
            if (unpaid != null) {
                billMapper.update(null, Wrappers.<Bill>lambdaUpdate()
                        .eq(Bill::getBillId, unpaid.getBillId())
                        .setSql("fee_immune = fee_immune + " + fee.toPlainString())
                        .setSql("total_money = total_money + " + fee.toPlainString()));
            } else {
                Bill bill = new Bill();
                bill.setClientId(clientId);
                bill.setPetId(petId);
                bill.setRecordId(vo.getRecordId());
                bill.setFeeRegister(BigDecimal.ZERO);
                bill.setFeeTreat(BigDecimal.ZERO);
                bill.setFeeDrug(BigDecimal.ZERO);
                bill.setFeeLab(BigDecimal.ZERO);
                bill.setFeeHospital(BigDecimal.ZERO);
                bill.setFeePackage(BigDecimal.ZERO);
                bill.setFeeSurgery(BigDecimal.ZERO);
                bill.setFeeImmune(fee);
                bill.setTotalMoney(fee);
                bill.setPayStatus(false);
                bill.setPayType(false);
                billMapper.insert(bill);
            }
        }

        ImmuneRecord record = new ImmuneRecord();
        record.setRecordId(vo.getRecordId());
        record.setPetId(petId);
        record.setClientId(clientId);
        record.setDoctorId(doctor == null ? null : doctor.getDoctorId());
        record.setDrugId(drug.getDrugId());
        record.setDrugName(drug.getDrugName());
        record.setDoseCnt(dose);
        record.setVaccinateTime(vaccinate);
        record.setNextTime(next);
        record.setNote(vo.getNote());
        boolean ok = this.save(record);
        if (ok) {
            return Result.success("接种登记成功，疫苗费 ￥" + fee.toPlainString()
                    + (vo.getRecordId() != null ? " 已计入病历 #" + vo.getRecordId() + " 账单" : ""));
        }
        return Result.error("登记失败");
    }

    @Override
    public IPage<ImmuneRecordDto> selectPage(String petName, Boolean dueSoon, int numPage, int pageSize) {
        return baseMapper.selectPage(new Page<>(numPage, pageSize), petName, dueSoon);
    }

    @Override
    public List<ImmuneRecordDto> selectByPet(Integer petId) {
        return baseMapper.selectByPet(petId);
    }
}