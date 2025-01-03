package com.zh.sbbot.util;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Arrays;
import java.util.Optional;

public class ExceptionUtil {
    public static String getStackTrace(Throwable e, String packagePrefix) {
        return Optional.ofNullable(e)
                .map(Throwable::getStackTrace)
                .flatMap(stackTrace -> Arrays.stream(stackTrace)
                        .filter(stack -> stack.getClassName().startsWith(packagePrefix))
                        .findFirst())
                .map(stackTraceElement -> String.format("%s.%s(%s:%d)",
                        stackTraceElement.getClassName(),
                        stackTraceElement.getMethodName(),
                        stackTraceElement.getFileName(),
                        stackTraceElement.getLineNumber()))
                .orElseGet(() -> e == null ? "请检查日志" : e.getClass().getSimpleName());
    }

    public static String getStackTrace(Throwable e) {
        return ExceptionUtils.getStackTrace(e);
    }
}
