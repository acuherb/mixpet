package com.ship.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.common.Result;
import com.ship.mapper.ImmuneRecordMapper;
import com.ship.mapper.VaccineMapper;
import com.ship.model.dto.ImmuneRecordDto;
import com.ship.model.entity.Doctor;
import com.ship.model.entity.ImmuneRecord;
import com.ship.model.entity.MedicalRecord;
import com.ship.model.entity.Vaccine;
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
    private final VaccineMapper vaccineMapper;

    @Autowired
    public ImmuneRecordServiceImpl(IMedicalRecordService recordService, VaccineMapper vaccineMapper) {
        this.recordService = recordService;
        this.vaccineMapper = vaccineMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result saveImmune(ImmuneVo vo, Doctor doctor) {
        if (vo == null || vo.getVaccineId() == null) return Result.error("请选择疫苗");
        if (vo.getPetId() == null && vo.getRecordId() == null) return Result.error("缺少宠物信息");
        Vaccine vaccine = vaccineMapper.selectById(vo.getVaccineId());
        if (vaccine == null || vaccine.getIsDel() != null && vaccine.getIsDel() == 1) return Result.error("疫苗不存在或已停用");

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
                .eq(ImmuneRecord::getVaccineId, vo.getVaccineId()).count();
        int dose = (cnt == null ? 0 : cnt.intValue()) + 1;

        LocalDateTime vaccinate = vo.getVaccinateTime() == null ? LocalDateTime.now() : vo.getVaccinateTime();
        LocalDateTime next = null;
        if (vaccine.getIntervalDays() != null && vaccine.getIntervalDays() > 0) {
            next = vaccinate.plusDays(vaccine.getIntervalDays());
        }

        ImmuneRecord record = new ImmuneRecord();
        record.setRecordId(vo.getRecordId());
        record.setPetId(petId);
        record.setClientId(clientId);
        record.setDoctorId(doctor == null ? null : doctor.getDoctorId());
        record.setVaccineId(vaccine.getVaccineId());
        record.setVaccineName(vaccine.getVaccineName());
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