package com.zh.sbbot.plugins.ai.handler.qwen;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("shiro.plugin.ai.qwen")
@Configuration
@Data
public class QwenConfig {
    private String apiKey;
}
