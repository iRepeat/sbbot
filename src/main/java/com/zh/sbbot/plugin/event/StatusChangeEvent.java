package com.zh.sbbot.plugin.event;

import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.CoreEvent;
import com.zh.sbbot.util.BotHelper;
import com.zh.sbbot.util.BotIdHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Primary
@Component
@Slf4j
@RequiredArgsConstructor
public class StatusChangeEvent extends CoreEvent {
    private final BotHelper botHelper;
    private final BotIdHolder botIdHolder;

    @Override
    public void online(Bot bot) {
        log.info("bot已上线：{}", bot.getSelfId());
        botIdHolder.setBotId(bot.getSelfId());
        botHelper.sendToDefault("我上线啦～");
        botIdHolder.clear();
    }

    @Override
    public void offline(long account) {
        log.info("bot已下线：{}", account);
        botHelper.sendToDefault("我下线啦～");
    }

    @Override
    public boolean session(WebSocketSession session) {
        // 返回 false 即可禁止连接
        return true;
    }

}
