package com.zh.sbbot.plugins.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiService {
    private final OpenAiChatClient openAiApi;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取AI答案
     *
     * @param text 用户输入的文本
     * @return AI答案
     */
    public String getAnswer(AiModel aiConfig, String text) {

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(aiConfig.getSystemTemplate());
        Message systemMessage = systemPromptTemplate.createMessage();

        PromptTemplate promptTemplate = new PromptTemplate(aiConfig.getPromptTemplate() + "：{query}");
        Message userMessage = promptTemplate.createMessage(Map.of("query", text));

        OpenAiChatOptions modelOptions = new OpenAiChatOptions();
        modelOptions.setModel(aiConfig.getModel());
        modelOptions.setMaxTokens(aiConfig.getMaxToken());
        modelOptions.setTemperature(Float.valueOf(aiConfig.getTemperature()));

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage), modelOptions);
        ChatResponse chatResponse = this.openAiApi.call(prompt);
        return chatResponse.getResult().getOutput().getContent();
    }

    /**
     * groupId为0，表示创建plugin_ai表；否则新建一条对应groupId记录
     */
    public void init(long groupId) {
        if (groupId == 0) {
            String createTableSql = """
                        CREATE TABLE IF NOT EXISTS plugin_ai (
                            group_id INTEGER PRIMARY KEY,
                            system_template TEXT DEFAULT '你是群聊中的知识文库，你的语言风格是幽默的',
                            prompt_template TEXT DEFAULT '群友向你提出了下列的问题',
                            max_token TEXT DEFAULT 1024,
                            model TEXT DEFAULT 'gpt-3.5-turbo',
                            temperature REAL DEFAULT 0.7,
                            is_disable INTEGER DEFAULT 0 CHECK (is_disable IN (0, 1))
                        );
                    """;
            jdbcTemplate.execute(createTableSql);
        } else {
            // 新建一条记录。当存在记录时并不会新建
            String insertSql = "INSERT OR IGNORE INTO plugin_ai (group_id) VALUES (?)";
            jdbcTemplate.update(insertSql, groupId);
        }
    }

    public void disable(long groupId) {
        String insertSql = "update plugin_ai set is_disable = 1 where group_id = ?";
        jdbcTemplate.update(insertSql, groupId);
    }

    public void enable(long groupId) {
        String insertSql = "update plugin_ai set is_disable = 0 where group_id = ?";
        jdbcTemplate.update(insertSql, groupId);
    }

    public AiModel findOne(long groupId) {
        String sql = "SELECT * FROM plugin_ai where group_id = ?";
        return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(AiModel.class), groupId);
    }
}