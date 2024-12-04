package com.zh.sbbot.plugin.event;


import com.mikuac.shiro.annotation.GroupMsgDeleteNoticeHandler;
import com.mikuac.shiro.annotation.PrivateMsgDeleteNoticeHandler;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.notice.GroupMsgDeleteNoticeEvent;
import com.mikuac.shiro.dto.event.notice.PrivateMsgDeleteNoticeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 私聊/群聊撤回消息事件
 */
@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class MsgDeleteEvent {


    @GroupMsgDeleteNoticeHandler
    public void onMsgDelete(GroupMsgDeleteNoticeEvent event, Bot bot) {
        log.info("{}撤回了一条消息：{}", event.getUserId(), event);
        if (event.getUserId().equals(bot.getSelfId())) {
            return;
        }
        String msg = MsgUtils.builder().face(269).reply(Math.toIntExact(event.getMessageId())).build();
        bot.sendGroupMsg(event.getGroupId(), msg, false);
    }

    @PrivateMsgDeleteNoticeHandler
    public void onPrivateMsgDelete(PrivateMsgDeleteNoticeEvent event, Bot bot) {
        log.info("{}撤回了一条消息：{}", event.getUserId(), event);
        if (event.getUserId().equals(bot.getSelfId())) {
            return;
        }
        String msg = MsgUtils.builder().face(269).reply(Math.toIntExact(event.getMessageId())).build();
        bot.sendPrivateMsg(event.getUserId(), msg, false);
    }
}
