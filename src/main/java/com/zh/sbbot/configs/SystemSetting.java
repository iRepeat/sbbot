package com.zh.sbbot.configs;

import com.zh.sbbot.constant.DictKey;
import com.zh.sbbot.repository.DictRepository;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "shiro.system")
@Getter
@Setter
public class SystemSetting implements CommandLineRunner {
    @Autowired
    private DictRepository dictRepository;

    /**
     * 管理员
     */
    private Set<String> superUser = new HashSet<>();

    /**
     * 全局bot开关。
     * 可由".up/.down"命令临时切换开关状态。
     */
    private boolean enable = true;

    /**
     * 如果数据库中配置了超级用户或开关，则加载到内存中
     */
    @Override
    public void run(String... args) {
        // 合并配置文件和DB中的超级用户配置
        String superUsers = dictRepository.getValue(DictKey.SYSTEM_SUPER_USER);
        Optional.ofNullable(superUsers).filter(StringUtils::isNotBlank)
                .map(s -> s.split(","))
                .map(Set::of)
                .ifPresent(dbSuperUser -> superUser.addAll(dbSuperUser));

        Boolean isEnable = dictRepository.getValue(DictKey.SYSTEM_ENABLE, Boolean.class);
        // 无论是配置文件还是DB，只要有配置false，则enable=false
        Optional.ofNullable(isEnable).ifPresent(b -> enable = b && enable);
    }
}
