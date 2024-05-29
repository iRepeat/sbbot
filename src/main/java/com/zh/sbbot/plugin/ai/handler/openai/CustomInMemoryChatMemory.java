package com.zh.sbbot.plugin.ai.handler.openai;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 拓展自{@link org.springframework.ai.chat.memory.InMemoryChatMemory}
 * <p>
 * 增加了批量删除会话的方法
 */
public class CustomInMemoryChatMemory implements ChatMemory {
    private Map<String, List<Message>> conversationHistory = new ConcurrentHashMap<>();

    @Override
    public void add(String conversationId, List<Message> messages) {
        this.conversationHistory.putIfAbsent(conversationId, new ArrayList<>());
        this.conversationHistory.get(conversationId).addAll(messages);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> all = this.conversationHistory.get(conversationId);
        return all != null ? all.stream().skip(Math.max(0, all.size() - lastN)).toList() : List.of();
    }

    @Override
    public void clear(String conversationId) {
        this.conversationHistory.remove(conversationId);
    }

    public void clearAll() {
        this.conversationHistory = new ConcurrentHashMap<>();
    }

    public void clearByPrefix(String conversationIdPrefix) {
        this.conversationHistory.keySet().stream().filter(k -> k.startsWith(conversationIdPrefix)).forEach(this.conversationHistory::remove);
    }


}
