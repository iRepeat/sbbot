package com.zh.sbbot.configs;

import com.zh.sbbot.utils.ConfigurationInitializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("shiro.system.ocr.baidu")
@Configuration
@Data
public class BaiduOCRConfig extends ConfigurationInitializer {
    private String appId;
    private String apiKey;
    private String secretKey;
    private boolean accurate;
}
