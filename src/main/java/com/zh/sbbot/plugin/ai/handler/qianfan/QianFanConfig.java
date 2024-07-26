package com.zh.sbbot.plugin.ai.handler.qianfan;

import com.baidubce.qianfan.core.auth.Auth;
import com.zh.sbbot.util.ConfigurationInitializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("shiro.plugin.ai.qianfan")
@Configuration
@Data
public class QianFanConfig extends ConfigurationInitializer {
    private String accessKey;
    private String secretKey;
    /**
     * 可选 OAuth（默认），IAM
     * {@link com.baidubce.qianfan.core.auth.Auth 参考}
     */
    private String authType = Auth.TYPE_OAUTH;
}
