package com.zh.sbbot.utils;

import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.zh.sbbot.configs.SystemSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
        return systemSetting.getSuperUser().contains(userId);
    }

    /**
     * 快速回复（文本）
     *
     * @param event 消息事件
     * @param text  回复内容
     */
    public void reply(AnyMessageEvent event, String text) {
        String msg = MsgUtils.builder()
                .reply(event.getMessageId())
                .text(text)
                .text("\n")
                .at(event.getUserId())
                .build();
        Bot bot = botContainer.robots.get(event.getSelfId());
        bot.sendMsg(event, msg, false);
    }


    /**
     * 群消息快速回复（文本）
     *
     * @param event      消息事件
     * @param text       回复内容
     */
    public void replyForGroup(GroupMessageEvent event, String text) {
        String msg = MsgUtils.builder()
                .reply(event.getMessageId())
                .text(text)
                .text("\n")
                .at(event.getUserId())
                .build();
        Bot bot = botContainer.robots.get(event.getSelfId());
        bot.sendGroupMsg(event.getGroupId(), event.getSender().getUserId(), msg, false);
    }
}
