package com.zh.sbbot.utils;

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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * bot静态工具类
 */
@Slf4j
public class BotUtil {


    /**
     * 提取消息参数
     */
    public static @Nullable String getParam(Matcher matcher) {
        return Optional.ofNullable(matcher).map(m -> m.group(2)).map(String::trim).filter(StringUtils::isNotBlank).orElse(null);
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
     * 由于当前Shiro框架不支持图片summary参数（会导致无法将`[CQ:image,summary=[xxx],file=xxx]`解析为ArrayMsg图片对象），因此有了这个适配方法。
     * <p>
     * [CQ:image,summary=[动画表情],file=xxx] => [CQ:image,file=xxx]
     */
    public static String adaptCQImage(String cqImage) {
        // 定义正则表达式，匹配 summary 字段及其内容
        String regex = "summary=\\[.*?\\],?";

        // 使用正则表达式替换 summary 字段
        String result = Pattern.compile(regex).matcher(cqImage).replaceAll("");

        // 去除可能多余的逗号
        return result.replaceAll(",]", "]").replaceAll("\\[,", "[");
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

