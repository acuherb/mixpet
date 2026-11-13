package com.mix.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mix.common.BizException;
import com.mix.mapper.ClientMapper;
import com.mix.mapper.DoctorMapper;
import com.mix.mapper.PetMapper;
import com.mix.mapper.PrescriptionMapper;
import com.mix.mapper.QuarantineCertMapper;
import com.mix.model.dto.RecordDto;
import com.mix.model.entity.Client;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.Pet;
import com.mix.model.entity.Prescription;
import com.mix.model.entity.QuarantineCert;
import com.mix.model.vo.QuarantineCertVo;
import com.mix.model.vo.QuarantineItemVo;
import com.mix.service.IMedicalRecordService;
import com.mix.service.IQuarantineCertService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 检疫单 Service 实现。
 * 数据来源：病历 + 宠物 + 客户 + 医生 + 处方明细，开具时全部快照落库，
 * 后续宠物资料/药品改价不影响历史单据（补打所见即当时所开）。
 */
@Slf4j
@Service
public class QuarantineCertServiceImpl extends ServiceImpl<QuarantineCertMapper, QuarantineCert>
        implements IQuarantineCertService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Resource
    private IMedicalRecordService recordService;
    @Resource
    private PetMapper petMapper;
    @Resource
    private ClientMapper clientMapper;
    @Resource
    private DoctorMapper doctorMapper;
    @Resource
    private PrescriptionMapper prescriptionMapper;

    @Override
    public QuarantineCertVo draft(Integer recordId) {
        if (recordId == null) throw new BizException("缺少病历号");
        RecordDto record = recordService.selectRecordOne(recordId);
        if (record == null) throw new BizException("病历不存在");

        // 已有单据：直接返回最近一张（前端提示"补打"而非"新开"）
        QuarantineCert exist = lambdaQuery()
                .eq(QuarantineCert::getRecordId, recordId)
                .eq(QuarantineCert::getIsDel, 0L)
                .orderByDesc(QuarantineCert::getCertId)
                .last("limit 1").one();
        if (exist != null) return toVo(exist);

        QuarantineCertVo vo = new QuarantineCertVo();
        vo.setRecordId(recordId);
        vo.setPetId(record.getPetId());
        vo.setClientId(record.getClientId());
        vo.setDoctorId(record.getDoctorId());
        vo.setExisted(false);

        Pet pet = record.getPetId() == null ? null : petMapper.selectById(record.getPetId());
        if (pet != null) {
            vo.setPetName(pet.getPetName());
            vo.setBreed(pet.getPetVariety());
            vo.setSpecies(inferSpecies(pet.getPetVariety(), pet.getPetInfo()));
            vo.setSex(pet.getPetSex() == null ? null : (pet.getPetSex() ? "公" : "母"));
            vo.setWeight(pet.getPetWeight());
            vo.setAgeDesc(ageDesc(pet.getPetBirthday()));
        }
        Client client = record.getClientId() == null ? null : clientMapper.selectById(record.getClientId());
        if (client != null) {
            vo.setOwnerName(client.getClientName());
            vo.setOwnerTel(client.getClientTel());
        }
        Doctor doctor = record.getDoctorId() == null ? null : doctorMapper.selectById(record.getDoctorId());
        if (doctor != null) vo.setDoctorName(doctor.getDoctorName());

        // 检疫清单：未作废处方明细快照
        vo.setItems(toItems(prescriptionMapper.selectByRecord(recordId)));
        return vo;
    }

    @Override
    public QuarantineCertVo issue(QuarantineCertVo vo) {
        if (vo == null || vo.getRecordId() == null) throw new BizException("缺少病历号");
        // 以草稿为底（含宠物/客户/医生快照与处方清单），前端只补结论/清单覆盖项
        QuarantineCertVo base = draft(vo.getRecordId());
        if (Boolean.TRUE.equals(base.getExisted())) {
            // 已有单据不可重复开具（补打走 selectByNo），避免一病历多单
            throw new BizException("该病历已开具检疫单 " + base.getCertNo() + "，如需重新开具请作废后重开");
        }
        if (vo.getContent() != null) base.setContent(vo.getContent());
        if (vo.getItems() != null) base.setItems(vo.getItems());
        if (vo.getPetName() != null) base.setPetName(vo.getPetName());
        if (vo.getSpecies() != null) base.setSpecies(vo.getSpecies());
        if (vo.getBreed() != null) base.setBreed(vo.getBreed());
        if (vo.getSex() != null) base.setSex(vo.getSex());
        if (vo.getAgeDesc() != null) base.setAgeDesc(vo.getAgeDesc());
        if (vo.getWeight() != null) base.setWeight(vo.getWeight());
        if (vo.getOwnerName() != null) base.setOwnerName(vo.getOwnerName());
        if (vo.getOwnerTel() != null) base.setOwnerTel(vo.getOwnerTel());

        QuarantineCert cert = new QuarantineCert();
        cert.setRecordId(base.getRecordId());
        cert.setPetId(base.getPetId());
        cert.setClientId(base.getClientId());
        cert.setDoctorId(base.getDoctorId());
        cert.setOwnerName(base.getOwnerName());
        cert.setOwnerTel(base.getOwnerTel());
        cert.setPetName(base.getPetName());
        cert.setSpecies(base.getSpecies());
        cert.setBreed(base.getBreed());
        cert.setSex(base.getSex());
        cert.setAgeDesc(base.getAgeDesc());
        cert.setWeight(base.getWeight());
        cert.setContent(base.getContent());
        cert.setItemsJson(writeItems(base.getItems()));
        cert.setIsDel(0L);

        // 按天自增单号，唯一索引冲突重试
        for (int i = 0; i < 3; i++) {
            cert.setCertNo(nextCertNo(LocalDate.now()));
            try {
                save(cert);
                return toVo(cert);
            } catch (DuplicateKeyException e) {
                log.warn("检疫单号冲突重试 certNo={}", cert.getCertNo());
            }
        }
        throw new BizException("检疫单号生成失败，请重试");
    }

    @Override
    public QuarantineCertVo selectByNo(String certNo) {
        if (certNo == null || certNo.isBlank()) throw new BizException("缺少检疫单号");
        QuarantineCert cert = lambdaQuery()
                .eq(QuarantineCert::getCertNo, certNo)
                .eq(QuarantineCert::getIsDel, 0L).one();
        if (cert == null) throw new BizException("检疫单不存在");
        return toVo(cert);
    }

    // ---------------------------------------------------------------- 内部

    /** 单号 JY-YYYYMMDD-XXXX：取当天最大流水 +1 */
    private String nextCertNo(LocalDate day) {
        String prefix = "JY-" + day.format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        QuarantineCert max = baseMapper.selectOne(new LambdaQueryWrapper<QuarantineCert>()
                .likeRight(QuarantineCert::getCertNo, prefix)
                .orderByDesc(QuarantineCert::getCertNo).last("limit 1"));
        int seq = 1;
        if (max != null && max.getCertNo() != null && max.getCertNo().length() > prefix.length()) {
            try {
                seq = Integer.parseInt(max.getCertNo().substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return prefix + String.format("%04d", seq);
    }

    private QuarantineCertVo toVo(QuarantineCert cert) {
        QuarantineCertVo vo = new QuarantineCertVo();
        vo.setCertNo(cert.getCertNo());
        vo.setRecordId(cert.getRecordId());
        vo.setPetId(cert.getPetId());
        vo.setClientId(cert.getClientId());
        vo.setDoctorId(cert.getDoctorId());
        vo.setOwnerName(cert.getOwnerName());
        vo.setOwnerTel(cert.getOwnerTel());
        vo.setPetName(cert.getPetName());
        vo.setSpecies(cert.getSpecies());
        vo.setBreed(cert.getBreed());
        vo.setSex(cert.getSex());
        vo.setAgeDesc(cert.getAgeDesc());
        vo.setWeight(cert.getWeight());
        vo.setContent(cert.getContent());
        vo.setCreateTime(cert.getCreateTime());
        vo.setExisted(true);
        Doctor doctor = cert.getDoctorId() == null ? null : doctorMapper.selectById(cert.getDoctorId());
        if (doctor != null) vo.setDoctorName(doctor.getDoctorName());
        vo.setItems(readItems(cert.getItemsJson()));
        return vo;
    }

    private List<QuarantineItemVo> toItems(List<Prescription> list) {
        List<QuarantineItemVo> out = new ArrayList<>();
        if (list == null) return out;
        for (Prescription p : list) {
            if (Boolean.TRUE.equals(p.getIsCancel())) continue;
            QuarantineItemVo it = new QuarantineItemVo();
            it.setDrugName(p.getDrugName());
            it.setDrugSpec(p.getDrugSpec());
            it.setDrugUnit(p.getDrugUnit());
            it.setQuantity(p.getQuantity());
            it.setUsageInfo(p.getUsageInfo());
            it.setSingleDose(p.getSingleDose());
            it.setSingleDoseUnit(p.getSingleDoseUnit());
            it.setDailyTimes(p.getDailyTimes());
            it.setDurationDays(p.getDurationDays());
            it.setDosage(dosageText(p));
            out.add(it);
        }
        return out;
    }

    /** 实际用量：优先结构化（单次×频次×天数），否则回退用法文本 */
    private String dosageText(Prescription p) {
        if (p.getSingleDose() == null) return p.getUsageInfo() == null ? "" : p.getUsageInfo();
        String unit = p.getSingleDoseUnit() == null ? "" : p.getSingleDoseUnit();
        StringBuilder sb = new StringBuilder();
        sb.append("单次").append(p.getSingleDose().stripTrailingZeros().toPlainString()).append(unit);
        sb.append("×每日").append(p.getDailyTimes() == null ? 1 : p.getDailyTimes()).append("次");
        if (p.getDurationDays() != null) sb.append("×").append(p.getDurationDays()).append("天");
        if (p.getUsageInfo() != null && !p.getUsageInfo().isBlank()) sb.append("（").append(p.getUsageInfo()).append("）");
        return sb.toString();
    }

    /** 种类推断：pet_variety 存的是品种（金毛犬/柴犬/椰羊），按关键字归大类 */
    private String inferSpecies(String variety, String info) {
        String s = (variety == null ? "" : variety) + (info == null ? "" : info);
        if (s.contains("猫")) return "猫";
        if (s.contains("兔")) return "兔";
        if (s.contains("仓鼠")) return "仓鼠";
        if (s.contains("龙猫")) return "龙猫";
        if (s.contains("鱼")) return "观赏鱼";
        if (s.contains("鸟") || s.contains("鹦鹉")) return "鸟";
        if (s.contains("犬") || s.contains("狗")) return "犬";
        return variety;
    }

    private String ageDesc(LocalDate birthday) {
        if (birthday == null) return null;
        Period p = Period.between(birthday, LocalDate.now());
        if (p.getYears() <= 0) return Math.max(0, p.getMonths()) + "月";
        return p.getYears() + "岁" + (p.getMonths() > 0 ? p.getMonths() + "月" : "");
    }

    private String writeItems(List<QuarantineItemVo> items) {
        try {
            return MAPPER.writeValueAsString(items == null ? List.of() : items);
        } catch (Exception e) {
            log.warn("检疫清单序列化失败", e);
            return "[]";
        }
    }

    private List<QuarantineItemVo> readItems(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return MAPPER.readValue(json, new TypeReference<List<QuarantineItemVo>>() {
            });
        } catch (Exception e) {
            log.warn("检疫清单解析失败", e);
            return new ArrayList<>();
        }
    }
}
