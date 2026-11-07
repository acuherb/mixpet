package com.mix.common;

/**
 * 业务失败异常：用于 @Transactional 方法内部表达失败。
 * 直接 return Result.error 不会触发事务回滚，会留下半截数据，必须改为抛出本异常。
 */
public class BizException extends RuntimeException {
    public BizException(String message) {
        super(message);
    }
}
