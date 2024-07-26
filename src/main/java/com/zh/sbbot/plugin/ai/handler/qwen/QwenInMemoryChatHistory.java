package com.zh.sbbot.plugin.ai.handler.qwen;


import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.zh.sbbot.plugin.ai.support.InMemoryChatHistory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通义千问上下文历史管理实现
 */
@Component
public class QwenInMemoryChatHistory extends InMemoryChatHistory<Message> {

    /**
     * 单独维护系统消息
     */
    private final ConcurrentHashMap<String, Message> systemMessages = new ConcurrentHashMap<>();

    public void setSystem(String conversationId, Message systemMessage) {
        if (systemMessage.getRole().equalsIgnoreCase(Role.SYSTEM.getValue())) {
            systemMessages.put(conversationId, systemMessage);
        }
    }

    public void removeSystem(String conversationId) {
        systemMessages.remove(conversationId);
    }

    public Message getSystem(String conversationId) {
        return systemMessages.get(conversationId);
    }

    @Override
    public ArrayList<Message> get(String conversationId) {
        ArrayList<Message> messages = super.get(conversationId);
        Message systemMessage = getSystem(conversationId);
        if (messages != null && systemMessage != null) {
            messages.add(0, systemMessage);
        }
        return messages;
    }

    @Override
    public ArrayList<Message> lastN(String conversationId, int lastN) {
        // 当前消息序列是user，assistant，[user，assistant...]，user
        // lastN需要是奇数，确保截取之后首个消息是user
        ArrayList<Message> messages = super.lastN(conversationId, lastN % 2 == 0 ? lastN + 1 : lastN);
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
        ArrayList<Message> messages = super.conversationHistory.get(conversationId);
        while (!messages.isEmpty() && !messages.get(messages.size() - 1).getRole().equalsIgnoreCase(Role.ASSISTANT.getValue())) {
            messages.remove(messages.size() - 1);
        }
    }

}
