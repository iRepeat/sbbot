package com.zh.sbbot.plugins.ai.handler;

import com.zh.sbbot.plugins.ai.dao.PluginAi;

public interface AiHandler {
    /**
     * 获取AI答案
     *
     * @param text           用户输入的文本
     * @param conversationId 用户的会话ID
     * @param pluginAi       AI配置
     * @return AI答案
     */
    String generateAnswer(PluginAi pluginAi, String text, String conversationId);

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
