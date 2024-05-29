package com.zh.sbbot.repository;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 命令别名
 */
@Slf4j
@RequiredArgsConstructor
@Repository
public class AliasRepository {
    private final JdbcTemplate jdbcTemplate;

    /**
     * 设置/移除键值对
     */
    public void setOrRemove(String cmd, Object value) {
        if (value == null || (value instanceof String) && StringUtils.isBlank((String) value)) {
            this.jdbcTemplate.update("DELETE from alias where cmd = ?", cmd);
        } else {
            this.jdbcTemplate.update("INSERT OR REPLACE INTO alias (cmd, value) VALUES (?,?)", cmd,
                    value instanceof String ? value : JSON.toJSONString(value));
        }
    }

    /**
     * 获取键值对
     */
    public String get(String cmd) {
        try {
            return this.jdbcTemplate.queryForObject("SELECT value FROM alias WHERE cmd = ?", String.class, cmd);
        } catch (IncorrectResultSizeDataAccessException e) {
            log.debug("cmd not found: {}", cmd);
            return null;
        }
    }


    /**
     * 获取所有的key
     */
    public List<String> getAll() {
        return this.jdbcTemplate.queryForList("SELECT `cmd` FROM alias", String.class);
    }


}
