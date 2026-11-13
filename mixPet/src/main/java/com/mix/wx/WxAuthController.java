package com.mix.wx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mix.common.BizException;
import com.mix.model.entity.Client;
import com.mix.service.IClientService;
import com.mix.util.UserUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * 公众号 OAuth 网页授权（openid 绑定骨架）：
 * 1. 登录态顾客调 GET /wx/auth/url 拿授权跳转地址（state 带 clientId+HMAC 签名防伪造绑定）；
 * 2. 微信回跳 GET /wx/auth/callback（放行）→ code 换 openid → 落 client.client_openid。
 * 流程文档见 application.yml mix.wx 注释与 GAP_LIST P0-3。
 */
@Slf4j
@RestController
@RequestMapping("/wx")
public class WxAuthController {
    private final WeChatProperties props;
    private final IClientService clientService;
    /** 复用全局验证码 HMAC 密钥签 state（不引入新密钥管理面） */
    @Value("${app.verify-code-secret}")
    private String stateSecret;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();

    public WxAuthController(WeChatProperties props, IClientService clientService) {
        this.props = props;
        this.clientService = clientService;
    }

    @PostConstruct
    void checkSecret() {
        if (stateSecret == null || stateSecret.isBlank())
            throw new IllegalStateException("app.verify-code-secret 缺失：state 签名无法进行");
    }

    /** 登录态顾客：生成公众号授权跳转地址（前端拿 url 后 window.location 跳转） */
    @GetMapping("/auth/url")
    public String authUrl() {
        if (!props.isEnabled()) throw new BizException("微信能力未启用");
        if (!(UserUtil.getUser() instanceof Client client) || client.getClientId() == null)
            throw new BizException("仅顾客账号可绑定微信");
        String state = signState(client.getClientId());
        String redirect = URLEncoder.encode(props.getOauthRedirectUri(), StandardCharsets.UTF_8);
        return "https://open.weixin.qq.com/connect/oauth2/authorize?appid=" + props.getAppId()
                + "&redirect_uri=" + redirect
                + "&response_type=code&scope=snsapi_base&state=" + state + "#wechat_redirect";
    }

    /** 微信回跳（放行）：code 换 openid 并绑定到 state 里的客户 */
    @GetMapping("/auth/callback")
    public String callback(@RequestParam String code, @RequestParam String state) {
        try {
            Integer clientId = verifyState(state);
            String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + props.getAppId()
                    + "&secret=" + props.getAppSecret()
                    + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + "&grant_type=authorization_code";
            HttpResponse<String> resp = http.send(HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode node = json.readTree(resp.body());
            String openid = node.path("openid").asText(null);
            if (openid == null || openid.isBlank())
                return html("绑定失败：微信授权码无效或已使用，请重新从公众号进入绑定。");
            clientService.lambdaUpdate()
                    .eq(Client::getClientId, clientId)
                    .set(Client::getClientOpenid, openid)
                    .update();
            log.info("微信 openid 绑定成功 clientId={}", clientId);
            return html("绑定成功，请关闭此页面并返回公众号继续操作。");
        } catch (Exception e) {
            log.warn("微信绑定回调异常", e);
            return html("绑定失败：" + (e.getMessage() == null ? "未知错误" : e.getMessage()));
        }
    }

    // ===== state 签名：base64url(clientId + "." + HMAC-SHA256(secret, clientId)) =====

    private String signState(Integer clientId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stateSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(String.valueOf(clientId).getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
            return clientId + "." + encoded;
        } catch (Exception e) {
            throw new IllegalStateException("state 签名失败", e);
        }
    }

    private Integer verifyState(String state) {
        if (state == null || !state.contains(".")) throw new BizException("绑定 state 非法");
        int dot = state.indexOf('.');
        Integer clientId;
        try {
            clientId = Integer.valueOf(state.substring(0, dot));
        } catch (NumberFormatException e) {
            throw new BizException("绑定 state 非法");
        }
        String expect = signState(clientId);
        if (!java.security.MessageDigest.isEqual(expect.getBytes(StandardCharsets.UTF_8),
                state.getBytes(StandardCharsets.UTF_8)))
            throw new BizException("绑定 state 校验失败");
        return clientId;
    }

    private String html(String msg) {
        // 走 OAuth 回调的是微信内置浏览器，直接回纯 HTML
        return "<!DOCTYPE html><meta charset='utf-8'><body style='text-align:center;padding-top:15vh;font-size:18px'>"
                + msg + "</body>";
    }
}
