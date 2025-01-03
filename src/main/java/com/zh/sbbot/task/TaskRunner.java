package com.zh.sbbot.task;

import com.alibaba.fastjson2.JSONObject;
import com.mikuac.shiro.common.utils.EventUtils;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.handler.injection.InjectionHandler;
import com.zh.sbbot.constant.TaskStatus;
import com.zh.sbbot.custom.AnnotationHandlerContainer;
import com.zh.sbbot.repository.TaskRepository;
import com.zh.sbbot.util.BotUtil;
import com.zh.sbbot.util.ExceptionUtil;
import com.zh.sbbot.util.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;


@Slf4j
public class TaskRunner implements Runnable {
    private final Long taskKey;

    TaskRepository taskRepository = SpringContextHolder.getBean(TaskRepository.class);
    TaskManager taskManager = SpringContextHolder.getBean(TaskManager.class);
    BotContainer botContainer = SpringContextHolder.getBean(BotContainer.class);
    EventUtils eventUtils = SpringContextHolder.getBean(EventUtils.class);
    AnnotationHandlerContainer container = SpringContextHolder.getBean(AnnotationHandlerContainer.class);
    InjectionHandler injectionHandler = SpringContextHolder.getBean(InjectionHandler.class);


    private TaskRunner(Long taskKey) {
        this.taskKey = taskKey;
    }

    public static TaskRunner create(Long taskKey) {
        return new TaskRunner(taskKey);
    }

    @Override
    public void run() {
        TaskModel task = taskRepository.get(taskKey);

        if (!Objects.equals(task.getStatus(), TaskStatus.ENABLE)) {
            throw new RuntimeException("the task is not enable");
        }
        try {
            Bot bot = botContainer.robots.get(task.getBotId());
            String result = invokeMessage(task.getCmd(), bot, task.getUserId(), task.getGroupId());
            task.setLastResult(result);
            task.setLastStatus("ok");
        } catch (Exception e) {
            log.error("An exception occurred while executing the task, with key [{}]", task.getKey(), e);
            task.setLastResult(ExceptionUtil.getStackTrace(e));
            task.setLastStatus("fail");
        }
        LocalDateTime nextTime = (Objects.requireNonNull(new CronTrigger(task.getCron()).nextExecution(new SimpleTriggerContext())))
                .atZone(ZoneId.of("GMT+8")).toLocalDateTime();
        String nextTimeStr = nextTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        task.setNextTime(nextTimeStr);
        if (taskManager.judgeNextTime(nextTime)) {
            task.setStatus(TaskStatus.INVALID);
            taskManager.cancelTask(taskKey);
        }
        taskRepository.batchUpdate(Collections.singletonList(task));
    }

    private String invokeMessage(String message, Bot bot, Long userId, Long groupId) {


        boolean isGroup = Objects.nonNull(groupId);

        GroupMessageEvent event = new GroupMessageEvent();
        event.setFont(0);
        event.setMessage(message);
        event.setRawMessage(message);
        event.setMessageType(isGroup ? "group" : "private");
        event.setPostType("message");
        event.setSelfId(bot.getSelfId());
        event.setMessageId(0);
        GroupMessageEvent.GroupSender sender = new GroupMessageEvent.GroupSender();
        sender.setNickname("by task execute");
        sender.setSex("unknown");
        sender.setUserId(userId);
        event.setSender(sender);
        event.setTime(System.currentTimeMillis() / 1000);
        event.setUserId(userId);
        event.setGroupId(isGroup ? groupId : 0);
        event.setArrayMsg(ShiroUtils.rawToArrayMsg(message));

        // 执行消息过滤
        if (eventUtils.getInterceptor(bot.getBotMessageEventInterceptor()).preHandle(bot, event)) {
            // 强制可执行管理员命令
            bot.setAnnotationHandler(container.getAnnotationHandler());
            // 执行消息事件处理
            injectionHandler.invokeAnyMessage(bot, BotUtil.castToAnyMessageEvent(event));
            if (isGroup) {
                // 群聊事件还要执行群聊消息处理
                injectionHandler.invokeGroupMessage(bot, event);
            }
        }

        return JSONObject.toJSONString(event);

    }
}
