package com.zh.sbbot.plugins.ai.handler.qianfan;

import com.baidubce.qianfan.core.auth.Auth;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("shiro.plugin.ai.qianfan")
@Configuration
@Data
public class QianFanConfig {
    private String accessKey;
    private String secretKey;
    /**
     * 可选 OAuth（默认），IAM
     * {@link com.baidubce.qianfan.core.auth.Auth 参考}
     */
    private String authType = Auth.TYPE_OAUTH;
}
