package com.zh.sbbot.utils;

import lombok.SneakyThrows;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.ByteArrayOutputStream;

public class CommandExecutor {

    @SneakyThrows
    public static String execute(String command) {
        CommandLine commandLine = CommandLine.parse(command);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
        Executor executor = new DefaultExecutor();
        executor.setStreamHandler(streamHandler);

        int exitValue = executor.execute(commandLine);
        if (exitValue == 0) {
            return outputStream.toString().trim();
        } else {
            throw new RuntimeException("Command failed with error: " + errorStream);
        }
    }

}