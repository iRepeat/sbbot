package com.zh.sbbot.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskModel {
    /**
     * 任务ID
     */
    @Builder.Default
    private Long key = System.currentTimeMillis();

    /**
     * cron表达式
     */
    private String cron;

    /**
     * 下次执行时间
     */
    private String nextTime;

    /**
     * 状态
     *
     * @see com.zh.sbbot.constant.TaskStatus
     */
    private String status;

    /**
     * 上次执行结果
     */
    private String lastResult;

    /**
     * 上次执行结果状态
     */
    private String lastStatus;

    /**
     * 任务参数
     */
    private String cmd;

    /**
     * 所属群聊
     */
    private Long groupId;

    /**
     * 目标用户
     */
    private Long userId;

    /**
     * botId
     */
    private Long botId;

}


