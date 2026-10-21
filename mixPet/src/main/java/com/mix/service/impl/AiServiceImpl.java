package com.mix.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mix.mapper.AiAdviceMapper;
import com.mix.model.dto.RecordDto;
import com.mix.model.entity.AiAdvice;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.ImageOrder;
import com.mix.model.entity.LabOrderItem;
import com.mix.model.entity.Pet;
import com.mix.model.vo.AiAdviceVo;
import com.mix.model.vo.AiContext;
import com.mix.model.vo.RecordFullVo;
import com.mix.service.AiProvider;
import com.mix.service.AiService;
import com.mix.service.IMedicalRecordService;
import com.mix.service.IPetService;
import com.mix.util.UserUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 辅助诊疗服务实现：
 * buildContext(RecordFullVo) → 防重复(request_hash) → Provider.assist → 留痕(ai_advice)
 * MVP 注入 MockAiProvider；上线期改为按 yml(ai.provider) 选择 Provider，业务无感
 */
@Service
public class AiServiceImpl implements AiService {
    private final IMedicalRecordService recordService;
    private final IPetService petService;
    private final AiAdviceMapper aiAdviceMapper;
    private final AiProvider aiProvider;
    private final boolean enabled;
    private final ObjectMapper json = new ObjectMapper();

    public AiServiceImpl(IMedicalRecordService recordService,
                         IPetService petService,
                         AiAdviceMapper aiAdviceMapper,
                         AiProvider aiProvider,
                         @Value("${ai.enabled:true}") boolean enabled) {
        this.recordService = recordService;
        this.petService = petService;
        this.aiAdviceMapper = aiAdviceMapper;
        this.aiProvider = aiProvider;
        this.enabled = enabled;
    }

    @Override
    public AiAdviceVo assist(Integer recordId) {
        if (!enabled) throw new IllegalStateException("AI 辅助功能未启用");
        RecordFullVo full = recordService.selectRecordFull(recordId);
        if (full == null || full.record() == null) throw new IllegalArgumentException("病历不存在");
        AiContext ctx = buildContext(full);

        // 防重复：同一病历+主诉+诊断的摘要 hash 命中则直接返回缓存（省成本）
        String hash = DigestUtils.md5DigestAsHex(
                (recordId + "|" + nullToEmpty(ctx.getChiefComplaint()) + "|" + nullToEmpty(ctx.getDiagnosis()))
                        .getBytes(StandardCharsets.UTF_8));
        AiAdvice cached = aiAdviceMapper.selectOne(Wrappers.<AiAdvice>lambdaQuery()
                .eq(AiAdvice::getRecordId, recordId)
                .eq(AiAdvice::getRequestHash, hash)
                .orderByDesc(AiAdvice::getId)
                .last("limit 1"));
        if (cached != null && cached.getAdviceJson() != null && !cached.getAdviceJson().isBlank()) {
            try {
                return json.readValue(cached.getAdviceJson(), AiAdviceVo.class);
            } catch (Exception ignore) {
                // 缓存解析失败则重新生成
            }
        }

        AiAdviceVo vo = aiProvider.assist(ctx);

        // 留痕：谁触发/何时/入参摘要/provider/生成内容
        AiAdvice log = new AiAdvice();
        log.setRecordId(recordId.longValue());
        log.setProvider(aiProvider.name());
        log.setRequestHash(hash);
        log.setDoctor(currentDoctorId());
        try {
            log.setAdviceJson(json.writeValueAsString(vo));
        } catch (Exception e) {
            log.setAdviceJson(null);
        }
        aiAdviceMapper.insert(log);
        return vo;
    }

    /** 由病历全量档案提取 AI 上下文（宠物档案 + 病历内容 + 体征 + 检验异常 + 影像报告） */
    private AiContext buildContext(RecordFullVo full) {
        RecordDto r = full.record();
        AiContext ctx = new AiContext();
        ctx.setRecordId(r.getRecordId());
        ctx.setPetName(r.getPetName());
        ctx.setDoctorName(r.getDoctorName());
        ctx.setChiefComplaint(r.getChiefComplaint());
        ctx.setPresentIllness(r.getPresentIllness());
        ctx.setPastHistory(r.getPastHistory());
        ctx.setWeight(r.getWeight());
        ctx.setTemperature(r.getTemperature());
        ctx.setHeartRate(r.getHeartRate());
        ctx.setRespiratoryRate(r.getRespiratoryRate());
        ctx.setBloodPressure(r.getBloodPressure());
        ctx.setPhysicalExam(r.getPhysicalExam());
        ctx.setDiagnosis(r.getDiagnosis());
        // 检验异常项（只取标记异常的明细，做关联解读）
        List<AiContext.LabAbnormal> labs = new ArrayList<>();
        if (full.labOrders() != null) {
            for (RecordFullVo.LabOrderFull lo : full.labOrders()) {
                if (lo.items() == null) continue;
                for (LabOrderItem it : lo.items()) {
                    if (Boolean.TRUE.equals(it.getIsAbnormal())) {
                        AiContext.LabAbnormal a = new AiContext.LabAbnormal();
                        a.setItemName(it.getItemName());
                        a.setResultValue(it.getResultValue());
                        a.setUnit(it.getItemUnit());
                        a.setRefRange(it.getRefRange());
                        labs.add(a);
                    }
                }
            }
        }
        ctx.setLabAbnormal(labs);
        // 影像报告文本
        List<String> imgs = new ArrayList<>();
        if (full.imageOrders() != null) {
            for (ImageOrder o : full.imageOrders()) {
                if (o.getResult() != null && !o.getResult().isBlank()) imgs.add(o.getResult());
            }
        }
        ctx.setImageReports(imgs);
        // 宠物档案（品种/性别/年龄）
        if (r.getPetId() != null) {
            Pet pet = petService.getById(r.getPetId());
            if (pet != null) {
                ctx.setBreed(pet.getPetVariety());
                ctx.setPetSex(Boolean.TRUE.equals(pet.getPetSex()) ? "公" : "母");
                if (pet.getPetBirthday() != null) {
                    Period p = Period.between(pet.getPetBirthday(), LocalDate.now());
                    ctx.setPetAge(p.getYears() + "岁" + p.getMonths() + "个月");
                }
            }
        }
        return ctx;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** 当前登录医生 id（未登录/非医生为 null） */
    private Long currentDoctorId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) return null;
        if (UserUtil.getUser() instanceof Doctor d) return d.getDoctorId().longValue();
        return null;
    }
}
