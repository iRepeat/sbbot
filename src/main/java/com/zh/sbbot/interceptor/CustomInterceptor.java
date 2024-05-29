package com.zh.sbbot.interceptor;

import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotMessageEventInterceptor;
import com.mikuac.shiro.dto.event.message.MessageEvent;
import com.zh.sbbot.configs.SystemSetting;
import com.zh.sbbot.utils.AnnotationHandlerContainer;
import com.zh.sbbot.utils.BotHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 此拦截器主要两个作用：1. 实现权限控制 2. 实现全局bot开关
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CustomInterceptor implements BotMessageEventInterceptor {
    private final AnnotationHandlerContainer container;
    private final SystemSetting systemSetting;
    private final BotHelper botHelper;


    @Override
    public boolean preHandle(Bot bot, MessageEvent event) {
        // 根据不同的用户权限设定不同的处理方法集合
        if (botHelper.isSuperUser(event.getUserId())) {
            bot.setAnnotationHandler(container.getAnnotationHandler());
        } else {
            bot.setAnnotationHandler(container.getAnnotationHandlerWithoutAdmin());
        }
        return List.of(".up", ".down").contains(event.getRawMessage()) || systemSetting.isEnable();
    }

    @Override
    public void afterCompletion(Bot bot, MessageEvent event) {
    }

}
