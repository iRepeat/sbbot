package com.zh.sbbot.utils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.*;

import java.io.ByteArrayOutputStream;

@Slf4j
public class CommandExecutor {


    @SneakyThrows
    public static String execute(String command, long timeout) {
        CommandLine commandLine = CommandLine.parse(command);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
        Executor executor = new DefaultExecutor();
        executor.setStreamHandler(streamHandler);

        // 设置Watchdog监视器以实施超时
        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
        executor.setWatchdog(watchdog);

        try {
            log.info("Executing command: " + commandLine);
            int exitValue = executor.execute(commandLine);
            if (exitValue == 0) {
                return outputStream.toString().trim();
            } else {
                throw new RuntimeException("Command failed with error: " + errorStream);
            }
        } catch (ExecuteException e) {
            if (watchdog.killedProcess()) {
                throw new RuntimeException("The process timeout and was killed.");
            }
            throw e;
        }
    }
}