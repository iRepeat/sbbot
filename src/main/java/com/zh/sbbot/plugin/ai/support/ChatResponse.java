package com.zh.sbbot.plugin.ai.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;

@Data
@Builder
public class ChatResponse {
    private String result;
    private boolean clearHistory;
    private String clearReason;

    public static ChatResponse build(String result) {
        return ChatResponse.builder().result(result).build();
    }

    public static ChatResponse build(String result, String finishReason) {
        return ChatResponse.builder().result(result).clearHistory(true).clearReason(finishReason).build();
    }

    @Override
    @SneakyThrows
    public String toString() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
