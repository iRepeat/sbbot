package com.zh.sbbot.repository;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.TypeReference;
import com.zh.sbbot.springevent.DictModifyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 数据字典
 * <p>
 * 可用作系统配置、插件配置或者自定义键值对
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class DictRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 设置/移除键值对
     */
    public void setOrRemove(String key, Object value) {
        // 将驼峰式命名转换为短线式命名：如apiKey <=> api-key
        key = key.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();

        if (value == null || (value instanceof String) && StringUtils.isBlank((String) value)) {
            this.jdbcTemplate.update("DELETE from dict where `key` = ?", key);
        } else {
            this.jdbcTemplate.update("INSERT OR REPLACE INTO dict (key, value) VALUES (?,?)", key,
                    value instanceof String ? value : JSON.toJSONString(value));
        }

        eventPublisher.publishEvent(new DictModifyEvent(this, key));
    }

    /**
     * 根据key获取dict表的value值
     *
     * @param key 驼峰式命名和短线式命名等价，如apikey <=> api-key
     */
    public String get(String key) {
        String value = accurateGet(key);
        if (value == null) {
            // 将驼峰式命名转换为短线式命名：如apiKey <=> api-key
            key = key.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
            value = accurateGet(key);
        }
        return value;
    }

    /**
     * 根据key获取dict表的value值，并转换为指定类型
     *
     * @param key 驼峰式命名和短线式命名等价，如apikey <=> api-key
     */
    public <T> T get(String key, Class<T> type) {
        T value = accurateGet(key, type);
        if (value == null) {
            // 将驼峰式命名转换为短线式命名：如apiKey <=> api-key
            key = key.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
            value = accurateGet(key, type);
        }
        return value;
    }


    /**
     * 根据key获取dict表的value值，并转换为指定类型
     *
     * @param key 驼峰式命名和短线式命名等价，如apikey <=> api-key
     */
    public <T> T get(String key, TypeReference<T> type) {
        T value = accurateGet(key, type);
        if (value == null) {
            // 将驼峰式命名转换为短线式命名：如apiKey <=> api-key
            key = key.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
            value = accurateGet(key, type);
        }
        return value;
    }


    /**
     * 根据key获取dict表的value值
     */
    public String accurateGet(String key) {
        try {
            return this.jdbcTemplate.queryForObject("SELECT value FROM dict WHERE key = ?", String.class, key);
        } catch (IncorrectResultSizeDataAccessException e) {
            log.debug("key not found: {}", key);
            return null;
        }
    }

    /**
     * 根据key获取dict表的value值，并转换为指定类型
     */
    @SuppressWarnings("unchecked")
    public <T> T accurateGet(String key, Class<T> type) {
        String value = accurateGet(key);
        if (value == null) return null;
        if (type.equals(String.class)) return (T) value;
        try {
            return JSONObject.parseObject(value, type, JSONReader.Feature.SupportSmartMatch);
        } catch (Exception e) {
            log.error("parse value error: '{}', key: {}, detail: {} ", value, key, e.getMessage());
            return null;
        }
    }


    /**
     * 根据key获取dict表的value值，并转换为指定类型
     */
    public <T> T accurateGet(String key, TypeReference<T> type) {
        String value = accurateGet(key);
        if (value == null) return null;
        try {
            return JSONObject.parseObject(value, type, JSONReader.Feature.SupportSmartMatch);
        } catch (Exception e) {
            log.error("parse value error: '{}', key: {}, detail: {} ", value, key, e.getMessage());
            return null;
        }
    }

    /**
     * 获取所有的key
     */
    public List<String> getAll() {
        return this.jdbcTemplate.queryForList("SELECT `key` FROM dict", String.class);
    }

}
