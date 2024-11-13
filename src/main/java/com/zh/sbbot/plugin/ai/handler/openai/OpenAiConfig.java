package com.zh.sbbot.plugin.ai.handler.openai;

import com.zh.sbbot.util.ConfigurationInitializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(prefix = "plugin.ai.openai")
@Configuration
@Data
public class OpenAiConfig extends ConfigurationInitializer {
    private String apiKey;
    private String baseUrl;
}
