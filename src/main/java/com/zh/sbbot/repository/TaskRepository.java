package com.zh.sbbot.repository;

import com.zh.sbbot.constant.TaskStatus;
import com.zh.sbbot.task.TaskModel;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 命令别名
 */
@Slf4j
@RequiredArgsConstructor
@Repository
public class TaskRepository {
    private final JdbcTemplate jdbcTemplate;

    public TaskModel get(Long key) {
        return this.jdbcTemplate.queryForObject("SELECT * FROM tasks WHERE `key` = ?",
                new BeanPropertyRowMapper<>(TaskModel.class), key);
    }

    public void setStatus(Long key, String status) {
        this.jdbcTemplate.update("update tasks set status = ? WHERE `key` =  ? ", status, key);
    }

    /**
     * @param excludeInvalid 为 true 时，仅查询状态是{@link TaskStatus#ENABLE}和状态是{@link TaskStatus#DISABLE}任务列表
     * @param groupId        只检索指定群聊。为 null 时检索全部
     */
    public List<TaskModel> list(boolean excludeInvalid, @Nullable Long groupId) {
        String sql;
        if (excludeInvalid) {
            sql = "SELECT * FROM tasks WHERE status IN ('enable', 'disable')";
        } else {
            sql = "SELECT * FROM tasks WHERE true ";
        }
        if (Objects.nonNull(groupId)) {
            sql += "and `group_id` = " + groupId;
        }
        return this.jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(TaskModel.class));
    }

    public void batchUpdate(List<TaskModel> newTasks) {
        String sql = "UPDATE tasks SET cron = ?, status = ?, next_time = ?, cmd = ?, group_id = ?, " +
                "last_result = ?, last_status = ?, user_id = ?, bot_id = ? WHERE `key` = ?";

        List<Object[]> batchArgs = new ArrayList<>();

        for (TaskModel task : newTasks) {
            batchArgs.add(new Object[]{
                    task.getCron(),
                    task.getStatus(),
                    StringUtils.isBlank(task.getNextTime()) ? StringUtils.EMPTY : task.getNextTime(),
                    task.getCmd(),
                    task.getGroupId(),
                    task.getLastResult(),
                    task.getLastStatus(),
                    task.getUserId(),
                    task.getBotId(),
                    task.getKey()
            });
        }

        this.jdbcTemplate.batchUpdate(sql, batchArgs);

    }

    public void saveOrUpdate(TaskModel task) {
        String selectSql = "SELECT COUNT(*) FROM tasks WHERE `key` = ?";
        Integer count = jdbcTemplate.queryForObject(selectSql, Integer.class, task.getKey());

        if (count > 0) {
            String updateSql = "UPDATE tasks SET cron = ?, status = ?, next_time = ?, cmd = ?, group_id = ?, " +
                    "last_result = ?, last_status = ?, user_id = ?, bot_id = ? WHERE `key` = ?";
            jdbcTemplate.update(updateSql, task.getCron(), task.getStatus(),
                    StringUtils.isBlank(task.getNextTime()) ? StringUtils.EMPTY : task.getNextTime(),
                    task.getCmd(), task.getGroupId(), task.getLastResult(),
                    task.getLastStatus(), task.getUserId(), task.getBotId(), task.getKey());
        } else {
            String insertSql = "INSERT INTO tasks (`key`, cron, status, next_time, cmd, group_id, last_result, " +
                    "last_status, user_id, bot_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(insertSql, task.getKey(), task.getCron(), task.getStatus(),
                    StringUtils.isBlank(task.getNextTime()) ? StringUtils.EMPTY : task.getNextTime(),
                    task.getCmd(), task.getGroupId(), task.getLastResult(),
                    task.getLastStatus(), task.getUserId(), task.getBotId());
        }
    }

    @SuppressWarnings("all")
    public void set(Long key, String fieldKey, String fieldValue) {
        String sql = "update tasks set %s = ? where `key` = ?".formatted(fieldKey);
        jdbcTemplate.update(sql, fieldValue, key);
    }

    public void remove(Long key) {
        jdbcTemplate.update("delete from tasks where `key` = ?", key);
    }
}
