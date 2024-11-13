package com.zh.sbbot.plugin.ai.handler.openai;

import com.zh.sbbot.plugin.ai.support.InMemoryChatHistory;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OPEN AI上下文历史管理实现
 */
@Component
public class OpenAiInMemoryChatHistory extends InMemoryChatHistory<OpenAiApi.ChatCompletionMessage> {

    /**
     * 单独维护系统消息
     */
    private final ConcurrentHashMap<String, OpenAiApi.ChatCompletionMessage> systemMessages = new ConcurrentHashMap<>();

    public void setSystem(String conversationId, OpenAiApi.ChatCompletionMessage systemMessage) {
        if (systemMessage.role().name().equals(OpenAiApi.ChatCompletionMessage.Role.SYSTEM.name())) {
            systemMessages.put(conversationId, systemMessage);
        }
    }

    public void removeSystem(String conversationId) {
        systemMessages.remove(conversationId);
    }

    public OpenAiApi.ChatCompletionMessage getSystem(String conversationId) {
        return systemMessages.get(conversationId);
    }

    @Override
    public ArrayList<OpenAiApi.ChatCompletionMessage> get(String conversationId) {
        ArrayList<OpenAiApi.ChatCompletionMessage> messages = super.get(conversationId);
        OpenAiApi.ChatCompletionMessage systemMessage = getSystem(conversationId);
        if (messages != null && systemMessage != null) {
            messages.add(0, systemMessage);
        }
        return messages;
    }

    @Override
    public ArrayList<OpenAiApi.ChatCompletionMessage> lastN(String conversationId, int lastN) {
        // 当前消息序列是user，assistant，[user，assistant...]，user
        // lastN需要是奇数，确保截取之后首个消息是user
        ArrayList<OpenAiApi.ChatCompletionMessage> messages = super.lastN(conversationId, lastN % 2 == 0 ? lastN + 1 : lastN);
        // 添加系统消息到消息起始位
        messages.add(0, getSystem(conversationId));
        return messages;
    }

    @Override
    public void clear(String conversationId) {
        super.clear(conversationId);
        removeSystem(conversationId);
    }

    @Override
    public void clearByPrefix(String conversationIdPrefix) {
        super.clearByPrefix(conversationIdPrefix);
        this.systemMessages.keySet().stream().filter(k -> k.startsWith(conversationIdPrefix)).forEach(this::removeSystem);
    }

    /**
     * 确保最后一个消息来源是assistant
     */
    public void repairEnd(String conversationId) {
        ArrayList<OpenAiApi.ChatCompletionMessage> messages = super.conversationHistory.get(conversationId);
        while (!messages.isEmpty() && !messages.get(messages.size() - 1).role().name().equals(OpenAiApi.ChatCompletionMessage.Role.ASSISTANT.name())) {
            messages.remove(messages.size() - 1);
        }
    }

}
