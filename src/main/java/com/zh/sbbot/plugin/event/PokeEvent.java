package com.zh.sbbot.plugin.event;

import com.alibaba.fastjson2.JSONObject;
import com.mikuac.shiro.annotation.GroupPokeNoticeHandler;
import com.mikuac.shiro.annotation.PrivatePokeNoticeHandler;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.notice.PokeNoticeEvent;
import com.zh.sbbot.plugin.ai.dao.PluginAi;
import com.zh.sbbot.plugin.ai.handler.AiHandler;
import com.zh.sbbot.plugin.ai.handler.AiHandlerSelector;
import com.zh.sbbot.plugin.ai.support.ChatResponse;
import com.zh.sbbot.util.BotHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class PokeEvent {
    private final BotHelper botHelper;
    private final AiHandlerSelector aiHandlerSelector;

    @PrivatePokeNoticeHandler
    @GroupPokeNoticeHandler
    public void poke(PokeNoticeEvent event, Bot bot) {
        log.info("received a poke: {}", JSONObject.toJSONString(event));
        boolean isPokeMe = Objects.equals(event.getTargetId(), event.getSelfId());
        if (isPokeMe) {
            MsgUtils msgBuilder = MsgUtils.builder().text(getAiResponse());
            if (event.getGroupId() != null) {
                bot.sendGroupMsg(event.getGroupId(), msgBuilder.at(event.getUserId()).build(), false);
            } else {
                bot.sendPrivateMsg(event.getUserId(), msgBuilder.build(), false);
            }
        } else {
            if (event.getGroupId() != null && !Objects.equals(event.getUserId(), event.getSelfId())) {
                botHelper.sendGroupPoke(event.getGroupId(), event.getTargetId());
            }
        }
    }

    private String getAiResponse() {
        AiHandler ai = aiHandlerSelector.getDefault();
        PluginAi option = PluginAi.defaultConfig(null, ai.vendor(), ai.defaultModel());
        option.setSystemTemplate("你的脾气非常暴躁易怒");
        option.setTemperature("0.9");
        ChatResponse chatResponse = ai.generateAnswer(option, "你拍你爹爹呢，相似的话有哪些？直接告诉我你的答案，不要做解释不要带引号");
        return chatResponse.getResult();
    }
}
