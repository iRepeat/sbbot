package com.zh.sbbot.exception;

import com.zh.sbbot.util.BotHelper;
import com.zh.sbbot.util.ExceptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ExceptionAspect {

    private final BotHelper botHelper;

    @AfterThrowing(pointcut = "execution(* com.zh.sbbot.plugin..*(..)) && (" +
            "@within(com.mikuac.shiro.annotation.common.Shiro) || " +
            "@within(org.springframework.web.bind.annotation.RestController))",
            throwing = "e")
    public void handleGenericException(Exception e) {
        String message = "发生异常：\n【%s】\n👇\n【%s】".formatted(ExceptionUtil.getStackTrace(e, "com.zh.sbbot"), e.getLocalizedMessage());
        log.error(message);
        botHelper.sendToDefault(message);
    }
}
