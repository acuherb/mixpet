package com.mix.controller;

import com.mix.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.*;

/**
 * SpringSecurity的控制器
 */
@RestController
@RequestMapping("/power")
@CrossOrigin // 没有意义的允许跨域，因为前端已经配置了
public class PowerController implements ErrorController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * 自定义错误的请求，这里路径在配置文件中配置
     *
     * @param req SpringBoot转发的请求
     * @return 401、404、403、500
     */
    @RequestMapping("/error")
    public Result handleError(HttpServletRequest req) {
        Integer statusCode = (Integer) req.getAttribute("jakarta.servlet.error.status_code");
        log.warn("请求错误码：{}", statusCode);
        return Result.error("请求失败：" + statusCode);
    }
}
