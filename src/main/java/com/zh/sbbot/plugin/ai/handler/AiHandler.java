package com.zh.sbbot.plugin.ai.handler;

import com.zh.sbbot.plugin.ai.dao.PluginAi;
import com.zh.sbbot.plugin.ai.support.ChatResponse;

import java.util.List;

public interface AiHandler {
    /**
     * AI chat
     *
     * @param text           用户输入的文本
     * @param conversationId 用户的会话ID
     * @param pluginAi       AI配置
     */
    ChatResponse chat(PluginAi pluginAi, String text, String conversationId);

    /**
     * AI chat
     *
     * @param text     用户输入的文本
     * @param pluginAi AI配置
     */
    default ChatResponse chat(PluginAi pluginAi, String text) {
        ChatResponse chatResponse = chat(pluginAi, text, "temp");
        clear("temp");
        return chatResponse;
    }

    /**
     * AI chat，具备AI视觉能力
     *
     * @param text           用户输入的文本
     * @param conversationId 用户的会话ID
     * @param pluginAi       AI配置
     */
    ChatResponse chat(PluginAi pluginAi, String text, List<String> images, String conversationId);

    /**
     * AI chat，具备AI视觉能力
     *
     * @param text     用户输入的文本
     * @param pluginAi AI配置
     */
    default ChatResponse chat(PluginAi pluginAi, String text, List<String> images) {
        ChatResponse chatResponse = chat(pluginAi, text, images, "temp");
        clear("temp");
        return chatResponse;
    }


    /**
     * 所属的AI厂商
     */
    String vendor();

    /**
     * 默认模型
     */
    String defaultModel();

    /**
     * 清除某个会话
     */
    void clear(String conversationId);

    /**
     * 根据前缀批量删除会话
     */
    void clearByPrefix(String conversationIdPrefix);
}
