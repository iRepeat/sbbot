package com.zh.sbbot.plugins;

import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.enums.AtEnum;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;

@Shiro
@Component
public class ExamplePlugin {


    @AnyMessageHandler
    @MessageHandlerFilter(startWith = "say", at = AtEnum.NEED)
    public void say(Bot bot, AnyMessageEvent event, Matcher matcher) {
        Optional.ofNullable(matcher.group(2))
                .ifPresent(s -> bot.sendMsg(event, s.trim(), false));
    }
}