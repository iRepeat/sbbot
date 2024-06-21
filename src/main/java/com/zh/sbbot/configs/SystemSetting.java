package com.zh.sbbot.configs;

import com.zh.sbbot.utils.ConfigurationInitializer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "shiro.system")
@Getter
@Setter
public class SystemSetting extends ConfigurationInitializer {

    /**
     * 管理员
     */
    private Long[] superUser;

    /**
     * 全局bot开关。
     * 可由".up/.down"命令临时切换开关状态。
     */
    private boolean enable = true;

    /**
     * 图片域名适配列表。匹配域名的图片会使用base64发送
     */
    private String[] adaptImageHost;
}
