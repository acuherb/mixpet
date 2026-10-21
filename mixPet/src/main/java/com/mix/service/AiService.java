package com.mix.service;

import com.mix.model.vo.AiAdviceVo;

/**
 * AI 辅助诊疗服务：
 * 由病历全量档案构建上下文 → Provider 生成结构化建议 → 留痕 + 防重复
 */
public interface AiService {
    /**
     * 生成（或命中缓存返回）辅助诊疗建议
     *
     * @param recordId 病历id
     */
    AiAdviceVo assist(Integer recordId);
}
