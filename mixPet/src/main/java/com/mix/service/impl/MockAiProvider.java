package com.mix.service.impl;

import com.mix.model.vo.AiAdviceVo;
import com.mix.model.vo.AiContext;
import com.mix.service.AiProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock AI Provider（MVP 演示用，不联网）：
 * 按主诉/现病史关键词给出常见病症的鉴别诊断、检查建议，
 * 结合检验异常项做解读，输出治疗参考与宠主文案。
 * 上线期由 DeepSeekProvider 替换，业务代码不变。
 */
@Component
public class MockAiProvider implements AiProvider {

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public AiAdviceVo assist(AiContext ctx) {
        String cc = nullToEmpty(ctx.getChiefComplaint()) + nullToEmpty(ctx.getPresentIllness())
                + nullToEmpty(ctx.getPhysicalExam());
        List<AiAdviceVo.Pri> diffs = new ArrayList<>();
        List<String> exams = new ArrayList<>();

        if (cc.contains("呕") || cc.contains("吐")) {
            diffs.add(new AiAdviceVo.Pri("急性胃肠炎", "高", "呕吐伴食欲下降为消化道常见表现，需结合排便与饮食史"));
            diffs.add(new AiAdviceVo.Pri("胰腺炎", "中", "中年犬多发，呕吐伴腹痛/弓背需排查，建议结合血脂/胰酶与超声"));
            diffs.add(new AiAdviceVo.Pri("胃肠异物阻塞", "中", "玩耍期犬猫需警惕误食，呕吐渐重建议影像排查"));
            exams.add("腹部超声（排查异物/胰腺/肠管状态）");
            exams.add("血常规 + CRP（评估炎症程度）");
        } else if (cc.contains("咳")) {
            diffs.add(new AiAdviceVo.Pri("传染性呼吸道感染（犬窝咳等）", "高", "咳嗽为主要症状，接触史有助判断"));
            diffs.add(new AiAdviceVo.Pri("肺炎", "中", "咳嗽伴发热/呼吸加快需排查，建议胸片评估"));
            diffs.add(new AiAdviceVo.Pri("心源性咳嗽", "中", "老年犬夜间咳嗽需结合心音与心超"));
            exams.add("胸部 DR 正侧位片");
            exams.add("血常规 + 呼吸道病原筛查");
        } else if (cc.contains("腹泻") || cc.contains("拉稀") || cc.contains("痢")) {
            diffs.add(new AiAdviceVo.Pri("急性肠炎", "高", "腹泻最常见为饮食/感染因素，注意脱水程度"));
            diffs.add(new AiAdviceVo.Pri("寄生虫性肠炎", "中", "幼年动物需粪检查找虫卵/球虫"));
            diffs.add(new AiAdviceVo.Pri("细小病毒性肠炎", "中", "未免疫幼犬呕吐+腥臭血便需高度警惕"));
            exams.add("粪检（虫卵/球虫/潜血）");
            exams.add("细小/冠状病毒检测（幼龄未免疫动物）");
        } else if (cc.contains("跛") || cc.contains("瘸") || cc.contains("关节")) {
            diffs.add(new AiAdviceVo.Pri("外伤/软组织损伤", "高", "突发跛行先排除外伤，结合触诊"));
            diffs.add(new AiAdviceVo.Pri("骨科疾病（髌骨/髋关节/韧带）", "中", "小型犬髌骨脱位、大型犬十字韧带损伤常见"));
            exams.add("患肢 X 线（关节/骨骼）");
        } else {
            diffs.add(new AiAdviceVo.Pri("需补充问诊信息", "—", "当前主诉/现病史信息较少，完善后再获取更可靠的鉴别方向"));
            exams.add("血常规 + 生化（基础评估）");
        }

        if (ctx.getWeight() != null && ctx.getWeight().compareTo(BigDecimal.ZERO) > 0)
            exams.add("按体重 " + ctx.getWeight() + "kg 核算给药剂量与输液量");

        // 指标解读：基于检验异常项（关联解读，而非逐条罗列超限）
        List<String> explain = new ArrayList<>();
        if (ctx.getLabAbnormal() != null && !ctx.getLabAbnormal().isEmpty()) {
            for (AiContext.LabAbnormal a : ctx.getLabAbnormal()) {
                String v = nullToEmpty(a.getResultValue()) + (a.getUnit() == null ? "" : a.getUnit());
                String ref = a.getRefRange() == null ? "" : "（参考 " + a.getRefRange() + "）";
                explain.add(a.getItemName() + " " + v + ref + " 偏离参考范围，建议结合其他指标与临床表现综合判断其意义");
            }
            explain.add("多项指标异常时建议关注整体趋势（如炎症+肝肾功能组合异常），而非孤立看待单项");
        } else {
            explain.add("暂无检验异常项；录入检验结果后将在此做关联解读");
        }
        if (ctx.getImageReports() != null && !ctx.getImageReports().isEmpty())
            explain.add("影像报告已出，建议与检验结果互为印证（如影像定位 + 化验定性）");

        List<String> treat = List.of(
                "对症支持：按脱水程度补液，消化道病例短期禁食后少量多餐过渡",
                "按检验/影像结果选择用药，避免无指征使用抗生素",
                "记录 24 小时进食、排便、精神与体温变化，复诊时对比评估疗效");

        String pet = ctx.getPetName() == null ? "宝贝" : ctx.getPetName();
        String brief = ctx.getChiefComplaint() == null || ctx.getChiefComplaint().isBlank()
                ? "身体不适" : ctx.getChiefComplaint();
        String ownerNote = "您好，" + pet + "目前主要表现为" + brief
                + "。我们已为它安排相应检查，初步方向已记录在病历医嘱中，接下来会按方案治疗并观察反应。"
                + "治疗期间如出现精神明显变差、症状加重或拒食等情况，请随时联系我们。";
        String revisit = "建议 3-5 天内复诊评估疗效；若症状加重或出现新情况，请提前就诊。";

        return new AiAdviceVo(diffs, exams, explain, treat, ownerNote, revisit);
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
