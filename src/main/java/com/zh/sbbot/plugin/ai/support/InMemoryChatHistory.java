package com.zh.sbbot.plugin.ai.support;


import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上下文历史管理
 */
public class InMemoryChatHistory<T> {

    protected final Map<String, ArrayList<T>> conversationHistory = new ConcurrentHashMap<>();

    public void add(String conversationId, T message) {
        if (!conversationHistory.containsKey(conversationId)) {
            initIfAbsent(conversationId, new ArrayList<>(Collections.singleton(message)));
            return;
        }
        this.conversationHistory.get(conversationId).add(message);
    }

    public void initIfAbsent(String conversationId, ArrayList<T> messages) {
        this.conversationHistory.putIfAbsent(conversationId, CollectionUtils.isEmpty(messages) ? new ArrayList<>() :
                new ArrayList<>(messages));
    }

    public void clear(String conversationId) {
        this.conversationHistory.remove(conversationId);
    }

    public void clearByPrefix(String conversationIdPrefix) {
        this.conversationHistory.keySet().stream().filter(k -> k.startsWith(conversationIdPrefix)).forEach(this.conversationHistory::remove);
    }

    public ArrayList<T> get(String conversationId) {
        ArrayList<T> history = this.conversationHistory.get(conversationId);
        return history == null ? null : new ArrayList<>(history);
    }

    /**
     * 截取lastN消息
     */
    public ArrayList<T> lastN(String conversationId, int lastN) {
        ArrayList<T> history = this.conversationHistory.get(conversationId);

        // 如果没有历史记录或历史记录为空，则返回空列表。
        if (CollectionUtils.isEmpty(history)) {
            return new ArrayList<>();
        }


        // 如果lastN大于或等于可用消息数，则返回所有消息
        if (lastN >= history.size()) {
            return new ArrayList<>(history);
        }

        // 计算获取最近N条消息的起始索引
        int start = history.size() - lastN;

        return new ArrayList<>(history.subList(start, history.size()));
    }

}
