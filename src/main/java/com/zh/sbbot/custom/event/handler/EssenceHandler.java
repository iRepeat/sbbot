package com.zh.sbbot.custom.event.handler;

import java.lang.annotation.*;

/**
 * 标识对应METHOD为群聊精华消息处理事件处理方法
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EssenceHandler {

}
