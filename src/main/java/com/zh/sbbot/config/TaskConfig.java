package com.zh.sbbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class TaskConfig {
    @Bean(name = "taskSchedulerPool")
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(60);
        taskScheduler.setThreadNamePrefix("task-pool-");
        // 设置终止时等待最大时间，单位s，即在关闭时，需等待其他任务完成执行
        taskScheduler.setAwaitTerminationSeconds(3000);
        // 设置关机时是否等待计划任务完成，不中断正在运行的任务并执行队列中的所有任务，默认false
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        return taskScheduler;
    }
}