package com.mix.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mix.model.entity.Client;
import com.mix.util.UserUtil;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 顾客端响应脱敏：仅当当前登录者是 CLIENT 时，按字段名黑名单递归剔除院内专属字段。
 *
 * <p>背景：项目里顾客可读接口（/record/detail、/record/one、/pet/archive、/bill/self、
 * /lab/record、/imageOrder/record、/checkup/record 等）普遍只做「是不是我的」归属校验，
 * 却把完整实体/DTO 原样返回，导致 internal_note、进价 cost、审核/签名版本、医生术前术后记录、
 * 客户 openid/身份证等院内字段出现在顾客可见的 HTTP 响应里，仅靠前端不渲染来遮住。</p>
 *
 * <p>选择 ResponseBodyAdvice 而非逐个 Controller 裁剪，是因为顾客可读接口有 30+ 个且分散，
 * 单点兜底能覆盖新接口；员工端与匿名请求完全不处理，不影响后台与首页公开数据。</p>
 *
 * <p>黑名单刻意保守：只删确凿院内专属且顾客端不渲染的字段。费用、诊断、处方、结果、
 * refRange、auditStatus/auditReason（注册审核提示要用）、clientBalance/Points（本人余额积分）、
 * isCancel（处方作废标记）、dispenser（处方笺发药人）等一律保留，避免破坏顾客端功能。</p>
 */
@RestControllerAdvice
public class ClientViewAdvice implements ResponseBodyAdvice<Object> {

    private static final Set<String> HIDDEN = Set.of(
            // 病历院内字段
            "internalNote", "reviewStatus", "reviewBy", "reviewAt", "reviewNote",
            "signedBy", "signedAt", "version", "cancelReason",
            "differentialDiagnosis", "diagnosisBasis", "diagnosisCode", "diagnosisCodeSystem",
            // 成本 / 进价（毛利口径，绝不可下发）
            "cost",
            // 检验 / 影像内部字段（结果值 resultValue、refRange、status、result 均保留）
            "orderNote", "resultBy", "resultRevision",
            "criticalHandled", "criticalHandleBy", "criticalHandleAt", "criticalNotifyAt",
            "dicomUid", "dicomSeries", "imageFiles",
            // 医生内部记录
            "preopNote", "intraopNote", "postopNote",
            // 操作人 / 审计 / 单据内部状态
            "depositBy", "followBy", "payBy", "refundBy", "dispenseBy",
            "printNo", "copies", "supplementStatus",
            // 微信 openid 属凭据，不下发；身份证/黑名单虽敏感但 clientOne.vue 本人可见可编辑，
            // 删掉会让资料页显示为空并提交时误清空，故保留（本人余额/积分/审核状态同理保留）
            "clientOpenid",
            "createName", "updateName", "isDel"
    );

    private final ObjectMapper mapper;

    public ClientViewAdvice(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** 只处理 JSON 响应；返回 String / 文件流的接口由其它 converter 处理，不能返回 Map 否则类型转换异常 */
    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null || !(UserUtil.getUser() instanceof Client)) return body;
        Object tree;
        try {
            tree = mapper.convertValue(body, Object.class);
        } catch (Exception e) {
            // 转换失败宁可原样返回，也不要把接口打挂
            return body;
        }
        strip(tree);
        return tree;
    }

    @SuppressWarnings("unchecked")
    private static void strip(Object node) {
        if (node instanceof Map<?, ?> map) {
            ((Map<Object, Object>) map).keySet()
                    .removeIf(k -> k instanceof String s && HIDDEN.contains(s));
            for (Object v : map.values()) strip(v);
        } else if (node instanceof List<?> list) {
            for (Object v : list) strip(v);
        }
    }
}
