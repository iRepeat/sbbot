package com.zh.sbbot.custom;


import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.Event;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class InjectionHandlerAdvice {

    private final BaseEventInterceptor baseEventInterceptor;

    @Pointcut("execution( * com.mikuac.shiro.handler.injection.InjectionHandler.invoke*(..)) " +
            "|| execution( * com.zh.sbbot.custom.ExtendInjectionHandler.invoke*(..))")
    private void invokeMethods() {
    }

    /**
     * 排除心跳检测事件
     */
    @Pointcut(" execution(public * com.mikuac.shiro.handler.injection.InjectionHandler.invokeHeartbeat(..))")
    private void excludedMethods() {
    }

    /**
     * 注入全局拦截器
     */
    @Around("invokeMethods() && !excludedMethods()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Object result = null;
        if (baseEventInterceptor.preHandle((Bot) args[0], (Event) args[1])) {
            result = joinPoint.proceed();
            baseEventInterceptor.afterCompletion((Bot) args[0], (Event) args[1]);
        }
        return result;
    }
}
