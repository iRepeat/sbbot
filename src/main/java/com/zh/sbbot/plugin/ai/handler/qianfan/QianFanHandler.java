package com.zh.sbbot.plugin.ai.handler.qianfan;

import com.baidubce.qianfan.Qianfan;
import com.baidubce.qianfan.model.chat.Message;
import com.zh.sbbot.plugin.ai.dao.PluginAi;
import com.zh.sbbot.plugin.ai.handler.AiHandler;
import com.zh.sbbot.plugin.ai.support.ChatResponse;
import com.zh.sbbot.plugin.ai.support.RoleEnum;
import com.zh.sbbot.plugin.ai.support.VendorEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class QianFanHandler implements AiHandler {
    private final QianFanConfig qianFanConfig;
    private final QianFanInMemoryChatHistory chatHistory;


    @Override
    public ChatResponse generateAnswer(PluginAi pluginAi, String text, String conversationId) {
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
                return ChatResponse.build(assistantMsg.getContent(), "触发百度风控：ban_round="+ response.getBanRound());
            }
            return ChatResponse.build(assistantMsg.getContent());

        } catch (Exception e) {
            log.error("qianfan error: ", e);
            chatHistory.repairEnd(conversationId);
            return ChatResponse.build(e.getMessage());
        }
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
