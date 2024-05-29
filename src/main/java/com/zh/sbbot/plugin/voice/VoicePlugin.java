package com.zh.sbbot.plugin.voice;

import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.enums.AtEnum;
import com.zh.sbbot.util.BotUtil;
import com.zh.sbbot.util.TTSUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;

@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class VoicePlugin {

    private final TTSUtil ttsUtil;

    @AnyMessageHandler
    @MessageHandlerFilter(startWith = ".tts", at = AtEnum.NOT_NEED)
    public void tts(Bot bot, AnyMessageEvent event, Matcher matcher) {
        String base64 = "base64://" + ttsUtil.generateToBase64(BotUtil.getParam(matcher));
        bot.sendMsg(event, ArrayMsgUtils.builder().voice(base64).build(), false);
    }


}
