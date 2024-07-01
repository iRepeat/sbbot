package com.zh.sbbot.plugins.message;


import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.model.ArrayMsg;
import com.zh.sbbot.utils.BotHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static com.mikuac.shiro.enums.MsgTypeEnum.*;

@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
public class MessageRepeatPlugin {

    /*
     * 群聊消息随机复读
     */
    @GroupMessageHandler
    public void repeatRandom(GroupMessageEvent event, Bot bot) {
        int i = RandomUtils.nextInt();
        if (i % 10 == 0) {
            bot.sendGroupMsg(event.getGroupId(), event.getArrayMsg(), false);
        }
    }

    /*
     * 单表情复读
     */
    @GroupMessageHandler
    public void repeatEmoji(GroupMessageEvent event, Bot bot) {
        List<ArrayMsg> arrayMsg = event.getArrayMsg();
        if (arrayMsg.size() == 1 && Arrays.asList(face, dice, new_dice, basketball, rps, new_rps, mface, marketface, at).contains(arrayMsg.get(0).getType())) {
            bot.sendGroupMsg(event.getGroupId(), event.getArrayMsg(), false);
        }
    }
}
