package com.zh.sbbot.custom;

import com.mikuac.shiro.common.utils.EventUtils;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotMessageEventInterceptor;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.dto.event.message.MessageEvent;
import com.mikuac.shiro.handler.injection.InjectionHandler;
import com.zh.sbbot.constant.MemberRole;
import com.zh.sbbot.repository.AliasRepository;
import com.zh.sbbot.util.BotHelper;
import com.zh.sbbot.util.BotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * 消息事件拦截器
 * 此拦截器主要作用：1. 实现权限控制 2. 实现命令别名 3. 消息事件拆分
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CustomMessageEventInterceptor implements BotMessageEventInterceptor {
    private final AnnotationHandlerContainer container;
    private final BotHelper botHelper;
    private final AliasRepository aliasRepository;
    private final EventUtils eventUtils;
    private final InjectionHandler injectionHandler;

    private void invokeMessage(String message, Bot bot, Long userId, Long groupId, Integer messageId) {

        boolean isGroup = Objects.nonNull(groupId);

        GroupMessageEvent event = new GroupMessageEvent();
        event.setFont(0);
        event.setMessage(message);
        event.setRawMessage(message);
        event.setMessageType(isGroup ? "group" : "private");
        event.setPostType("message");
        event.setSelfId(bot.getSelfId());
        event.setMessageId(messageId);
        GroupMessageEvent.GroupSender sender = new GroupMessageEvent.GroupSender();
        sender.setNickname("by multi-message execute");
        sender.setSex("unknown");
        sender.setUserId(userId);
        event.setSender(sender);
        event.setTime(System.currentTimeMillis() / 1000);
        event.setUserId(userId);
        event.setGroupId(isGroup ? groupId : 0);
        event.setArrayMsg(ShiroUtils.rawToArrayMsg(message));

        // 执行消息过滤
        if (eventUtils.getInterceptor(bot.getBotMessageEventInterceptor()).preHandle(bot, event)) {
            // 强制可执行管理员命令
            bot.setAnnotationHandler(container.getAnnotationHandler());
            // 执行消息事件处理
            injectionHandler.invokeAnyMessage(bot, BotUtil.castToAnyMessageEvent(event));
            if (isGroup) {
                // 群聊事件还要执行群聊消息处理
                injectionHandler.invokeGroupMessage(bot, event);
            }
        }

    }

    @Override
    public boolean preHandle(Bot bot, MessageEvent event) {
        // 判断是否是命令别名
        String unescaped = ShiroUtils.unescape(event.getRawMessage());
        String value = getMatchingValue(unescaped, event);
        if (!value.startsWith(".alias") && botHelper.isSuperUser(event.getUserId())
                && value.trim().contains("<|>")) {
            // 消息拆分，模拟发送多个消息事件
            for (String message : value.trim().split("<\\|>")) {
                message = message.trim();
                if (StringUtils.isNotBlank(message)) {
                    invokeMessage(
                            message,
                            bot,
                            event.getUserId(),
                            event instanceof GroupMessageEvent groupMessageEvent ? groupMessageEvent.getGroupId() : null,
                            event instanceof GroupMessageEvent groupMessageEvent ? groupMessageEvent.getMessageId() : null
                    );
                }
            }
            // 当前消息事件终止
            return false;
        }
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
        // 超级用户可用的处理方法集合
        else if (botHelper.isSuperUser(event.getUserId())) {
            bot.setAnnotationHandler(container.getAnnotationHandler());
        }
        // 群主或群管理可用的处理方法集合
        else if (event instanceof GroupMessageEvent groupEvent) {
            MemberRole role = botHelper.getMemberRole(event.getSelfId(), event.getUserId(), groupEvent.getGroupId());
            switch (role) {
                case OWNER -> bot.setAnnotationHandler(container.getAnnotationHandlerWithGroupOwner());
                case ADMIN -> bot.setAnnotationHandler(container.getAnnotationHandlerWithGroupAdmin());
                default -> bot.setAnnotationHandler(container.getAnnotationHandlerWithoutAdmin());
            }
        }
        // 普通用户可用的处理方法集合
        else {
            bot.setAnnotationHandler(container.getAnnotationHandlerWithoutAdmin());
        }
        return true;
    }

    private String getMatchingValue(String rawMessage, MessageEvent event) {
        String[] messageParts = Optional.ofNullable(rawMessage)
                .map(String::trim)
                .filter(s -> !s.contains("<|>"))
                .map(s -> s.split("\\$"))
                .orElse(new String[0]);
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
    }


}
