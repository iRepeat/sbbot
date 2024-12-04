package com.zh.sbbot.custom;

import com.alibaba.fastjson2.JSONObject;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.handler.event.NoticeEvent;
import com.zh.sbbot.constant.ExtendEventType;
import com.zh.sbbot.custom.event.EssenceNoticeEvent;
import com.zh.sbbot.custom.event.GroupMsgEmojiLikeNoticeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 拓展事件分发
 */
@Component
@RequiredArgsConstructor
public class ExtendEventHandler implements ApplicationRunner {

    private final NoticeEvent notice;
    private final ExtendInjectionHandler extendInjectionHandler;


    @Override
    public void run(ApplicationArguments args) {
        // 群聊贴表情事件
        notice.handlers.put(ExtendEventType.Notice.GROUP_MSG_EMOJI_LIKE, this::groupMsgEmojiLike);
        // 群聊精华消息操作事件
        notice.handlers.put(ExtendEventType.Notice.ESSENCE, this::essence);
    }

    /**
     * 群聊贴表情事件处理
     */
    public void groupMsgEmojiLike(Bot bot, JSONObject resp) {
        GroupMsgEmojiLikeNoticeEvent event = resp.to(GroupMsgEmojiLikeNoticeEvent.class);
        extendInjectionHandler.invokeGroupMsgEmojiLike(bot, event);
    }


    /**
     * 群聊精华消息操作事件处理
     */
    public void essence(Bot bot, JSONObject resp) {
        EssenceNoticeEvent event = resp.to(EssenceNoticeEvent.class);
        extendInjectionHandler.invokeGroupMsgEmojiLike(bot, event);
    }

}
