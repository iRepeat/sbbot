package com.zh.sbbot.repository;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 系统数据字典
 * <p>
 * 可用作系统动态配置
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class DictRepository {
    private final JdbcTemplate jdbcTemplate;

    /**
     * 设置/移除键值对
     */
    public void setOrRemove(String key, Object value) {
        if (value == null || (value instanceof String) && StringUtils.isBlank((String) value)) {
            this.jdbcTemplate.update("DELETE from dict where `key` = ?", key);
        } else {
            this.jdbcTemplate.update("INSERT OR REPLACE INTO dict (key, value) VALUES (?,?)", key,
                    value instanceof String ? value : JSON.toJSONString(value));
        }
    }

    /**
     * 获取键值对
     */
    public String get(String key) {
        try {
            return this.jdbcTemplate.queryForObject("SELECT value FROM dict WHERE key = ?", String.class, key);
        } catch (IncorrectResultSizeDataAccessException e) {
            log.debug("key not found: {}", key);
            return null;
        }
    }

    /**
     * 获取指定类型的值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        String value = get(key);
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
     * 获取所有的key
     */
    public List<String> getAll() {
        return this.jdbcTemplate.queryForList("SELECT `key` FROM dict", String.class);
    }

}
