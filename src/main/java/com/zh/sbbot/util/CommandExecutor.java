package com.zh.sbbot.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CommandExecutor {


    @SneakyThrows
    public static String execute(String command, long timeout) {
        ProcessBuilder processBuilder = new ProcessBuilder(List.of("sh", "-c", command));
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        if (!process.waitFor(timeout, TimeUnit.MILLISECONDS)) {
            process.destroy();
            throw new RuntimeException("Command timed out");
        }

        int exitValue = process.exitValue();
        if (exitValue == 0) {
            return IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8).trim();
        } else {
            throw new RuntimeException("Command failed [" + exitValue + "] with error: " + IOUtils.toString(process.getErrorStream(),
                    StandardCharsets.UTF_8));
        }
    }


}