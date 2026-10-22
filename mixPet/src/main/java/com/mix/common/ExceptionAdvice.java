package com.mix.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;


@RestControllerAdvice
public class ExceptionAdvice {
    private static final Logger log = LoggerFactory.getLogger(ExceptionAdvice.class);

    @ExceptionHandler(DuplicateKeyException.class)
    public Result doDuplicateKeyException() {
        return Result.error("不能重复插入");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result doDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.warn("数据完整性违规异常，可能空值插入或插入过长==>" + e);
        return Result.error("数据完整性违约");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result doAccessDeniedException() {
        return ResultConst.NO_POWER;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result doMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        if (msg.isEmpty()) msg = "参数校验失败";
        return Result.error(msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result doConstraintViolationException(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        if (msg.isEmpty()) msg = "参数校验失败";
        return Result.error(msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result doMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        return Result.error("缺少必需参数: " + e.getParameterName());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result doIllegalArgumentException(IllegalArgumentException e) {
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result doOtherException(Exception ex) {
        log.error("系统未预期异常", ex);
        return Result.error("系统繁忙，请稍后再试！");
    }
}