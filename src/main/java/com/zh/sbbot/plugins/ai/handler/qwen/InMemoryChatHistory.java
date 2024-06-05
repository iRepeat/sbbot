package com.zh.sbbot.plugins.ai.handler.qwen;


import com.alibaba.dashscope.common.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上下文历史管理
 */
@Component
public class InMemoryChatHistory {

    private final Map<String, List<Message>> conversationHistory = new ConcurrentHashMap<>();

    public InMemoryChatHistory add(String conversationId, List<Message> messages) {
        this.conversationHistory.get(conversationId).addAll(messages);
        return this;
    }

    public List<Message> get(String conversationId, int lastN) {
        List<Message> history = this.conversationHistory.get(conversationId);

        // 如果没有历史记录或历史记录为空，则返回空列表。
        if (CollectionUtils.isEmpty(history)) {
            return List.of();
        }

        // 在最终列表中始终包含第一个系统消息
        Message systemMessage = history.get(0);
        int historySize = history.size();

        // 要确保截取消息之后仍按照system，user，assistant的顺序，lastN需要为奇数
        lastN = lastN % 2 == 0 ? lastN + 1 : lastN;

        // 如果lastN大于或等于可用消息数，则返回所有消息
        if (lastN >= historySize - 1) {
            return new ArrayList<>(history);
        }

        // 计算获取最近N条消息的起始索引
        int start = historySize - lastN;

        // 准备要返回的消息列表
        List<Message> resultList = new ArrayList<>(lastN + 1);
        resultList.add(systemMessage);
        resultList.addAll(history.subList(start, historySize));

        return resultList;
    }

    public InMemoryChatHistory initIfAbsent(String conversationId, List<Message> messages) {
        this.conversationHistory.putIfAbsent(conversationId, CollectionUtils.isEmpty(messages) ? new ArrayList<>() :
                new ArrayList<>(messages));
        return this;
    }

    public void clear(String conversationId) {
        this.conversationHistory.remove(conversationId);
    }

    public void clearByPrefix(String conversationIdPrefix) {
        this.conversationHistory.keySet().stream().filter(k -> k.startsWith(conversationIdPrefix)).forEach(this.conversationHistory::remove);
    }

}
