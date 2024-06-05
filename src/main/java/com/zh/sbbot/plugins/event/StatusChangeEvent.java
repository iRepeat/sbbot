package com.zh.sbbot.plugins.event;

import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.CoreEvent;
import com.zh.sbbot.constant.DictKey;
import com.zh.sbbot.repository.DictRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

@Primary
@Component
@Slf4j
@RequiredArgsConstructor
public class StatusChangeEvent extends CoreEvent {
    private final DictRepository dictRepository;

    @Override
    public void online(Bot bot) {
        log.info("bot已上线：{}", bot.getSelfId());
        Optional.ofNullable(dictRepository.getValue(DictKey.PLUGIN_EVENT_RECEIVE_USER, Long.class))
                .ifPresent(userId -> bot.sendPrivateMsg(userId, "我上线啦～", false));
    }

    @Override
    public void offline(long account) {
        log.info("bot已下线：{}", account);
    }

    @Override
    public boolean session(WebSocketSession session) {
        // 返回 false 即可禁止连接
        return true;
    }

}
