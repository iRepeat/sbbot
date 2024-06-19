package com.zh.sbbot.repository;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * 设置键值对
     * 存在就覆盖，不存在就创建
     */
    public void setValue(String cmd, Object value) {
        this.jdbcTemplate.update("INSERT OR REPLACE INTO alias (cmd, value) VALUES (?,?)", cmd,
                value instanceof String ? value : JSON.toJSONString(value));
    }

    /**
     * 获取键值对
     */
    public String getValue(String cmd) {
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
    public List<String> getAllKeys() {
        return this.jdbcTemplate.queryForList("SELECT `cmd` FROM alias", String.class);
    }


}
