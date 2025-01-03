package com.zh.sbbot.task;

import com.zh.sbbot.constant.TaskStatus;
import com.zh.sbbot.repository.TaskRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

@Component
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TaskManager implements ApplicationRunner {

    /**
     * 存储所有计划任务
     */
    private static final HashMap<Long, ScheduledFuture<?>> schedules = new HashMap<>();
    private final TaskRepository taskRepository;
    @Resource(name = "taskSchedulerPool")
    private ThreadPoolTaskScheduler taskScheduler;

    @Override
    public void run(ApplicationArguments args) {
        reload(null);
    }

    /**
     * 重新加载数据库中的计划任务
     *
     * @param groupId 如果不为null，仅重载指定群聊的task
     */
    public int reload(Long groupId) {
        List<TaskModel> tasks = taskRepository.list(true, groupId);
        if (groupId == null) {
            cancelAll();
        } else {
            tasks.forEach(taskModel -> cancelTask(taskModel.getKey()));
        }
        int count = 0;
        for (TaskModel task : tasks) {
            try {
                if (task.getStatus().equals(TaskStatus.ENABLE)) {
                    saveSchedulingTask(task);
                    count += 1;
                }
            } catch (Exception e) {
                log.error("loaded task {} failed", task.getKey(), e);
            }
        }
        log.debug("Loaded scheduled tasks: {}", count);
        return count;
    }

    public void cancelTask(Long... keys) {
        for (Long key : keys) {
            if (schedules.containsKey(key)) {
                ScheduledFuture<?> future = schedules.get(key);
                // 终止当前计划任务
                future.cancel(Boolean.TRUE);
                schedules.remove(key);
            }
        }
    }

    public void cancelAll() {
        schedules.forEach((key, value) -> value.cancel(true));
        schedules.clear();
    }


    /**
     * 判断下次执行时间是否有效
     * 1.下次执行时间未过期 2.下次执行时间在半年内
     */
    public boolean judgeNextTime(LocalDateTime nextTime) {
        ZoneId currentZone = ZoneId.of("GMT+8");
        return nextTime.isBefore(LocalDateTime.now(currentZone)) || nextTime.isAfter(LocalDateTime.now(currentZone).plusHours(6 * 30 * 24));
    }

    /**
     * 更改计划任务状态。“无效”状态的任务不能更新状态
     *
     * @param status {@link com.zh.sbbot.constant.TaskStatus}
     * @return 更新后的任务详情
     */
    public TaskModel setTaskStatus(Long key, @Nonnull String status) {
        Assert.state(TaskStatus.isValid(status), "Status is invalid: " + status);

        TaskModel dbTask = taskRepository.get(key);

        Assert.notNull(dbTask, "Task not found: " + key);

        Assert.state(Set.of(TaskStatus.ENABLE, TaskStatus.DISABLE).contains(dbTask.getStatus()),
                "Cannot enable tasks with status: [%s]".formatted(TaskStatus.INVALID));

        switch (status) {
            case TaskStatus.DISABLE, TaskStatus.INVALID -> {
                cancelTask(key);
                taskRepository.setStatus(key, status);
            }
            case TaskStatus.ENABLE -> {
                saveSchedulingTask(dbTask);
            }
        }

        return getSchedulingTask(key);
    }

    /**
     * @param excludeInvalid 为 true 时，仅查询状态是{@link TaskStatus#ENABLE}和状态是{@link TaskStatus#DISABLE}的任务列表
     * @param groupId        只检索指定群聊。为 null 时检索全部
     */
    public List<TaskModel> listSchedulingTask(boolean excludeInvalid, @Nullable Long groupId) {
        return taskRepository.list(excludeInvalid, groupId);
    }

    public TaskModel getSchedulingTask(Long key) {
        return taskRepository.get(key);
    }

    public void execTaskImmediately(Long key) {
        TaskRunner.create(key).run();
    }

    /**
     * 创建或更新计划任务
     */
    public void saveSchedulingTask(TaskModel task) {
        boolean validExpression = CronExpression.isValidExpression(task.getCron());
        if (!validExpression) {
            throw new RuntimeException("Invalid cron expression: " + task.getCron());
        }
        // 创建计划任务触发器
        CronTrigger trigger = new CronTrigger(task.getCron());

        // 计算cron的下次执行时间
        LocalDateTime nextTime = (Objects.requireNonNull(trigger.nextExecution(new SimpleTriggerContext())))
                .atZone(ZoneId.of("GMT+8")).toLocalDateTime();
        String nextTimeStr = nextTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        task.setNextTime(nextTimeStr);
        // 判断下次执行时间是否有效
        if (judgeNextTime(nextTime)) {
            throw new RuntimeException("下次执行时间（%s）过期或者间隔时间过长".formatted(nextTimeStr));
        }
        task.setStatus(TaskStatus.ENABLE);
        taskRepository.saveOrUpdate(task);

        // 创建执行器
        TaskRunner taskRunner = TaskRunner.create(task.getKey());
        // 添加计划任务
        ScheduledFuture<?> schedule = taskScheduler.schedule(taskRunner, trigger);

        schedules.put(task.getKey(), Objects.requireNonNull(schedule));
        log.info("register scheduled task  successful: [ {} ], with id: [ {} ], next execution time: [ {} ]",
                task.getCmd(), task.getKey(), task.getNextTime());
    }

    public TaskModel editTask(Long key, String fieldKey, String fieldValue) {
        if (schedules.containsKey(key) && "cron".equals(fieldKey)) {
            // 编辑了计划任务需要重新创建计划任务
            cancelTask(key);
            TaskModel existTask = getSchedulingTask(key);
            existTask.setCron(fieldValue);
            saveSchedulingTask(existTask);
        } else {
            // 代表变更了计划任务的参数，直接更新数据库即可
            taskRepository.set(key, fieldKey, fieldValue);
        }
        return getSchedulingTask(key);
    }

    public void delSchedulingTask(Long key) {
        cancelTask(key);
        taskRepository.remove(key);
    }
}