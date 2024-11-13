package com.zh.sbbot.plugin.ai.handler.openai;

import com.alibaba.fastjson2.JSONObject;
import com.zh.sbbot.plugin.ai.dao.PluginAi;
import com.zh.sbbot.plugin.ai.handler.AiHandler;
import com.zh.sbbot.plugin.ai.support.ChatResponse;
import com.zh.sbbot.plugin.ai.support.VendorEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAiChatHandler implements AiHandler {
    private final OpenAiInMemoryChatHistory chatHistory;
    private final OpenAiConfig openAiConfig;


    @Override
    public ChatResponse generateAnswer(PluginAi pluginAi, String text, String conversationId) {

        OpenAiApi openAiApi = new OpenAiApi(openAiConfig.getBaseUrl(), openAiConfig.getApiKey());

        // 添加系统提示到上下文
        OpenAiApi.ChatCompletionMessage systemMsg = new OpenAiApi.ChatCompletionMessage(pluginAi.getSystemTemplate(),
                OpenAiApi.ChatCompletionMessage.Role.SYSTEM);
        chatHistory.setSystem(conversationId, systemMsg);
        // 添加用户对话到上下文
        OpenAiApi.ChatCompletionMessage userMsg = new OpenAiApi.ChatCompletionMessage(text, OpenAiApi.ChatCompletionMessage.Role.USER);
        chatHistory.add(conversationId, userMsg);


        // 创建 ChatCompletionRequest 对象
        OpenAiApi.ChatCompletionRequest request = new OpenAiApi.ChatCompletionRequest(
                chatHistory.lastN(conversationId, pluginAi.getLastN()),
                pluginAi.getModel(),
                null,
                null,
                null,
                null,
                pluginAi.getMaxToken(),
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                Double.valueOf(pluginAi.getTemperature()),
                Double.valueOf(pluginAi.getTopP()),
                null,
                null,
                null,
                conversationId
        );
        try {
            ResponseEntity<OpenAiApi.ChatCompletion> response = openAiApi.chatCompletionEntity(request);

            Assert.state(!response.getStatusCode().isError() && response.getBody() != null && !response.getBody().choices().isEmpty(),
                    "AI响应出错：" + JSONObject.toJSONString(response));

            OpenAiApi.ChatCompletionMessage assistantMsg =response.getBody().choices().get(0).message();

            chatHistory.add(conversationId, assistantMsg);

            return ChatResponse.build(assistantMsg.content());
        } catch (Exception e) {
            chatHistory.repairEnd(conversationId);
            throw e;
        }
    }

    @Override
    public String vendor() {
        return VendorEnum.openai.name();
    }

    @Override
    public String defaultModel() {
        return "gpt-4o-mini";
    }

    @Override
    public void clear(String conversationId) {
        this.chatHistory.clear(conversationId);
    }

    @Override
    public void clearByPrefix(String conversationIdPrefix) {
        this.chatHistory.clearByPrefix(conversationIdPrefix);
    }


}