package com.mix.controller;

import com.mix.model.vo.AiAdviceVo;
import com.mix.model.vo.AiAssistResult;
import com.mix.security.model.RoleConst;
import com.mix.service.AiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 辅助诊疗（MVP：接口抽象 + Mock Provider + 留痕）
 * 合规：输出仅供参考；AI 不写业务库，仅医生确认后一键填入
 */
@RestController
@RequestMapping("/ai")
public class AiController {
    private final AiService aiService;
    private final boolean enabled;

    public AiController(AiService aiService, @Value("${ai.enabled:true}") boolean enabled) {
        this.aiService = aiService;
        this.enabled = enabled;
    }

    /**
     * 生成辅助诊疗建议（同一病历相同内容命中缓存不重复生成；留痕 ai_advice）
     */
    @PostMapping("/assist")
    @Secured(RoleConst.DOCTOR)
    public AiAssistResult assist(@RequestParam Integer recordId) {
        if (!enabled) return AiAssistResult.fail("AI 辅助功能未启用");
        try {
            AiAdviceVo vo = aiService.assist(recordId);
            return AiAssistResult.ok(vo);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return AiAssistResult.fail(e.getMessage());
        } catch (Exception e) {
            // 兜底：AI 是旁路功能，异常绝不阻塞接诊
            return AiAssistResult.fail("AI 辅助生成失败，请稍后重试或继续手动接诊");
        }
    }
}
