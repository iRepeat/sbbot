package com.zh.sbbot.interceptor;

import com.alibaba.fastjson2.JSONObject;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotMessageEventInterceptor;
import com.mikuac.shiro.dto.event.message.MessageEvent;
import com.zh.sbbot.configs.SystemSetting;
import com.zh.sbbot.repository.AliasRepository;
import com.zh.sbbot.utils.AnnotationHandlerContainer;
import com.zh.sbbot.utils.BotHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 此拦截器主要两个作用：1. 实现权限控制 2. 实现全局bot开关 3. 实现命令别名
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CustomInterceptor implements BotMessageEventInterceptor {
    private final AnnotationHandlerContainer container;
    private final SystemSetting systemSetting;
    private final BotHelper botHelper;
    private final AliasRepository aliasRepository;


    @Override
    public boolean preHandle(Bot bot, MessageEvent event) {
        // 判断是否是命令别名
        String unescaped = ShiroUtils.unescape(event.getRawMessage());
        String value = getMatchingValue(unescaped);
        if (StringUtils.isNotBlank(value)) {
            log.info("replace: [{}] => [{}] ", unescaped, value);
            event.setRawMessage(value);
            event.setMessage(JSONObject.toJSONString(ShiroUtils.rawToArrayMsg(value)));
            // 别名命令不检查管理员权限
            bot.setAnnotationHandler(container.getAnnotationHandler());
        }
        // 根据不同的用户权限设定不同的处理方法集合
        else if (botHelper.isSuperUser(event.getUserId())) {
            bot.setAnnotationHandler(container.getAnnotationHandler());
        } else {
            bot.setAnnotationHandler(container.getAnnotationHandlerWithoutAdmin());
        }
        return List.of(".up", ".down").contains(event.getRawMessage()) || systemSetting.isEnable();
    }

    private String getMatchingValue(String rawMessage) {
        String[] messageParts = Optional.ofNullable(rawMessage).map(s -> s.trim().split("\\$")).orElse(new String[0]);
        if (messageParts.length == 0) {
            return null;
        }
        String value = aliasRepository.get(messageParts[0].trim());
        if (value == null) {
            return null;
        }
        if (StringUtils.countMatches(value, "$") != messageParts.length - 1) {
            log.info("matched alias: [{}] but parameter size is not equal, message: [{}]", value, rawMessage);
            return null;
        }
        for (int i = 1; i < messageParts.length; i++) {
            value = value.replaceFirst("\\$", messageParts[i]);
        }
        return value;
    }

    @Override
    public void afterCompletion(Bot bot, MessageEvent event) {
    }

}
