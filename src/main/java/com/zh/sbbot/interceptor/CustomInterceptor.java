package com.zh.sbbot.interceptor;

import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotMessageEventInterceptor;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.dto.event.message.MessageEvent;
import com.zh.sbbot.configs.SystemSetting;
import com.zh.sbbot.repository.AliasRepository;
import com.zh.sbbot.utils.AnnotationHandlerContainer;
import com.zh.sbbot.utils.BotHelper;
import com.zh.sbbot.utils.BotIdHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 此拦截器主要作用：1. 实现权限控制 2. 实现全局bot开关 3. 实现命令别名 4. 存储botId到上下文
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CustomInterceptor implements BotMessageEventInterceptor {
    private final AnnotationHandlerContainer container;
    private final SystemSetting systemSetting;
    private final BotHelper botHelper;
    private final AliasRepository aliasRepository;
    private final BotIdHolder botHolder;


    @Override
    public boolean preHandle(Bot bot, MessageEvent event) {
        // 判断是否是命令别名
        String unescaped = ShiroUtils.unescape(event.getRawMessage());
        String value = getMatchingValue(unescaped, event);
        if (!Objects.equals(value, unescaped)) {
            log.info("replace: [{}] => [{}] ", unescaped, value);
            if (event.getRawMessage() != null) {
                event.setRawMessage(value);
            }
            if (event.getMessage() != null) {
                event.setMessage(value);
            }
            if (event.getArrayMsg() != null) {
                event.setArrayMsg(ShiroUtils.rawToArrayMsg(value));
            }
            // 别名命令不检查管理员权限
            bot.setAnnotationHandler(container.getAnnotationHandler());
        }
        // 根据不同的用户权限设定不同的处理方法集合
        else if (botHelper.isSuperUser(event.getUserId())) {
            bot.setAnnotationHandler(container.getAnnotationHandler());
        } else {
            bot.setAnnotationHandler(container.getAnnotationHandlerWithoutAdmin());
        }
        boolean b = List.of(".up", ".down").contains(event.getRawMessage()) || systemSetting.isEnable();
        if (b) botHolder.setBotId(event.getSelfId());
        return b;
    }

    private String getMatchingValue(String rawMessage, MessageEvent event) {
        String[] messageParts = Optional.ofNullable(rawMessage).map(s -> s.trim().split("\\$")).orElse(new String[0]);
        if (messageParts.length == 0) {
            return rawMessage;
        }
        String value = aliasRepository.get(messageParts[0].trim());
        if (value == null) {
            return rawMessage;
        }
        if (StringUtils.countMatches(value, "【参数】") > messageParts.length - 1) {
            log.info("matched alias: [{}] but missing parameters, message: [{}]", value, rawMessage);
            return rawMessage;
        }
        StringBuilder valueBuilder = new StringBuilder(value);
        for (int i = 1; i < messageParts.length; i++) {
            if (!valueBuilder.toString().contains("【参数】")) {
                // 将messageParts[i](包含i)之后的字符串与value拼接
                valueBuilder.append("$").append(messageParts[i]);
            } else {
                valueBuilder = new StringBuilder(valueBuilder.toString().replaceFirst("【参数】", messageParts[i]));
            }
        }
        value = String.valueOf(valueBuilder);
        value = value.replace("【group】", event instanceof GroupMessageEvent ?
                ((GroupMessageEvent) event).getGroupId().toString() : "0");
        value = value.replace("【user】", event.getUserId() != null ?
                event.getUserId().toString() : "0");
        value = value.replace("【self】", event.getSelfId() != null ?
                event.getSelfId().toString() : "0");
        return getMatchingValue(value, event);
    }

    @Override
    public void afterCompletion(Bot bot, MessageEvent event) {
        botHolder.clear();
    }

}
