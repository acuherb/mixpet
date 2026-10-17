/**
 * AI 辅助诊疗：结构化建议（与后端 AiAdviceVo 一一对应）
 */
export interface AiPri {
    /** 可能疾病 */
    cond: string;
    /** 倾向：高/中/低 */
    prob: string;
    /** 依据 */
    basis: string;
}

export interface AiAdvice {
    /** 鉴别诊断 */
    differentials: AiPri[];
    /** 建议检查 */
    examSuggest: string[];
    /** 指标解读 */
    indexExplain: string[];
    /** 治疗参考 */
    treatRef: string[];
    /** 向宠主说明文案 */
    ownerNote: string;
    /** 复诊建议 */
    revisitSuggest: string;
}

/** AI 接口返回（不动通用 Res 结构） */
export interface AiAssistResult {
    code: number;
    msg: string;
    advice: AiAdvice | null;
}
