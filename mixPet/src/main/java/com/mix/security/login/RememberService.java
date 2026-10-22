package com.mix.security.login;

import com.mix.security.details.ClientDetails;
import com.mix.security.details.DoctorDetails;
import com.mix.security.model.MyUser;
import com.mix.security.model.RoleEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * 记住我
 */
@Component
public class RememberService extends AbstractRememberMeServices {
    private final Log log = LogFactory.getLog(this.getClass());
    private final ClientDetails clientDetails;
    private final DoctorDetails doctorDetails;

    public RememberService(ClientDetails clientDetails,
                           DoctorDetails doctorDetails,
                           @Value("${remember-me.key:}") String key) {
        // 兜底：配置为空（如环境变量未注入）时用固定默认值，避免启动即抛 key cannot be empty
        super(StringUtils.hasLength(key) ? key : "MixPetDefaultRememberKey", clientDetails);
        this.clientDetails = clientDetails;
        this.doctorDetails = doctorDetails;
    }

    @Override
    protected void onLoginSuccess(HttpServletRequest req, HttpServletResponse resp, Authentication sucAut) {
        log.info("登录成功的记住我处理");
        if (sucAut.getPrincipal() instanceof MyUser myUser) {
            String username = myUser.username();
            String password = myUser.password();
            RoleEnum roleEnum = myUser.roleEnum();
            if (!StringUtils.hasLength(username)) {
                log.info("用户名为空");
                return;
            }
            long expiryTime = System.currentTimeMillis();
            // 记住我3个小时
            int lifeHour = 3;
            expiryTime += 1000L * 3600L * (long) lifeHour;
            String signatureValue = this.makeTokenSignature(expiryTime, username, password, roleEnum.name());
            setCookie(new String[]{username, Long.toString(expiryTime), roleEnum.name(), "MD5", signatureValue}, lifeHour * 3600, req, resp);
        }
    }

    @Override
    protected boolean rememberMeRequested(HttpServletRequest req, String parameter) {
        log.info("自定义RememberMe处理;");
        if (JsonUtil.isJson(req)) {
            log.info("芝士JSON");
            boolean rememberMe = (boolean) req.getAttribute("rememberMe");
            log.info("记住我处理器收到的参数是:" + rememberMe);
            return rememberMe;
        } else return false;
    }

    @Override
    protected UserDetails processAutoLoginCookie(String[] cookieTokens, HttpServletRequest req, HttpServletResponse resp) throws RememberMeAuthenticationException, UsernameNotFoundException {
        if (cookieTokens.length != 4 && cookieTokens.length != 5) {
            throw new InvalidCookieException("Cookie 字段数不符");
        }
        log.info("收到的cookie" + Arrays.toString(cookieTokens));
        String name = cookieTokens[0];
        long expiryTime;
        try {
            expiryTime = Long.parseLong(cookieTokens[1]);
        } catch (NumberFormatException e) {
            throw new InvalidCookieException("Cookie 时间格式错误");
        }
        // 1) 校验是否过期
        if (expiryTime < System.currentTimeMillis()) {
            throw new InvalidCookieException("Cookie 已过期");
        }
        String role = cookieTokens[2];
        // 2) 先加载用户：active=0（停用/黑名单）会在此抛 UsernameNotFoundException，从而被拦截
        UserDetails user = role.equals(RoleEnum.DOCTOR.name())
                ? doctorDetails.loadUserByUsername(name)
                : clientDetails.loadUserByUsername(name);
        // 3) 校验签名，防止伪造/篡改（签名含密码与 key，密码变更后自动失效，需重新登录）
        String presentedSignature = cookieTokens[cookieTokens.length - 1];
        String expectedSignature = makeTokenSignature(expiryTime, name, user.getPassword(), role);
        if (!expectedSignature.equals(presentedSignature)) {
            throw new InvalidCookieException("Cookie 签名不符");
        }
        return user;
    }

    /***
     * 设置token内容
     */
    private String makeTokenSignature(long expiryTime, String username, String password, String role) {
        String data = username + ":" + expiryTime + ":" + password + ":" + role + ":" + this.getKey();
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return new String(Hex.encode(digest.digest(data.getBytes())));
        } catch (NoSuchAlgorithmException var8) {
            throw new IllegalStateException("Token加密失败");
        }
    }
}
