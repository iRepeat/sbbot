package com.zh.sbbot.exception;

import com.zh.sbbot.util.BotHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ExceptionAspect {

    private final BotHelper botHelper;

    private static String getStackTrace(Throwable e) {
        return Optional.ofNullable(e)
                .map(Throwable::getStackTrace)
                .flatMap(stackTrace -> Arrays.stream(stackTrace)
                        .filter(stack -> stack.getClassName().startsWith("com.zh.sbbot"))
                        .findFirst())
                .map(stackTraceElement -> String.format("%s.%s(%s:%d)",
                        stackTraceElement.getClassName(),
                        stackTraceElement.getMethodName(),
                        stackTraceElement.getFileName(),
                        stackTraceElement.getLineNumber()))
                .orElseGet(() -> e == null ? "请检查日志" : e.getClass().getSimpleName());
    }

    @AfterThrowing(pointcut = "execution(* com.zh.sbbot.plugin..*(..))", throwing = "e")
    public void handleGenericException(Exception e) {
        String message = "发生异常：\n【%s】\n👇\n【%s】".formatted(getStackTrace(e), e.getLocalizedMessage());
        log.error(message);
        botHelper.sendToDefault(message);
    }
}
