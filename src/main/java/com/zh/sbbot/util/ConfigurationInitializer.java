package com.zh.sbbot.util;

import com.alibaba.fastjson2.JSONObject;
import com.zh.sbbot.repository.DictRepository;
import com.zh.sbbot.springevent.DictModifyEvent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.event.EventListener;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

/**
 * 从dict表中读取并覆盖相同配置
 */
@Slf4j
public abstract class ConfigurationInitializer implements CommandLineRunner {
    private String prefix = "";
    @Autowired
    private DictRepository dictRepository;

    @Override
    public void run(String... args) {
        ConfigurationProperties[] configurationProperties = this.getClass().getSuperclass().getAnnotationsByType(ConfigurationProperties.class);

        if (configurationProperties.length == 0) {
            throw new IllegalArgumentException("no @ConfigurationProperties found on " + this.getClass().getSuperclass().getTypeName());
        }

        prefix = Optional.ofNullable(configurationProperties[0].prefix()).filter(StringUtils::isNotBlank).orElse(configurationProperties[0].value());
        if (StringUtils.isBlank(prefix))
            throw new IllegalArgumentException("prefix or value must be set on @ConfigurationProperties");

        for (Field field : this.getClass().getSuperclass().getDeclaredFields()) {
            fillFieldFromDB(field);
        }
    }

    @SneakyThrows
    private Object fillFieldFromDB(Field field) {
        String key = prefix + "." + field.getName();
        Object value = dictRepository.get(key, field.getType());
        if (value == null) {
            return null;
        }
        field.setAccessible(true);
        field.set(this, value);
        log.info("property was overridden by: [{}]", key);
        return value;
    }

    @SneakyThrows
    @EventListener
    public void onModifyDict(DictModifyEvent event) {
        Arrays.stream(this.getClass().getSuperclass().getDeclaredFields())
                .filter(field -> (prefix + "." + field.getName()).replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase().equals(event.getKey()))
                .forEach(field -> {
                    Object value = fillFieldFromDB(field);
                    log.info("receive dict modify event: {} = {}", event.getKey(), JSONObject.toJSONString(value));
                });
    }
}
