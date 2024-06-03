package com.zh.sbbot.utils;

import com.mikuac.shiro.enums.MsgTypeEnum;
import com.mikuac.shiro.model.ArrayMsg;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * bot静态工具类
 */
public class BotUtil {

    /**
     * 提取消息参数
     */
    public static @Nullable String getParam(Matcher matcher) {
        return Optional.ofNullable(matcher.group(2)).map(String::trim).filter(StringUtils::isNotBlank).orElse(null);
    }


    /**
     * 提取消息中的文本。以“，”为分隔符
     *
     * @param arrayMsg 消息链
     * @return 消息文本
     */
    public static String getText(List<ArrayMsg> arrayMsg) {
        return getText(arrayMsg, "，");
    }

    /**
     * 提取消息中的文本。自定义分隔符
     *
     * @param arrayMsg 消息链
     * @return 消息文本
     */
    public static String getText(List<ArrayMsg> arrayMsg, String join) {
        return arrayMsg.stream()
                .filter(it -> MsgTypeEnum.text == it.getType())
                .map(it -> it.getData().get("text"))
                .collect(Collectors.joining(join)).trim();
    }

}

