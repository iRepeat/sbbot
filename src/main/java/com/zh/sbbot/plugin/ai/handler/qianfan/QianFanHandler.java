package com.zh.sbbot.plugin.ai.handler.qianfan;

import com.baidubce.qianfan.Qianfan;
import com.baidubce.qianfan.model.chat.Message;
import com.zh.sbbot.plugin.ai.dao.PluginAi;
import com.zh.sbbot.plugin.ai.handler.AiHandler;
import com.zh.sbbot.plugin.ai.support.ChatResponse;
import com.zh.sbbot.plugin.ai.support.RoleEnum;
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
@RequiredArgsConstructor
@Slf4j
public class QianFanHandler implements AiHandler {
    private final QianFanConfig qianFanConfig;
    private final QianFanInMemoryChatHistory chatHistory;
    private final OCRUtil ocrUtil;


    @Override
    public ChatResponse chat(PluginAi pluginAi, String text, String conversationId) {
        try {

            // 添加用户对话到上下文
            Message userMsg = new Message().setRole(RoleEnum.USER).setContent(text);
            chatHistory.add(conversationId, userMsg);


            com.baidubce.qianfan.model.chat.ChatResponse response = new Qianfan(qianFanConfig.getAuthType(), qianFanConfig.getAccessKey(), qianFanConfig.getSecretKey())
                    .chatCompletion()
                    .model(pluginAi.getModel())
                    .messages(chatHistory.lastN(conversationId, pluginAi.getLastN()))
                    .system(pluginAi.getSystemTemplate())
                    .topP(Double.valueOf(pluginAi.getTopP()))
                    .temperature(Double.valueOf(pluginAi.getTemperature()))
                    .disableSearch(pluginAi.getEnableSearch() == 0)
                    .maxOutputTokens(pluginAi.getMaxToken())
                    .execute();

            Message assistantMsg = new Message().setRole("assistant").setContent(response.getResult());
            chatHistory.add(conversationId, assistantMsg);

            if (response.getNeedClearHistory()) {
                clear(conversationId);
                return ChatResponse.build(assistantMsg.getContent(), "触发百度风控：ban_round=" + response.getBanRound());
            }
            return ChatResponse.build(assistantMsg.getContent());

        } catch (Exception e) {
            log.error("qianfan error: ", e);
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
        return VendorEnum.qianfan.name();
    }

    @Override
    public String defaultModel() {
        return "ERNIE-3.5-8K";
    }

    @Override
    public void clear(String conversationId) {
        chatHistory.clear(conversationId);
    }

    @Override
    public void clearByPrefix(String conversationIdPrefix) {
        chatHistory.clear(conversationIdPrefix);
    }
}
