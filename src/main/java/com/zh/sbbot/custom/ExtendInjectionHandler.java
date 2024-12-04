package com.zh.sbbot.custom;

import com.mikuac.shiro.common.utils.InternalUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.handler.injection.InjectionHandler;
import com.mikuac.shiro.model.HandlerMethod;
import com.zh.sbbot.custom.event.EssenceNoticeEvent;
import com.zh.sbbot.custom.event.handler.EssenceHandler;
import com.zh.sbbot.custom.event.handler.GroupMsgEmojiLikeHandler;
import com.zh.sbbot.custom.event.GroupMsgEmojiLikeNoticeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * 拓展事件执行器
 */
@Component
@Primary
@Slf4j
public class ExtendInjectionHandler extends InjectionHandler {
    @Lazy
    @Autowired
    private AnnotationHandlerContainer annotationHandlerContainer;


    /**
     * 核心逻辑来自于：{@linkplain InjectionHandler}
     */
    private void invokeMethod(HandlerMethod method, Map<Class<?>, Object> params) {
        Class<?>[] types = method.getMethod().getParameterTypes();
        Object[] objects = new Object[types.length];
        Arrays.stream(types).forEach(InternalUtils.consumerWithIndex((item, index) -> {
            if (params.containsKey(item)) {
                objects[index] = params.remove(item);
                return;
            }
            objects[index] = null;
        }));
        try {
            method.getMethod().invoke(method.getObject(), objects);
        } catch (Exception e) {
            log.error("Invoke method exception: {}", e.getMessage(), e);
        }
    }

    /**
     * 核心逻辑来自于：{@linkplain InjectionHandler}
     */
    private <T> void invoke(Bot bot, T event, Class<? extends Annotation> type) {
        Optional<List<HandlerMethod>> methods = Optional.ofNullable(annotationHandlerContainer.getAnnotationHandler().get(type));
        if (methods.isEmpty()) {
            return;
        }
        Map<Class<?>, Object> params = new HashMap<>();
        params.put(Bot.class, bot);
        params.put(event.getClass(), event);
        methods.get().forEach(method -> invokeMethod(method, params));
    }

    /**
     * 拓展事件执行 - 群聊贴表情事件
     */
    public void invokeGroupMsgEmojiLike(Bot bot, GroupMsgEmojiLikeNoticeEvent event) {
        this.invoke(bot, event, GroupMsgEmojiLikeHandler.class);
    }

    /**
     * 拓展事件执行 - 群聊精华消息操作事件
     */
    public void invokeGroupMsgEmojiLike(Bot bot, EssenceNoticeEvent event) {
        this.invoke(bot, event, EssenceHandler.class);
    }

}
