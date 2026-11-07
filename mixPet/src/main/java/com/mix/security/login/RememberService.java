package com.mix.security.login;

import com.mix.security.details.ClientDetails;
import com.mix.security.details.DoctorDetails;
import com.mix.security.model.MyUser;
import com.mix.security.model.RoleEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * 记住我（Cookie 签名改为 HMAC-SHA256，密钥由环境变量强校验提供）
 */
@Component
public class RememberService extends AbstractRememberMeServices {
    private static final String SIGN_ALGORITHM = "HmacSHA256";
    private final Log log = LogFactory.getLog(this.getClass());
    private final ClientDetails clientDetails;
    private final DoctorDetails doctorDetails;

    public RememberService(ClientDetails clientDetails,
                           DoctorDetails doctorDetails,
                           @Value("${remember-me.key:}") String key) {
        // 在 super 参数里做强校验：密钥缺失/过短/公开模板值 → 拒绝启动，不再有可推导的兜底密钥
        super(requireStrongKey(key), clientDetails);
        this.clientDetails = clientDetails;
        this.doctorDetails = doctorDetails;
    }

    private static String requireStrongKey(String key) {
        if (key == null || key.length() < 32
                || key.equals("YourSecureRememberMeKeyAtLeast32Chars!")
                || key.equals("MixPet@Remember2026")) {
            throw new IllegalStateException(
                    "REMEMBER_ME_KEY 未配置、短于 32 字符或仍是公开模板值，拒绝启动（请在 .env 中配置强随机密钥）");
        }
        return key;
    }

    @Override
    protected void onLoginSuccess(HttpServletRequest req, HttpServletResponse resp, Authentication sucAut) {
        if (sucAut.getPrincipal() instanceof MyUser myUser) {
            String username = myUser.username();
            String password = myUser.password();
            RoleEnum roleEnum = myUser.roleEnum();
            if (!StringUtils.hasLength(username)) return;
            long expiryTime = System.currentTimeMillis() + 1000L * 3600L * 3;
            String signatureValue = this.makeTokenSignature(expiryTime, username, password, roleEnum.name());
            setCookie(new String[]{username, Long.toString(expiryTime), roleEnum.name(),
                    SIGN_ALGORITHM, signatureValue}, 3 * 3600, req, resp);
        }
    }

    @Override
    protected boolean rememberMeRequested(HttpServletRequest req, String parameter) {
        if (JsonUtil.isJson(req)) {
            return Boolean.TRUE.equals(req.getAttribute("rememberMe"));
        }
        return false;
    }

    @Override
    protected UserDetails processAutoLoginCookie(String[] cookieTokens, HttpServletRequest req, HttpServletResponse resp)
            throws RememberMeAuthenticationException, UsernameNotFoundException {
        if (cookieTokens.length != 5) {
            throw new InvalidCookieException("Cookie 字段数不符");
        }
        long expiryTime;
        try {
            expiryTime = Long.parseLong(cookieTokens[1]);
        } catch (NumberFormatException e) {
            throw new InvalidCookieException("Cookie 时间格式错误");
        }
        if (expiryTime < System.currentTimeMillis()) {
            throw new InvalidCookieException("Cookie 已过期");
        }
        String name = cookieTokens[0];
        String role = cookieTokens[2];
        // 先加载用户：active=0（停用/黑名单）会在此抛 UsernameNotFoundException，从而被拦截
        UserDetails user = role.equals(RoleEnum.DOCTOR.name())
                ? doctorDetails.loadUserByUsername(name)
                : clientDetails.loadUserByUsername(name);
        // 签名含密码哈希与 key，密码变更后自动失效，需重新登录
        String presentedSignature = cookieTokens[4];
        String expectedSignature = makeTokenSignature(expiryTime, name, user.getPassword(), role);
        if (!expectedSignature.equals(presentedSignature)) {
            throw new InvalidCookieException("Cookie 签名不符");
        }
        return user;
    }

    /** HMAC-SHA256(key, username:expiry:passwordHash:role)，不再使用可碰撞的 MD5 拼接 */
    private String makeTokenSignature(long expiryTime, String username, String password, String role) {
        String data = username + ":" + expiryTime + ":" + password + ":" + role;
        try {
            Mac mac = Mac.getInstance(SIGN_ALGORITHM);
            mac.init(new SecretKeySpec(getKey().getBytes(StandardCharsets.UTF_8), SIGN_ALGORITHM));
            return new String(Hex.encode(mac.doFinal(data.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception e) {
            throw new IllegalStateException("记住我签名失败", e);
        }
    }
}
