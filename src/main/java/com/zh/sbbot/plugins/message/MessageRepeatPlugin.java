package com.zh.sbbot.plugins.message;


import com.alibaba.fastjson2.JSONObject;
import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.enums.MsgTypeEnum;
import com.mikuac.shiro.model.ArrayMsg;
import com.zh.sbbot.constant.DictKey;
import com.zh.sbbot.repository.DictRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static com.mikuac.shiro.enums.MsgTypeEnum.*;

@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
public class MessageRepeatPlugin {

    private final DictRepository dictRepository;

    /*
     * 群聊消息随机复读
     */
    @GroupMessageHandler
    public void repeatRandom(GroupMessageEvent event, Bot bot) {
        JSONObject map = dictRepository.get(DictKey.PLUGIN_REPEAT_MSG_RULE, JSONObject.class);
        List<ArrayMsg> msg = event.getArrayMsg();
        for (int i = msg.size() - 1; i >= 0; i--) {
            ArrayMsg arrayMsg = msg.get(i);
            if (arrayMsg.getType().equals(text)) {
                for (String word : map.keySet()) {
                    if (arrayMsg.getData().get("text").endsWith(word)) {
                        msg.addAll(ArrayMsgUtils.builder().text(map.get(word).toString()).build());
                        bot.sendGroupMsg(event.getGroupId(), msg, false);
                        return;
                    }
                }
            }
        }
    }

    /*
     * 单表情复读
     */
    @GroupMessageHandler
    public void repeatEmoji(GroupMessageEvent event, Bot bot) {
        List<MsgTypeEnum> typeEnums = event.getArrayMsg().stream().map(ArrayMsg::getType).distinct().toList();
        if (typeEnums.size() == 1 && Arrays.asList(face, dice, new_dice, basketball, rps, new_rps, mface, marketface, at)
                .contains(typeEnums.get(0))) {
            bot.sendGroupMsg(event.getGroupId(), event.getArrayMsg(), false);
        }
    }
}
