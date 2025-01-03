package com.zh.sbbot.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * botId上下文
 */
@Component
@RequiredArgsConstructor
public class BotIdHolder {
    private final ThreadLocal<Long> botId = new ThreadLocal<>();


    public Long getBotId() {
        return this.botId.get();
    }

    public void setBotId(Long botId) {
        if (botId == null) {
            return;
        }
        this.botId.set(botId);
    }

    public void clear() {
        this.botId.remove();
    }
}
