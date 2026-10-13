package com.ship.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.common.Result;
import com.ship.mapper.DrugMapper;
import com.ship.mapper.ImmuneRecordMapper;
import com.ship.model.dto.ImmuneRecordDto;
import com.ship.model.entity.Doctor;
import com.ship.model.entity.Drug;
import com.ship.model.entity.ImmuneRecord;
import com.ship.model.entity.MedicalRecord;
import com.ship.model.vo.ImmuneVo;
import com.ship.service.IImmuneRecordService;
import com.ship.service.IMedicalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 接种档案 Service 实现
 */
@Service
public class ImmuneRecordServiceImpl extends ServiceImpl<ImmuneRecordMapper, ImmuneRecord> implements IImmuneRecordService {
    private final IMedicalRecordService recordService;
    private final DrugMapper drugMapper;

    @Autowired
    public ImmuneRecordServiceImpl(IMedicalRecordService recordService, DrugMapper drugMapper) {
        this.recordService = recordService;
        this.drugMapper = drugMapper;
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
        return Result.choice("接种登记", this.save(record));
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