package com.mix.service.impl;

import com.mix.model.vo.AiAdviceVo;
import com.mix.model.vo.AiContext;
import com.mix.service.AiProvider;
import org.springframework.stereotype.Component;

/**
 * 已停用的占位 Provider：未接入真实模型时不产生任何诊断建议。
 * 保留 Bean 只是为了让 AiServiceImpl 的构造注入在无模型时仍可启动；
 * 一旦被调用即明确拒绝，绝不返回关键词拼装的假数据。
 */
@Component
public class MockAiProvider implements AiProvider {

    @Override
    public String name() {
        return "unavailable";
    }

    @Override
    public AiAdviceVo assist(AiContext ctx) {
        throw new IllegalStateException("AI 辅助未启用：请先配置真实模型提供方");
    }
}
