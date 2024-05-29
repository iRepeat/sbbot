package com.zh.sbbot.util;

import com.zh.sbbot.repository.DictRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * 从dict表中读取并覆盖相同配置
 */
public abstract class ConfigurationInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationInitializer.class);
    @Autowired
    private DictRepository dictRepository;


    @Override
    public void run(String... args) throws Exception {
        ConfigurationProperties[] configurationProperties = this.getClass().getSuperclass().getAnnotationsByType(ConfigurationProperties.class);

        if (configurationProperties.length == 0) {
            throw new IllegalArgumentException("no @ConfigurationProperties found on " + this.getClass().getSuperclass().getTypeName());
        }

        String prefix = Optional.ofNullable(configurationProperties[0].prefix()).filter(StringUtils::isNotBlank).orElse(configurationProperties[0].value());
        if (StringUtils.isBlank(prefix))
            throw new IllegalArgumentException("prefix or value must be set on @ConfigurationProperties");

        for (Field field : this.getClass().getSuperclass().getDeclaredFields()) {
            String key = prefix + "." + field.getName();
            Object value = dictRepository.get(key, field.getType());
            if (value == null) {
                // 将驼峰式命名转换为短线式命名：如apiKey => api-key
                key = prefix + "." + field.getName().replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
                value = dictRepository.get(key, field.getType());
                if (value == null) {
                    continue;
                }
            }
            field.setAccessible(true);
            field.set(this, value);
            log.info("property was overridden by: [{}]", key);
        }
    }
}
