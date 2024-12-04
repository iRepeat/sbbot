package com.zh.sbbot.plugin.event;

import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.EventUtils;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.response.GetMsgResp;
import com.mikuac.shiro.dto.action.response.GroupMemberInfoResp;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.enums.MsgTypeEnum;
import com.mikuac.shiro.handler.injection.InjectionHandler;
import com.zh.sbbot.constant.DictKey;
import com.zh.sbbot.custom.AnnotationHandlerContainer;
import com.zh.sbbot.custom.event.GroupMsgEmojiLikeNoticeEvent;
import com.zh.sbbot.custom.event.handler.GroupMsgEmojiLikeHandler;
import com.zh.sbbot.repository.DictRepository;
import com.zh.sbbot.util.BotHelper;
import com.zh.sbbot.util.BotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

/**
 * 表情回应事件
 */
@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class GroupEmojiLikeEvent {

    private final DictRepository dictRepository;
    private final BotHelper botHelper;
    private final InjectionHandler injectionHandler;
    private final EventUtils eventUtils;
    @Lazy
    @Autowired
    private AnnotationHandlerContainer annotationHandlerContainer;

    /*
     * 群聊命令重新执行（通过表情回应Bot消息）
     */
    @GroupMsgEmojiLikeHandler
    public void emojiLikeToRegenerate(GroupMsgEmojiLikeNoticeEvent event, Bot bot) {
        String listenId = dictRepository.get(DictKey.PLUGIN_EMOJI_LIKE_LISTENER);

        if (StringUtils.isBlank(listenId)) {
            listenId = "123";
        }

        if (!event.getLikes().get(0).getEmojiId().equals(listenId)) {
            return;
        }

        Optional<String> originMessageIdOpt = Optional.ofNullable(bot.getMsg(event.getMessageId()))
                .map(ActionData::getData)
                .map(GetMsgResp::getRawMessage)
                .map(ShiroUtils::rawToArrayMsg)
                .orElse(Collections.emptyList())
                .stream()
                .filter(it -> it.getType() == MsgTypeEnum.reply)
                .map(arrayMsg -> arrayMsg.getData().get("id"))
                .findFirst();

        if (originMessageIdOpt.isEmpty()) {
            log.warn("bot message has not \"reply\" type message.");
            return;
        }

        GetMsgResp originMessage = originMessageIdOpt
                .map(Integer::parseInt)
                .map(bot::getMsg)
                .map(ActionData::getData)
                .orElse(null);

        if (Objects.isNull(originMessage) || StringUtils.isBlank(originMessage.getRawMessage())) {
            log.warn("origin message cannot be retrieved.");
            return;
        }

        GroupMessageEvent fakeEvent = new GroupMessageEvent();
        fakeEvent.setFont(0);
        fakeEvent.setMessage(originMessage.getRawMessage());
        fakeEvent.setRawMessage(originMessage.getRawMessage());
        fakeEvent.setMessageType("group");
        fakeEvent.setPostType("message");
        fakeEvent.setMessageId(Integer.valueOf(originMessageIdOpt.get()));
        fakeEvent.setSelfId(bot.getSelfId());
        Optional<GroupMemberInfoResp> senderInfo = Optional.ofNullable(bot.getGroupMemberInfo(event.getGroupId(), Long.parseLong(originMessage.getSender().getUserId()),
                false)).map(ActionData::getData);
        GroupMessageEvent.GroupSender sender = new GroupMessageEvent.GroupSender();
        sender.setNickname(senderInfo.map(GroupMemberInfoResp::getNickname).orElse("by plugin: " +
                "emoji-like-to-regenerate"));
        sender.setSex(senderInfo.map(GroupMemberInfoResp::getSex).orElse("unknown"));
        sender.setUserId(Long.valueOf(originMessage.getSender().getUserId()));
        fakeEvent.setSender(sender);
        fakeEvent.setTime(System.currentTimeMillis() / 1000);
        fakeEvent.setUserId(Long.valueOf(originMessage.getSender().getUserId()));
        fakeEvent.setGroupId(event.getGroupId());
        fakeEvent.setArrayMsg(ShiroUtils.rawToArrayMsg(originMessage.getRawMessage()));

        // 执行消息过滤
        if (eventUtils.getInterceptor(bot.getBotMessageEventInterceptor()).preHandle(bot, fakeEvent)) {
            // 强制可执行管理员命令
            if (botHelper.isSuperUser(Long.valueOf(originMessage.getSender().getUserId()))) {
                bot.setAnnotationHandler(annotationHandlerContainer.getAnnotationHandler());
            }
            // 执行消息事件处理
            injectionHandler.invokeAnyMessage(bot, BotUtil.castToAnyMessageEvent(fakeEvent));
            // 群聊事件还要执行群聊消息处理
            injectionHandler.invokeGroupMessage(bot, fakeEvent);
        }

        // 撤回消息
         bot.deleteMsg(event.getMessageId());

    }
}