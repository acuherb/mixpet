package com.mix.model.vo;

import java.util.List;

/**
 * AI 辅助诊疗结构化建议（Provider 统一输出，前端按节渲染）
 */
public record AiAdviceVo(
        /** 鉴别诊断：条件/概率/依据 */
        List<Pri> differentials,
        /** 建议检查 */
        List<String> examSuggest,
        /** 指标解读 */
        List<String> indexExplain,
        /** 治疗参考 */
        List<String> treatRef,
        /** 向宠主说明文案 */
        String ownerNote,
        /** 复诊建议 */
        String revisitSuggest
) {
    /** 鉴别诊断项 */
    public record Pri(String cond, String prob, String basis) {
    }
}
