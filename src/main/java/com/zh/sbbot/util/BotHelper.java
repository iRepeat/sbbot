package com.zh.sbbot.util;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.response.GroupMemberInfoResp;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.zh.sbbot.config.SystemSetting;
import com.zh.sbbot.constant.DictKey;
import com.zh.sbbot.constant.MemberRole;
import com.zh.sbbot.repository.DictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 结合了spring上下文的bot工具类
 */
@Component
@RequiredArgsConstructor
public class BotHelper {

    private final SystemSetting systemSetting;
    private final BotContainer botContainer;
    private final BotIdHolder botIdHolder;
    private final DictRepository dictRepository;

    /**
     * 判断是否是超级用户
     *
     * @param userId 用户id
     * @return 是否是超级用户
     */
    public boolean isSuperUser(Long userId) {
        return Arrays.asList(systemSetting.getSuperUser()).contains(userId);
    }

    /**
     * 检查是否是群聊/用户黑名单
     */
    public boolean isBlock(Long userOrGroupId) {
        if (Objects.isNull(userOrGroupId)) {
            return false;
        }
        Set<Long> ids = dictRepository.get(DictKey.SYSTEM_BLOCK_IDS, new TypeReference<>() {
        });
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }
        return ids.contains(userOrGroupId);
    }


    /**
     * 获取群成员类型
     */
    public MemberRole getMemberRole(Long botId, Long userId, Long groupId) {
        if (userId == null) return MemberRole.OTHER;
        ActionData<GroupMemberInfoResp> memberInfo = getBot(botId).getGroupMemberInfo(groupId, userId, true);
        return Optional.ofNullable(memberInfo).map(ActionData::getData).
                map(GroupMemberInfoResp::getRole).map(MemberRole::of).orElse(MemberRole.OTHER);
    }

    /**
     * 快速回复
     *
     * @param event      消息事件
     * @param text       回复内容
     * @param autoEscape 是否解析CQ码
     */
    public void reply(AnyMessageEvent event, String text, boolean autoEscape) {
        // 创建MsgUtils实例，根据是否有消息ID来决定是否需要reply
        MsgUtils msgUtils = MsgUtils.builder();
        if (!autoEscape && event.getMessageId() != null) {
            msgUtils.reply(event.getMessageId());
        }

        // 构建消息内容，根据是否有用户ID来决定是否需要@用户
        msgUtils.text(text);
        if (!autoEscape && event.getSender().getUserId() != null) {
            msgUtils.text("\n").at(event.getSender().getUserId());
        }
        String msg = msgUtils.build();

        // 发送群消息
        getBot().sendMsg(event, msg, autoEscape);
    }

    /**
     * 快速回复，解析CQ码
     *
     * @param event 消息事件
     * @param text  回复内容
     */
    public void reply(AnyMessageEvent event, String text) {
        reply(event, text, false);
    }

    /**
     * 群消息快速回复
     *
     * @param event      消息事件
     * @param text       回复内容
     * @param autoEscape 是否解析CQ码
     */
    public void reply(GroupMessageEvent event, String text, boolean autoEscape) {
        AnyMessageEvent anyMessageEvent = BotUtil.castToAnyMessageEvent(event);
        reply(anyMessageEvent, text, autoEscape);
    }

    /**
     * 群消息快速回复，解析CQ码
     *
     * @param event 消息事件
     * @param text  回复内容
     */
    public void reply(GroupMessageEvent event, String text) {
        reply(event, text, false);
    }

    /**
     * 发送消息给默认用户（{@link  DictKey#SYSTEM_EVENT_RECEIVE_USER}）
     */
    public void sendToDefault(String text) {
        Optional.ofNullable(dictRepository.get(DictKey.SYSTEM_EVENT_RECEIVE_USER, Long.class))
                .ifPresent(userId -> getBot().sendPrivateMsg(userId, text, false));
    }

    /**
     * 拓展api-设置头像
     */
    public void setSelfAvatar(String file) {
        Map<String, Object> params = new HashMap<>();
        params.put("file", file);
        getBot().customRequest(() -> "set_qq_avatar", params);
    }


    /**
     * 拓展api-设置群聊头像
     */
    public void setGroupAvatar(String file, Long groupCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("file", file);
        params.put("group_id", groupCode);
        getBot().customRequest(() -> "set_group_portrait", params);
    }


    /**
     * 拓展api-发送表情回应
     * <a href="https://bot.q.qq.com/wiki/develop/api-v2/openapi/emoji/model.html#EmojiType">see</a>
     */
    public void sendEmojiLike(String messageId, String emojiId) {
        Map<String, Object> params = new HashMap<>();
        params.put("message_id", messageId);
        params.put("emoji_id", emojiId);
        getBot().customRequest(() -> "set_msg_emoji_like", params);
    }

    /**
     * 拓展api-发送群聊poke
     */
    public void sendGroupPoke(Long groupId, Long userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("group_id", groupId);
        params.put("user_id", userId);
        getBot().customRequest(() -> "group_poke", params);
    }

    /**
     * 拓展api-发送群聊AI语音
     */
    public void sendGroupAiRecord(Long groupId, String character, String text) {
        Map<String, Object> params = new HashMap<>();
        params.put("group_id", groupId);
        params.put("character", character);
        params.put("text", text);
        getBot().customRequest(() -> "send_group_ai_record", params);
    }
    /**
     * 拓展api-获取AI语音链接
     */
    public String getAiRecord(Long groupId, String character, String text) {
        Map<String, Object> params = new HashMap<>();
        params.put("group_id", groupId);
        params.put("character", character);
        params.put("text", text);
        ActionData<?> actionData = getBot().customRequest(() -> "get_ai_record", params);
        return Optional.ofNullable(actionData).map(ActionData::getData).map(Object::toString).orElse(null);
    }

    /**
     * 自定义拓展api
     */
    public ActionData<?> sendCustomRequest(String apiName, String json) {
        return getBot().customRequest(() -> apiName, JSONObject.parse(json));

    }

    /**
     * 获取默认bot
     */
    public Bot defaultBot() {
        return getBot(systemSetting.getDefaultBot());
    }

    /**
     * 获取当前上下文bot。
     * 如果上下文bot不存在（如异步线程中），则返回默认bot
     */
    public Bot getBot() {
        Bot bot = getBot(botIdHolder.getBotId());
        return bot == null ? defaultBot() : bot;
    }


    /**
     * 根据Id获取bot
     */
    public Bot getBot(Long botId) {
        return botId == null ? null : botContainer.robots.get(botId);
    }

}
