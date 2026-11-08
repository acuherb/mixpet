package com.mix.service;

import com.mix.model.vo.AiAdviceVo;
import com.mix.model.vo.AiContext;

/**
 * AI 辅助诊疗 Provider 抽象（未来接真实模型的扩展点）。
 * 当前无可用实现、ai.enabled=false：不得提供规则/演示假建议。
 * 接入方式：新增 XxxAiProvider implements AiProvider + @Component，并设 AI_ENABLED=true。
 */
public interface AiProvider {
    /** 提供方标识：mock / deepseek */
    String name();

    /** 基于病历上下文生成结构化建议 */
    AiAdviceVo assist(AiContext ctx);
}
