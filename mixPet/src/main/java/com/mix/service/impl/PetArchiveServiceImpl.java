package com.mix.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mix.mapper.*;
import com.mix.model.dto.RecordDto;
import com.mix.model.entity.*;
import com.mix.model.vo.PetArchive;
import com.mix.service.IMedicalRecordService;
import com.mix.service.IPetArchiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 宠物就诊档案（全量聚合）
 * <p>
 * 一次性把一只宠物的诊疗事件全查出来，避免前端逐条病历再请求处方/化验/影像（N+1）。
 * isDel 由全局逻辑删除配置自动过滤，不用手写条件。
 */
@Service
public class PetArchiveServiceImpl implements IPetArchiveService {

    /** 单宠物最大就诊数实测 437，取 1000 兜底 */
    private static final int MAX_VISITS = 1000;

    private final PetMapper petMapper;
    private final ClientMapper clientMapper;
    private final IMedicalRecordService recordService;
    private final PrescriptionMapper prescriptionMapper;
    private final ImmuneRecordMapper immuneMapper;
    private final HospitalMapper hospitalMapper;
    private final SurgeryMapper surgeryMapper;
    private final LabOrderMapper labOrderMapper;
    private final ImageOrderMapper imageOrderMapper;

    @Autowired
    public PetArchiveServiceImpl(PetMapper petMapper, ClientMapper clientMapper,
                                 IMedicalRecordService recordService, PrescriptionMapper prescriptionMapper,
                                 ImmuneRecordMapper immuneMapper, HospitalMapper hospitalMapper,
                                 SurgeryMapper surgeryMapper, LabOrderMapper labOrderMapper,
                                 ImageOrderMapper imageOrderMapper) {
        this.petMapper = petMapper;
        this.clientMapper = clientMapper;
        this.recordService = recordService;
        this.prescriptionMapper = prescriptionMapper;
        this.immuneMapper = immuneMapper;
        this.hospitalMapper = hospitalMapper;
        this.surgeryMapper = surgeryMapper;
        this.labOrderMapper = labOrderMapper;
        this.imageOrderMapper = imageOrderMapper;
    }

    @Override
    public PetArchive get(Integer petId) {
        Pet pet = petMapper.selectById(petId);
        if (pet == null) return null;

        PetArchive a = new PetArchive();
        a.setPet(pet);
        a.setClient(pet.getClientId() == null ? null : clientMapper.selectById(pet.getClientId()));

        // 1) 全部病历
        List<RecordDto> visits = recordService.selectPetRecords(petId, 1, MAX_VISITS).getRecords();
        a.setVisits(visits);

        // 2) 处方：一次 in 查询取全部，按 recordId 分组
        List<Integer> recordIds = visits.stream().map(RecordDto::getRecordId).toList();
        if (!recordIds.isEmpty()) {
            List<Prescription> pris = prescriptionMapper.selectList(
                    new LambdaQueryWrapper<Prescription>()
                            .in(Prescription::getRecordId, recordIds)
                            .orderByAsc(Prescription::getPrescriptionId));
            Map<Integer, List<Prescription>> map = pris.stream()
                    .collect(Collectors.groupingBy(Prescription::getRecordId));
            a.setPrescriptions(map);
        } else {
            a.setPrescriptions(Map.of());
        }

        // 3) 免疫 / 住院 / 手术 / 化验 / 影像
        a.setImmune(immuneMapper.selectList(new LambdaQueryWrapper<ImmuneRecord>()
                .eq(ImmuneRecord::getPetId, petId)
                .orderByDesc(ImmuneRecord::getVaccinateTime)));
        a.setHospital(hospitalMapper.selectList(new LambdaQueryWrapper<HospitalRecord>()
                .eq(HospitalRecord::getPetId, petId)
                .orderByDesc(HospitalRecord::getInTime)));
        a.setSurgery(surgeryMapper.selectList(new LambdaQueryWrapper<Surgery>()
                .eq(Surgery::getPetId, petId)
                .orderByDesc(Surgery::getCreateTime)));
        a.setLab(labOrderMapper.selectList(new LambdaQueryWrapper<LabOrder>()
                .eq(LabOrder::getPetId, petId)
                .orderByDesc(LabOrder::getCreateTime)));
        a.setImage(imageOrderMapper.selectList(new LambdaQueryWrapper<ImageOrder>()
                .eq(ImageOrder::getPetId, petId)
                .orderByDesc(ImageOrder::getCreateTime)));

        // 4) 汇总指标
        PetArchive.Stat stat = new PetArchive.Stat();
        stat.setVisitCount(visits.size());
        LocalDateTime first = visits.stream().map(RecordDto::getCreateTime)
                .filter(java.util.Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
        LocalDateTime last = visits.stream().map(RecordDto::getCreateTime)
                .filter(java.util.Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
        stat.setFirstVisit(first == null ? null : first.toLocalDate().toString());
        stat.setLastVisit(last == null ? null : last.toLocalDate().toString());
        stat.setLastDiagnosis(visits.isEmpty() ? null : visits.get(0).getDiagnosis());
        stat.setWeight(visits.stream().map(RecordDto::getWeight)
                .filter(java.util.Objects::nonNull).findFirst().orElse(pet.getPetWeight()));
        stat.setNextImmune(a.getImmune().stream().map(ImmuneRecord::getNextTime)
                .filter(t -> t != null && !t.toLocalDate().isBefore(LocalDate.now()))
                .min(Comparator.naturalOrder())
                .map(t -> t.toLocalDate().toString()).orElse(null));
        a.setStat(stat);
        return a;
    }
}
