package com.zh.sbbot.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "shiro.system")
@Getter
@Setter
public class SystemSetting {
    /**
     * 管理员
     */
    private Set<Long> superUser;

    /**
     * 全局bot开关。
     * 可由".up/.down"命令临时切换开关状态。
     */
    private boolean enable = true;

}
