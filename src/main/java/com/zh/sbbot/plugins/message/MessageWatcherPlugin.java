package com.zh.sbbot.plugins.message;


import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.zh.sbbot.utils.BotHelper;
import com.zh.sbbot.utils.BotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
public class MessageWatcherPlugin {

    private final BotHelper botHelper;

    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {"】", "【"}, endWith = {"】", "【"})
    public void base64(AnyMessageEvent event) {
        String param = BotUtil.base64(ShiroUtils.unescape(event.getMessage()));
        botHelper.reply(event, param, true);
    }

}
