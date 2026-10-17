package com.ship.service;

import com.ship.model.vo.AiAdviceVo;
import com.ship.model.vo.AiContext;

/**
 * AI 辅助诊疗 Provider 抽象：
 * MVP 用 MockAiProvider（规则演示，不联网）；上线期实现 DeepSeekProvider（JDK HttpClient）即可切换，业务无感
 */
public interface AiProvider {
    /** 提供方标识：mock / deepseek */
    String name();

    /** 基于病历上下文生成结构化建议 */
    AiAdviceVo assist(AiContext ctx);
}
