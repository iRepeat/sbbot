package com.zh.sbbot.plugin.event;


import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.EventUtils;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.response.GetMsgResp;
import com.mikuac.shiro.dto.action.response.GroupMemberInfoResp;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.handler.injection.InjectionHandler;
import com.zh.sbbot.custom.AnnotationHandlerContainer;
import com.zh.sbbot.custom.event.EssenceNoticeEvent;
import com.zh.sbbot.custom.event.handler.EssenceHandler;
import com.zh.sbbot.repository.DictRepository;
import com.zh.sbbot.util.BotHelper;
import com.zh.sbbot.util.BotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 群聊精华消息操作事件
 */
@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class EssenceEvent {

    private final DictRepository dictRepository;
    private final BotHelper botHelper;
    private final InjectionHandler injectionHandler;
    private final EventUtils eventUtils;
    @Lazy
    @Autowired
    private AnnotationHandlerContainer annotationHandlerContainer;

    /*
     * 群聊命令一键执行（通过消息加精）
     */
    @EssenceHandler
    public void essenceAddToExecute(EssenceNoticeEvent event, Bot bot) {
        if (!"add".equals(event.getSubType())) {
            return;
        }

        String originMessage = Optional.ofNullable(event.getMessageId())
                .map(bot::getMsg)
                .map(ActionData::getData)
                .map(GetMsgResp::getRawMessage)
                .orElse(StringUtils.EMPTY);

        if (StringUtils.isBlank(originMessage)) {
            log.warn("origin message cannot be retrieved.");
            return;
        }

        GroupMessageEvent fakeEvent = new GroupMessageEvent();
        fakeEvent.setFont(0);
        fakeEvent.setMessage(originMessage);
        fakeEvent.setRawMessage(originMessage);
        fakeEvent.setMessageType("group");
        fakeEvent.setPostType("message");
        fakeEvent.setMessageId(event.getMessageId());
        fakeEvent.setSelfId(bot.getSelfId());
        Optional<GroupMemberInfoResp> senderInfo = Optional.ofNullable(bot.getGroupMemberInfo(event.getGroupId(),
                event.getOperatorId(), false)).map(ActionData::getData);
        GroupMessageEvent.GroupSender sender = new GroupMessageEvent.GroupSender();
        sender.setNickname(senderInfo.map(GroupMemberInfoResp::getNickname).orElse("by plugin: " +
                "essence-add-to-execute"));
        sender.setSex(senderInfo.map(GroupMemberInfoResp::getSex).orElse("unknown"));
        sender.setUserId(event.getOperatorId());
        fakeEvent.setSender(sender);
        fakeEvent.setTime(System.currentTimeMillis() / 1000);
        fakeEvent.setUserId(event.getOperatorId());
        fakeEvent.setGroupId(event.getGroupId());
        fakeEvent.setArrayMsg(ShiroUtils.rawToArrayMsg(originMessage));

        // 执行消息过滤
        if (eventUtils.getInterceptor(bot.getBotMessageEventInterceptor()).preHandle(bot, fakeEvent)) {
            // 强制可执行管理员命令
            if (botHelper.isSuperUser(event.getOperatorId())) {
                bot.setAnnotationHandler(annotationHandlerContainer.getAnnotationHandler());
            }
            // 执行消息事件处理
            injectionHandler.invokeAnyMessage(bot, BotUtil.castToAnyMessageEvent(fakeEvent));
            // 群聊事件还要执行群聊消息处理
            injectionHandler.invokeGroupMessage(bot, fakeEvent);
        }

        // 移除消息精华
         bot.deleteEssenceMsg(event.getMessageId());

    }

}
