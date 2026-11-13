package com.mix.wx;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信能力配置（P0-3 骨架）：全部参数占位，enabled=false 时支付与模板消息全链路旁路。
 * 见 application.yml「mix.wx」块与接入清单注释。
 */
@Data
@Component
@ConfigurationProperties(prefix = "mix.wx")
public class WeChatProperties {
    /** 全局开关：false 时所有微信接口直接抛"未启用"/静默跳过 */
    private boolean enabled;
    /** 公众号/服务号 appid */
    private String appId;
    private String appSecret;
    /** 商户号 */
    private String mchId;
    /** 商户 API 证书序列号 */
    private String mchSerialNo;
    /** APIv3 密钥（回调解密用） */
    private String apiV3Key;
    /** 商户私钥文件路径（apiclient_key.pem） */
    private String privateKeyPath;
    /** 微信支付回调地址（公网 https） */
    private String notifyUrl;
    /** 公众号 OAuth 网页授权回调地址 */
    private String oauthRedirectUri;
    /** 4 类模板消息 ID（注册成功/就诊提醒/缴费提醒/免疫提醒） */
    private TemplateIds templateIds = new TemplateIds();

    @Data
    public static class TemplateIds {
        /** ①挂号成功通知 */
        private String registerSuccess;
        /** ②就诊提醒 */
        private String visitRemind;
        /** ③缴费提醒 */
        private String payRemind;
        /** ④免疫到期提醒 */
        private String immuneRemind;
    }
}
