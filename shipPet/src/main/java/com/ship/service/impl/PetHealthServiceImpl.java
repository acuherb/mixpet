package com.ship.service.impl;

import com.ship.mapper.ImmuneRecordMapper;
import com.ship.mapper.PetMapper;
import com.ship.model.dto.RecordDto;
import com.ship.model.entity.HospitalRecord;
import com.ship.model.entity.ImmuneRecord;
import com.ship.model.entity.MedicalRecord;
import com.ship.model.entity.Pet;
import com.ship.model.vo.PetHealth;
import com.ship.service.IHospitalService;
import com.ship.service.IMedicalRecordService;
import com.ship.service.IPetHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * 一页式宠物健康档案 Service 实现
 */
@Service
public class PetHealthServiceImpl implements IPetHealthService {
    private final PetMapper petMapper;
    private final IMedicalRecordService recordService;
    private final ImmuneRecordMapper immuneMapper;
    private final IHospitalService hospitalService;

    @Autowired
    public PetHealthServiceImpl(PetMapper petMapper, IMedicalRecordService recordService,
                                ImmuneRecordMapper immuneMapper, IHospitalService hospitalService) {
        this.petMapper = petMapper;
        this.recordService = recordService;
        this.immuneMapper = immuneMapper;
        this.hospitalService = hospitalService;
    }

    @Override
    public PetHealth get(Integer petId) {
        Pet pet = petMapper.selectById(petId);
        if (pet == null) return null;
        PetHealth h = new PetHealth();
        h.setPet(pet);

        // 就诊史（近50条）
        List<RecordDto> visits = recordService.selectPetRecords(petId, 1, 50).getRecords();
        h.setVisits(visits);
        h.setVisitCount(visits.size());
        if (!visits.isEmpty()) {
            h.setLastVisitDate(visits.get(0).getCreateTime() == null ? null
                    : visits.get(0).getCreateTime().toLocalDate().toString());
            h.setLastDiagnosis(visits.get(0).getDiagnosis());
        }

        // 体重曲线（有记录体重的就诊，按时间正序）
        List<MedicalRecord> recs = recordService.lambdaQuery()
                .eq(MedicalRecord::getPetId, petId)
                .isNotNull(MedicalRecord::getWeight)
                .orderByAsc(MedicalRecord::getCreateTime)
                .list();
        h.setWeightTrend(recs.stream()
                .filter(r -> r.getCreateTime() != null && r.getWeight() != null)
                .map(r -> new PetHealth.WeightPoint(r.getCreateTime().toLocalDate(), r.getWeight()))
                .toList());

        // 免疫记录
        h.setImmune(immuneMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ImmuneRecord>()
                .eq(ImmuneRecord::getPetId, petId)
                .orderByDesc(ImmuneRecord::getCreateTime)));

        // 下次免疫（未来最近的 nextTime）
        h.setNextVaccineDate(immuneMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ImmuneRecord>()
                        .eq(ImmuneRecord::getPetId, petId)
                        .isNotNull(ImmuneRecord::getNextTime)).stream()
                .map(ImmuneRecord::getNextTime)
                .filter(t -> t != null && !t.toLocalDate().isBefore(LocalDate.now()))
                .min(Comparator.naturalOrder())
                .map(t -> t.toLocalDate().toString())
                .orElse(null));

        // 住院记录
        h.setHospital(hospitalService.lambdaQuery()
                .eq(HospitalRecord::getPetId, petId)
                .orderByDesc(HospitalRecord::getCreateTime)
                .list());
        return h;
    }
}
