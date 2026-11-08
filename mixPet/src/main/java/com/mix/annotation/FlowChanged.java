package com.mix.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在「会改变诊疗流环节」的业务方法上：方法成功返回后自动向在线员工广播
 * FLOW_REFRESH 事件，驱动三端流水线即时流转（闭环的神经）。
 * <p>
 * 只应打在 controller 方法上——被调 service 的 @Transactional 在返回时已提交，
 * 此刻广播前端再拉取才不会读到旧数据。业务失败（Result.code=0）或抛异常时不广播。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FlowChanged {
    /** 发生变化的环节：wait/treat/result/pay/follow */
    String stage();

    /** 实时动态里展示的提示文案 */
    String text();
}
