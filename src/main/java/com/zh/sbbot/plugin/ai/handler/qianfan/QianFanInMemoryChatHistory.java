package com.zh.sbbot.plugin.ai.handler.qianfan;


import com.alibaba.dashscope.common.Role;
import com.baidubce.qianfan.model.chat.Message;
import com.zh.sbbot.plugin.ai.support.InMemoryChatHistory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * 千帆大模型上下文历史管理实现
 */
@Component
public class QianFanInMemoryChatHistory extends InMemoryChatHistory<Message> {

    @Override
    public ArrayList<Message> lastN(String conversationId, int lastN) {
        // 当前消息序列是user，assistant，[user，assistant...]，user
        // lastN需要是奇数，确保截取之后首个消息是user
        return super.lastN(conversationId, lastN % 2 == 0 ? lastN + 1 : lastN);
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
