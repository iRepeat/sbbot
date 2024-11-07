package com.zh.sbbot.plugin.ai.handler.qwen;

import com.zh.sbbot.util.ConfigurationInitializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(prefix = "plugin.ai.qwen")
@Configuration
@Data
public class QwenConfig extends ConfigurationInitializer {
    private String apiKey;
}
