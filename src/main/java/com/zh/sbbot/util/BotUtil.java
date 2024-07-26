package com.zh.sbbot.util;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.enums.MsgTypeEnum;
import com.mikuac.shiro.model.ArrayMsg;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * bot静态工具类
 */
@Slf4j
public class BotUtil {


    /**
     * 提取消息参数，解密base64
     */
    public static @Nullable String getParam(Matcher matcher) {
        return getParam(matcher, 2);
    }

    /**
     * 提取消息参数，解密base64
     */
    public static @Nullable String getParam(Matcher matcher, int groupNo) {
        String param = Optional.ofNullable(matcher).map(m -> m.group(groupNo)).map(String::trim).filter(StringUtils::isNotBlank).orElse(null);
        return base64(param);
    }

    /**
     * 消息base64编码/解码
     */
    public static String base64(String param) {
        if (param != null) {
            if (param.endsWith("【") && param.startsWith("】")) {
                return new String(Base64.getDecoder().decode(param.replaceAll("【", "").replaceAll("】", ""))).trim();
            } else if (param.startsWith("【") && param.endsWith("】")) {
                return new String(Base64.getEncoder().encode(param.replaceAll("【", "").replaceAll("】", "").getBytes())).trim();
            }
        }
        return param;
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

    /**
     * 判断是否是回复机器人
     */
    public static boolean isReplyMe(GroupMessageEvent event) {
        List<ArrayMsg> arrayMsg = event.getArrayMsg();
        return arrayMsg.stream().anyMatch(msg ->
                msg.getType().equals(MsgTypeEnum.at)
                        && msg.getData().get("qq").equals(event.getSelfId().toString())
                        && arrayMsg.stream().anyMatch(it -> it.getType() == MsgTypeEnum.reply));
    }

    /**
     * String -> List<ArrayMsg>
     */
    public static List<ArrayMsg> parseArrayMsg(String message) {
        try {
            return JSONObject.parseObject(message, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.debug("error to parse message: {}", message, e);
            return Collections.emptyList();
        }
    }

    /**
     * GroupMessageEvent -> AnyMessageEvent
     */
    public static AnyMessageEvent castToAnyMessageEvent(GroupMessageEvent event) {
        AnyMessageEvent target = new AnyMessageEvent();
        BeanUtils.copyProperties(event, target);
        return target;
    }
}

