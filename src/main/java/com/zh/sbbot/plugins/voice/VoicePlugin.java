package com.zh.sbbot.plugins.voice;

import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.enums.AtEnum;
import com.zh.sbbot.utils.BotUtil;
import com.zh.sbbot.utils.TTSUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    @MessageHandlerFilter(startWith = ".dz", at = AtEnum.NOT_NEED)
    public void dz(Bot bot, AnyMessageEvent event, Matcher matcher) {
        String url = "http://ovoa.cc/api/dingzhen.php?message=" + BotUtil.getParam(matcher);
        bot.sendMsg(event, ArrayMsgUtils.builder().voice(url).build(), false);
    }

    @AnyMessageHandler
    @MessageHandlerFilter(startWith = ".tts", at = AtEnum.NOT_NEED)
    public void tts(Bot bot, AnyMessageEvent event, Matcher matcher) {
        String base64 = "base64://" + ttsUtil.generateToBase64(BotUtil.getParam(matcher));
        bot.sendMsg(event, ArrayMsgUtils.builder().voice(base64).build(), false);
    }


}
