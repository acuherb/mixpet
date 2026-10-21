package com.mix.model.vo;

/**
 * AI 辅助诊疗接口返回（不动通用 Result 结构）
 *
 * @param code   1成功 0失败
 * @param msg    提示文案
 * @param advice 结构化建议（失败为 null）
 */
public record AiAssistResult(Integer code, String msg, AiAdviceVo advice) {
    public static AiAssistResult ok(AiAdviceVo advice) {
        return new AiAssistResult(1, "已生成辅助建议（仅供参考）", advice);
    }

    public static AiAssistResult fail(String msg) {
        return new AiAssistResult(0, msg, null);
    }
}
