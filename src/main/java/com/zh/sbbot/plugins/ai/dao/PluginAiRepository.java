package com.zh.sbbot.plugins.ai.dao;

import com.zh.sbbot.constant.DictKey;
import com.zh.sbbot.repository.DictRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
    public void init(long groupId, String defaultModel) {
        String deleteSql = "DELETE FROM plugin_ai WHERE group_id = ?";
        jdbcTemplate.update(deleteSql, groupId);
        String insertSql = "INSERT INTO plugin_ai (group_id, model) values (?,?)";
        // 使用当前AI服务的默认模型作为群聊的默认模型
        jdbcTemplate.update(insertSql, groupId, defaultModel);
    }

    /**
     * 初始化表
     */
    public void initTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS plugin_ai");
        String createTableSql = dictRepository.getValue(DictKey.PLUGIN_AI_CREATE_SQL);
        jdbcTemplate.execute(createTableSql);
    }

    public void disable(long groupId) {
        String insertSql = "update plugin_ai set is_disable = 1 where group_id = ?";
        jdbcTemplate.update(insertSql, groupId);
    }

    public void enable(long groupId) {
        String insertSql = "update plugin_ai set is_disable = 0 where group_id = ?";
        jdbcTemplate.update(insertSql, groupId);
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

    public void switchModel(Long groupId, String model) {
        String insertSql = "update plugin_ai set model = ? where group_id = ?";
        jdbcTemplate.update(insertSql, model, groupId);
    }
}
