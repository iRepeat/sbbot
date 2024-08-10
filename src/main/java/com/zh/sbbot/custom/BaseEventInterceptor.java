package com.zh.sbbot.custom;

import com.alibaba.fastjson2.JSONObject;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.Event;
import com.mikuac.shiro.dto.event.message.MessageEvent;
import com.zh.sbbot.config.SystemSetting;
import com.zh.sbbot.util.BotIdHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 全局事件拦截器
 * 主要作用：1. 全局开关 2. 存储BotId上下文 3. 记录消息日志
 */
@Component
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class BaseEventInterceptor {

    private final BotIdHolder botHolder;
    private final SystemSetting systemSetting;

    /**
     * 预处理
     *
     * @param bot   {@link Bot}
     * @param event {@link Event}
     * @return true 为执行 false 为拦截 拦截后不再传递给 plugin
     */
    public boolean preHandle(Bot bot, Event event) {
        boolean isPass = event instanceof MessageEvent msgEvent ?
                List.of(".up", ".down").contains(msgEvent.getRawMessage()) || systemSetting.isEnable()
                : systemSetting.isEnable();
        if (isPass) {
            botHolder.setBotId(event.getSelfId());
        }
        log.debug("【{}】enable: {}, bot: {}, event: {}", event.getClass().getSimpleName(), isPass, event.getSelfId(),
                JSONObject.toJSONString(event));
        return isPass;
    }

    /**
     * 执行后
     *
     * @param bot   {@link Bot}
     * @param event {@link Event}
     */
    public void afterCompletion(Bot bot, Event event) {
        log.debug("【{}】finished!", event.getClass().getSimpleName());
        botHolder.clear();
    }

}
