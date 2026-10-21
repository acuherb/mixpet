package com.mix.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.common.Result;
import com.mix.mapper.ClientMapper;
import com.mix.mapper.DoctorMapper;
import com.mix.mapper.MedicalRecordMapper;
import com.mix.mapper.PetMapper;
import com.mix.mapper.SurgeryConsentMapper;
import com.mix.mapper.SurgeryMapper;
import com.mix.model.entity.Client;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.MedicalRecord;
import com.mix.model.entity.Pet;
import com.mix.model.entity.Surgery;
import com.mix.model.entity.SurgeryConsent;
import com.mix.model.vo.ConsentSheetVo;
import com.mix.model.vo.SurgeryConsentVo;
import com.mix.service.ISurgeryConsentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 手术知情同意书 Service 实现
 */
@Service
public class SurgeryConsentServiceImpl extends ServiceImpl<SurgeryConsentMapper, SurgeryConsent> implements ISurgeryConsentService {
    private final SurgeryMapper surgeryMapper;
    private final PetMapper petMapper;
    private final ClientMapper clientMapper;
    private final DoctorMapper doctorMapper;
    private final MedicalRecordMapper recordMapper;

    @Autowired
    public SurgeryConsentServiceImpl(SurgeryMapper surgeryMapper, PetMapper petMapper,
                                     ClientMapper clientMapper, DoctorMapper doctorMapper,
                                     MedicalRecordMapper recordMapper) {
        this.surgeryMapper = surgeryMapper;
        this.petMapper = petMapper;
        this.clientMapper = clientMapper;
        this.doctorMapper = doctorMapper;
        this.recordMapper = recordMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result sign(SurgeryConsentVo vo, String operator) {
        if (vo == null || vo.getSurgeryId() == null) return Result.error("缺少手术单id");
        Surgery s = surgeryMapper.selectById(vo.getSurgeryId());
        if (s == null) return Result.error("手术单不存在");
        if (s.getStatus() != null && s.getStatus() == 3) return Result.error("手术已取消，无需签同意书");
        if (!Boolean.TRUE.equals(vo.getAgree())) return Result.error("需畜主同意并签字");
        if (vo.getSigner() == null || vo.getSigner().isBlank()) return Result.error("请填写签字人（畜主）姓名");
        SurgeryConsent exist = this.lambdaQuery().eq(SurgeryConsent::getSurgeryId, vo.getSurgeryId()).one();
        SurgeryConsent c = exist != null ? exist : new SurgeryConsent();
        c.setSurgeryId(s.getSurgeryId());
        c.setRecordId(s.getRecordId());
        c.setPetId(s.getPetId());
        c.setClientId(s.getClientId());
        c.setSurgeryName(s.getSurgeryName());
        c.setAnesthesiaType(s.getAnesthesiaType());
        c.setRisks(vo.getRisks());
        c.setAgree(true);
        c.setSigner(vo.getSigner());
        c.setSignedTime(LocalDateTime.now());
        c.setOperator(operator);
        c.setNote(vo.getNote());
        if (exist != null) this.updateById(c);
        else this.save(c);
        return Result.success("知情同意书已签署");
    }

    @Override
    public SurgeryConsent getBySurgery(Integer surgeryId) {
        return this.lambdaQuery().eq(SurgeryConsent::getSurgeryId, surgeryId).one();
    }

    @Override
    public ConsentSheetVo consentSheet(Integer surgeryId) {
        Surgery s = surgeryMapper.selectById(surgeryId);
        if (s == null) return null;
        ConsentSheetVo vo = new ConsentSheetVo();
        vo.setSurgeryId(s.getSurgeryId());
        vo.setConsentNo("No." + s.getSurgeryId());
        vo.setSurgeryName(s.getSurgeryName());
        vo.setSurgeryType(s.getSurgeryType());
        vo.setAnesthesiaType(s.getAnesthesiaType());
        vo.setScheduledTime(s.getScheduledTime());
        Pet pet = petMapper.selectById(s.getPetId());
        if (pet != null) {
            vo.setPetName(pet.getPetName());
            vo.setPetVariety(pet.getPetVariety());
            vo.setPetSex(pet.getPetSex());
            vo.setPetBirthday(pet.getPetBirthday());
            vo.setPetWeight(pet.getPetWeight());
            vo.setPetChip(pet.getPetChip());
        }
        Client client = clientMapper.selectById(s.getClientId());
        if (client != null) vo.setClientName(client.getClientName());
        Doctor doctor = doctorMapper.selectById(s.getDoctorId());
        if (doctor != null) vo.setDoctorName(doctor.getDoctorName());
        MedicalRecord record = recordMapper.selectById(s.getRecordId());
        if (record != null) {
            vo.setDiagnosis(record.getDiagnosis());
            vo.setCreateTime(record.getCreateTime());
        }
        SurgeryConsent c = getBySurgery(surgeryId);
        if (c != null) {
            vo.setRisks(c.getRisks());
            vo.setAgree(c.getAgree());
            vo.setSigner(c.getSigner());
            vo.setSignedTime(c.getSignedTime());
            vo.setOperator(c.getOperator());
        }
        return vo;
    }
}
