package com.zh.sbbot.plugin.ai.dao;

import com.zh.sbbot.constant.DictKey;
import com.zh.sbbot.repository.DictRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PluginAiRepository {
    private final JdbcTemplate jdbcTemplate;
    private final DictRepository dictRepository;

    /**
     * 初始化对应群组的配置
     */
    public void init(long groupId, String defaultModel, String vendor) {
        String deleteSql = "DELETE FROM plugin_ai WHERE group_id = ?";
        jdbcTemplate.update(deleteSql, groupId);
        String sql = "INSERT INTO plugin_ai (group_id, model, vendor) values (?,?,?)";
        // 使用当前AI服务的默认模型作为群聊的默认模型
        jdbcTemplate.update(sql, groupId, defaultModel, vendor);
    }

    /**
     * 初始化表
     */
    public void initTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS plugin_ai");
        String createTableSql = dictRepository.get(DictKey.PLUGIN_AI_CREATE_SQL);
        jdbcTemplate.execute(createTableSql);
    }

    public void disable(long groupId) {
        String sql = "update plugin_ai set is_disable = 1 where group_id = ?";
        jdbcTemplate.update(sql, groupId);
    }

    public void enable(long groupId) {
        String sql = "update plugin_ai set is_disable = 0 where group_id = ?";
        jdbcTemplate.update(sql, groupId);
    }

    public List<PluginAi> getAll() {
        String sql = "SELECT * FROM plugin_ai";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(PluginAi.class));
    }

    public PluginAi findOne(long groupId) {
        String sql = "SELECT * FROM plugin_ai where group_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(PluginAi.class), groupId);
        } catch (IncorrectResultSizeDataAccessException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public void switchAi(Long groupId, String vendor, String model) {
        String sql = "update plugin_ai set model = ?, vendor = ? where group_id = ?";
        jdbcTemplate.update(sql, model, vendor, groupId);
    }

    public int update(String column, String value, Long groupId) {
        String sql = "update plugin_ai set %s = ? where group_id = ?".formatted(column);
        return jdbcTemplate.update(sql, value, groupId);
    }
}
