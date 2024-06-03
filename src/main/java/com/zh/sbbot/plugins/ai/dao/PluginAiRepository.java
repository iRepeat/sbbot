package com.zh.sbbot.plugins.ai.dao;

import com.zh.sbbot.constant.DictKey;
import com.zh.sbbot.repository.DictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class PluginAiRepository {
    private final JdbcTemplate jdbcTemplate;
    private final DictRepository dictRepository;

    /**
     * groupId为0，表示创建plugin_ai表；否则新建一条对应groupId记录
     */
    public void init(long groupId) {
        if (groupId == 0) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS plugin_ai_config");
            String createTableSql = dictRepository.getValue(DictKey.PLUGIN_AI_CREATE_SQL);
            jdbcTemplate.execute(createTableSql);
        } else {
            // 初始化该group的配置信息
            PluginAi defaultConfig = dictRepository.getValue(DictKey.PLUGIN_AI_DEFAULT_CONFIG, PluginAi.class);
            String deleteSql = "DELETE FROM plugin_ai_config WHERE group_id = ?";
            jdbcTemplate.update(deleteSql, groupId);
            String insertSql = "INSERT INTO plugin_ai_config (group_id, system_template, prompt_template, max_token, " +
                    "model, temperature, is_disable) values (?,?,?,?,?,?,?)";
            jdbcTemplate.update(insertSql, groupId, defaultConfig.getSystemTemplate(), defaultConfig.getPromptTemplate(),
                    defaultConfig.getMaxToken(), defaultConfig.getModel(), defaultConfig.getTemperature(), defaultConfig.getIsDisable());
        }
    }

    public void disable(long groupId) {
        String insertSql = "update plugin_ai_config set is_disable = 1 where group_id = ?";
        jdbcTemplate.update(insertSql, groupId);
    }

    public void enable(long groupId) {
        String insertSql = "update plugin_ai_config set is_disable = 0 where group_id = ?";
        jdbcTemplate.update(insertSql, groupId);
    }

    public PluginAi findOne(long groupId) {
        String sql = "SELECT * FROM plugin_ai_config where group_id = ?";
        return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(PluginAi.class), groupId);
    }
}
