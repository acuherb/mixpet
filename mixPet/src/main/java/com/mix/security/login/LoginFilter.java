package com.mix.security.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mix.common.LoginAttemptLimiter;
import com.mix.security.model.LoginVo;
import com.mix.service.IAuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * 自定义的登录处理器，让 Security 用 Json 校验密码；同时做失败锁定与计数。
 * 注意：不打印登录请求体（含明文密码）。
 */
@Component
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    private static final Logger log = LoggerFactory.getLogger(LoginFilter.class);
    private static final String ATTEMPT_KEY = "loginAttemptKey";

    private final LoginAttemptLimiter loginAttemptLimiter;

    public LoginFilter(LoginProvider loginProvider, RememberService rememberService,
                       LoginAttemptLimiter loginAttemptLimiter, IAuditLogService auditLogService) {
        super(new ProviderManager(loginProvider));
        this.loginAttemptLimiter = loginAttemptLimiter;
        setFilterProcessesUrl("/cao/toLogin");
        LoginHandler loginHandler = new LoginHandler(auditLogService);
        setAuthenticationSuccessHandler(loginHandler);
        setAuthenticationFailureHandler(loginHandler);
        setRememberMeServices(rememberService);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse resp)
            throws AuthenticationException {
        if (!JsonUtil.isJson(req)) {
            return super.attemptAuthentication(req, resp);
        }
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = req.getInputStream()) {
            LoginVo login = mapper.readValue(is, LoginVo.class);
            if (login == null) throw new LockedException("请求格式错误");
            String key = attemptKey(req, login.username());
            if (loginAttemptLimiter.isBlocked(key)) {
                throw new LockedException("登录失败次数过多，请 15 分钟后重试");
            }
            req.setAttribute(ATTEMPT_KEY, key);
            req.setAttribute("loginUsername", login.username());
            req.setAttribute("rememberMe", login.remember());
            UsernamePasswordAuthenticationToken authRequest =
                    new UsernamePasswordAuthenticationToken(login, login.password());
            setDetails(req, authRequest);
            return this.getAuthenticationManager().authenticate(authRequest);
        } catch (IOException e) {
            log.warn("登录请求 JSON 解析失败");
            UsernamePasswordAuthenticationToken authRequest =
                    new UsernamePasswordAuthenticationToken("", "");
            return this.getAuthenticationManager().authenticate(authRequest);
        }
    }

    /** 按「来源 IP + 账号」计数；不信任 X-Forwarded-For（可被客户端伪造） */
    private String attemptKey(HttpServletRequest req, String username) {
        return req.getRemoteAddr() + "|" + (username == null ? "" : username);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        loginAttemptLimiter.recordFailure((String) request.getAttribute(ATTEMPT_KEY));
        super.unsuccessfulAuthentication(request, response, failed);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult)
            throws IOException, ServletException {
        loginAttemptLimiter.clear((String) request.getAttribute(ATTEMPT_KEY));
        super.successfulAuthentication(request, response, chain, authResult);
    }
}
