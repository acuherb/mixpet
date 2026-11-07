package com.mix.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mix.model.entity.Client;
import com.mix.util.UserUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 后端强制首次改密：顾客使用初始口令时，除登录/改密/公开资源外一律拒绝业务接口。
 * 前端守卫只是 UX，不能作为安全边界。
 */
@Component
public class PasswordChangeInterceptor implements HandlerInterceptor {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> WHITELIST = Set.of(
            "/login", "/cao", "/power", "/common", "/error",
            "/client/password", "/notice/four", "/notice/public", "/notice/detail",
            "/pet/four", "/foster/pet", "/doctor/view", "/setting/hospital");

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        String path = req.getRequestURI();
        for (String allow : WHITELIST) {
            if (path.equals(allow) || path.startsWith(allow + "/")) return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return true;
        if (UserUtil.getUser() instanceof Client client
                && !Boolean.TRUE.equals(client.getClientPwdChanged())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json;charset=UTF-8");
            MAPPER.writeValue(resp.getOutputStream(), Result.error("请先修改初始密码"));
            return false;
        }
        return true;
    }
}
