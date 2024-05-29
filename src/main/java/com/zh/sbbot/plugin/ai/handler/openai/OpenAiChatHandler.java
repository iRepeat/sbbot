package com.zh.sbbot.plugin.ai.handler.openai;

import com.zh.sbbot.plugin.ai.dao.PluginAi;
import com.zh.sbbot.plugin.ai.handler.AiHandler;
import com.zh.sbbot.plugin.ai.support.ChatResponse;
import com.zh.sbbot.plugin.ai.support.VendorEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAiChatHandler implements AiHandler {
    private final OpenAiChatModel openAiApi;
    private final CustomInMemoryChatMemory chatMemory = new CustomInMemoryChatMemory();


    @Override
    public ChatResponse generateAnswer(PluginAi pluginAi, String text, String conversationId) {

        ChatClient.CallResponseSpec response = ChatClient
                .builder(openAiApi)
                .defaultAdvisors(new PromptChatMemoryAdvisor(chatMemory))
                .build()
                .prompt()
                .options(OpenAiChatOptions.builder().withModel(pluginAi.getModel()).withMaxTokens(pluginAi.getMaxToken())
                        .withTemperature(Double.valueOf(pluginAi.getTemperature())).withN(pluginAi.getLastN()).withTopP(Double.valueOf(pluginAi.getTopP())).build())
                .system(pluginAi.getSystemTemplate())
                .user(text)
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
                .call();

        return ChatResponse.build(response.content());
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
        this.chatMemory.clear(conversationId);
    }

    @Override
    public void clearByPrefix(String conversationIdPrefix) {
        this.chatMemory.clearByPrefix(conversationIdPrefix);
    }


}