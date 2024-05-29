package com.zh.sbbot.annotation;

import java.lang.annotation.*;

/**
 * 标识对应METHOD为群聊贴表情事件处理方法
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GroupMsgEmojiLikeHandler {

}
