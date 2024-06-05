package com.zh.sbbot.plugins.ai.handler.qwen;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.zh.sbbot.plugins.ai.common.AIVendorEnum;
import com.zh.sbbot.plugins.ai.dao.PluginAi;
import com.zh.sbbot.plugins.ai.handler.AiHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class QwenHandler implements AiHandler {
    private final QwenConfig config;
    private final InMemoryChatHistory chatHistory;

    @Override
    public String generateAnswer(PluginAi pluginAi, String text, String conversationId) {
        try {

            // 添加系统信息（如果不存在）和用户对话到上下文
            Message systemMsg = Message.builder().role(Role.SYSTEM.getValue()).content(pluginAi.getSystemTemplate()).build();
            Message userMsg = Message.builder().role(Role.USER.getValue()).content(text).build();
            chatHistory.initIfAbsent(conversationId, List.of(systemMsg))
                    .add(conversationId, List.of(userMsg));

            // 构造参数
            GenerationParam param = GenerationParam.builder()
                    .model(pluginAi.getModel())
                    .apiKey(config.getApiKey())
                    .messages(chatHistory.get(conversationId, pluginAi.getLastN()))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .temperature(Float.valueOf(pluginAi.getTemperature()))
                    .topP(0.8)
                    .prompt(pluginAi.getPromptTemplate())
                    .maxTokens(pluginAi.getMaxToken())
                    .enableSearch(pluginAi.getEnableSearch() == 1)
                    .build();

            // 调用
            GenerationResult result = new Generation().call(param);

            // 添加响应到上下文
            Message output = result.getOutput().getChoices().get(0).getMessage();
            chatHistory.add(conversationId, List.of(output));

            return output.getContent();
        } catch (Exception e) {
            log.error("qwen error:", e);
            return e.getMessage();
        }
    }

    @Override
    public String vendor() {
        return AIVendorEnum.qwen.name();
    }

    @Override
    public String defaultModel() {
        return "qwen-turbo";
    }

    @Override
    public void clear(String conversationId) {
        chatHistory.clear(conversationId);
    }

    @Override
    public void clearByPrefix(String conversationIdPrefix) {
        chatHistory.clearByPrefix(conversationIdPrefix);
    }
}
