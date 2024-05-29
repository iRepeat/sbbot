package com.zh.sbbot.plugin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;


@Data
@NoArgsConstructor
public class SimpleMsgModel {
    /**
     * 群组id（为空表示私聊消息）
     */
    private Long group;

    /**
     * 目标用户
     */
    private Long user;

    /**
     * 文本。可接受CQ码
     */
    private String text;

    /**
     * 机器人id
     */
    private Long bot;

    @Override
    @SneakyThrows
    public String toString() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
