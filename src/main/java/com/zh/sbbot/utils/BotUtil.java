package com.zh.sbbot.utils;

import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
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

    /**
     * 由于当前lagrange框架无法发送某些域名的图片，因此有了这个适配方法。
     * <p>
     * 处理图片消息，将发送图片的方式由url转为base64
     *
     * @param arrayMsgList 包含“图片”类型的消息链
     * @return 原消息链
     */
    public static List<ArrayMsg> adaptImgData(List<ArrayMsg> arrayMsgList) {
        arrayMsgList.forEach(arrayMsg -> {
            if (arrayMsg.getType().equals(MsgTypeEnum.image)
                    && StringUtils.isNotBlank(arrayMsg.getData().getOrDefault("file", null))) {
                String file = arrayMsg.getData().get("file");
                if (file.startsWith("http://") || file.startsWith("https://")) {
                    String base64Image = DownloadUtil.downloadIntoMemory(file);
                    arrayMsg.getData().put("file", "base64://" + base64Image);
                }
            }

        });
        return arrayMsgList;
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
}

