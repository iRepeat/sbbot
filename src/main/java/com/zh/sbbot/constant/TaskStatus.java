package com.zh.sbbot.constant;

import lombok.SneakyThrows;

import java.lang.reflect.Field;

/**
 * 计划任务的状态
 */
public interface TaskStatus {
    /**
     * 正常启用的状态
     */
    String ENABLE = "enable";
    /**
     * 任务禁用。
     */
    String DISABLE = "disable";
    /**
     * 任务下次执行时间过期或者大于半年
     */
    String INVALID = "invalid";

    @SneakyThrows
    static boolean isValid(String taskStatus) {
        for (Field field : TaskStatus.class.getFields()) {
            String status = (String) field.get(null);
            if (status.equals(taskStatus)) {
                return true;
            }
        }
        return false;
    }
}
