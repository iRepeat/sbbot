package com.zh.sbbot.config;

import com.zh.sbbot.util.ConfigurationInitializer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
     * 默认bot
     */
    private Long defaultBot;

    /**
     * 全局bot开关。
     * 可由".up/.down"命令临时切换开关状态。
     */
    private boolean enable = true;
}
