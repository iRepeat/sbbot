package com.zh.sbbot.config;

import com.zh.sbbot.util.ConfigurationInitializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("system.tts.qq")
@Configuration
@Data
public class QQTTSConfig extends ConfigurationInitializer {
    /**
     * AI角色（决定音色）
     */
    private String character;
    /**
     * AI角色所属群聊
     */
    private Long group;
}
