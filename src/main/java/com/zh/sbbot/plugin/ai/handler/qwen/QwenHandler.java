package com.zh.sbbot.plugin.ai.handler.qwen;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.zh.sbbot.plugin.ai.dao.PluginAi;
import com.zh.sbbot.plugin.ai.handler.AiHandler;
import com.zh.sbbot.plugin.ai.support.ChatResponse;
import com.zh.sbbot.plugin.ai.support.VendorEnum;
import com.zh.sbbot.util.OCRUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class QwenHandler implements AiHandler {
    private final QwenConfig config;
    private final QwenInMemoryChatHistory chatHistory;
    private final OCRUtil ocrUtil;

    @Override
    public ChatResponse chat(PluginAi pluginAi, String text, String conversationId) {
        try {

            // 添加系统信息（如果不存在）和用户对话到上下文
            Message systemMsg = Message.builder().role(Role.SYSTEM.getValue()).content(pluginAi.getSystemTemplate()).build();
            Message userMsg = Message.builder().role(Role.USER.getValue()).content(text).build();
            chatHistory.setSystem(conversationId, systemMsg);
            chatHistory.add(conversationId, userMsg);

            // 构造参数
            GenerationParam param = GenerationParam.builder()
                    .model(pluginAi.getModel())
                    .apiKey(config.getApiKey())
                    .messages(chatHistory.lastN(conversationId, pluginAi.getLastN()))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .temperature(Float.valueOf(pluginAi.getTemperature()))
                    .topP(Double.valueOf(pluginAi.getTopP()))
                    .maxTokens(pluginAi.getMaxToken())
                    .enableSearch(pluginAi.getEnableSearch() == 1)
                    .build();

            // 调用
            GenerationResult result = new Generation().call(param);

            // 添加响应到上下文
            Message output = result.getOutput().getChoices().get(0).getMessage();
            chatHistory.add(conversationId, output);

            return ChatResponse.build(output.getContent());
        } catch (Exception e) {
            log.error("qwen error:", e);
            chatHistory.repairEnd(conversationId);
            return ChatResponse.build(e.getMessage());
        }
    }

    @Override
    public ChatResponse chat(PluginAi pluginAi, String text, List<String> images, String conversationId) {
        text += extractTextUseOcr(images);
        return chat(pluginAi, text, conversationId);
    }

    /**
     * 使用OCR能力识别图片文本内容
     */
    private String extractTextUseOcr(List<String> images) {
        if (CollectionUtils.isEmpty(images)) {
            return StringUtils.EMPTY;
        }

        return images.stream()
                .map(ocrUtil::baidu)
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String vendor() {
        return VendorEnum.qwen.name();
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
