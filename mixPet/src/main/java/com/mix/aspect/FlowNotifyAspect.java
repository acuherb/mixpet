package com.mix.aspect;

import com.mix.annotation.FlowChanged;
import com.mix.common.Result;
import com.mix.ws.FlowNotify;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 诊疗流状态变更广播切面：@FlowChanged 方法成功（含 Result.code==1）后推一次 FLOW_REFRESH。
 * 广播失败绝不影响业务结果；前端另有轮询兜底。
 */
@Aspect
@Component
public class FlowNotifyAspect {

    @Around("@annotation(flowChanged)")
    public Object around(ProceedingJoinPoint point, FlowChanged flowChanged) throws Throwable {
        Object result = point.proceed();
        // Result.error / Result.choice(false) 都表示业务未成功，不驱动流转
        if (result instanceof Result r) {
            if (r.code() != null && r.code() == 1) FlowNotify.fire(flowChanged.stage(), flowChanged.text());
        } else {
            // 非 Result 返回（如直接返回实体/void）视为成功
            FlowNotify.fire(flowChanged.stage(), flowChanged.text());
        }
        return result;
    }
}
