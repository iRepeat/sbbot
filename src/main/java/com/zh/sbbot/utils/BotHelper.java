package com.zh.sbbot.utils;

import com.google.common.collect.Maps;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.zh.sbbot.configs.SystemSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/**
 * 结合了spring上下文的bot工具类
 */
@Component
@RequiredArgsConstructor
public class BotHelper {

    private final SystemSetting systemSetting;
    private final BotContainer botContainer;

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
     * 默认bot
     */
    public Bot defaultBot() {
        return botContainer.robots.get(systemSetting.getDefaultBot());
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
        defaultBot().sendMsg(event, msg, autoEscape);
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
     * 拓展api-设置头像
     */
    public void setSelfAvatar(String url) {
        Map<String, Object> params = Maps.newHashMap();
        params.put("file", url);
        defaultBot().customRequest(() -> "set_qq_avatar", params);
    }

}
