package com.zh.sbbot.plugins.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;

@Getter
enum MsgType {
    PRIVATE,
    GROUP
}

@Data
public class SimplePushModel {
    /**
     * 消息类型 {@link MsgType}
     */
    private String type;

    /**
     * 目标用户或者群组
     */
    @Nonnull
    private Long target;

    /**
     * 文本。可接受CQ码
     */
    @Nonnull
    private String text;

    /**
     * 机器人id
     */
    @Nonnull
    private Long bot;

    @Override
    @SneakyThrows
    public String toString() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
