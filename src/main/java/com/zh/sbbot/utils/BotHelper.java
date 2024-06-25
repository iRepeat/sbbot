package com.zh.sbbot.utils;

import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.enums.MsgTypeEnum;
import com.mikuac.shiro.model.ArrayMsg;
import com.zh.sbbot.configs.SystemSetting;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * 结合了spring上下文的bot工具类
 */
@Component
@RequiredArgsConstructor
public class BotHelper {

    private final SystemSetting systemSetting;
    private final BotContainer botContainer;

    /**
     * 判断是否是超级用户
     *
     * @param userId 用户id
     * @return 是否是超级用户
     */
    public boolean isSuperUser(Long userId) {
        return Arrays.asList(systemSetting.getSuperUser()).contains(userId);
    }

    /**
     * 默认bot
     */
    public Bot defaultBot(){
        return botContainer.robots.get(systemSetting.getDefaultBot());
    }

    /**
     * 快速回复（文本）
     *
     * @param event 消息事件
     * @param text  回复内容
     */
    public void reply(AnyMessageEvent event, String text) {
        String msg = MsgUtils.builder()
                .reply(event.getMessageId())
                .text(text)
                .text("\n")
                .at(event.getUserId())
                .build();
        defaultBot().sendMsg(event, msg, false);
    }


    /**
     * 群消息快速回复（文本）
     *
     * @param event 消息事件
     * @param text  回复内容
     */
    public void replyForGroup(GroupMessageEvent event, String text) {
        String msg = MsgUtils.builder()
                .reply(event.getMessageId())
                .text(text)
                .text("\n")
                .at(event.getUserId())
                .build();
        defaultBot().sendGroupMsg(event.getGroupId(), event.getSender().getUserId(), msg, false);
    }


    /**
     * 由于当前lagrange框架无法发送某些域名的图片，因此有了这个适配方法。
     * <p>
     * 处理图片消息，将发送图片的方式由url转为base64
     *
     * @param arrayMsgList 包含“图片”类型的消息链
     * @return 原消息链
     */
    public List<ArrayMsg> adaptImgData(List<ArrayMsg> arrayMsgList) {
        String[] adaptImageHost = systemSetting.getAdaptImageHost();

        // 判断是否需要进行域名适配
        Predicate<String> needAdapt = url -> {
            try {
                URI uri = new URL(url).toURI();
                String domain = uri.getHost();
                return Arrays.asList(adaptImageHost).contains(domain);
            } catch (Exception e) {
                return false;
            }
        };

        arrayMsgList.forEach(arrayMsg -> {
            if (arrayMsg.getType().equals(MsgTypeEnum.image)
                    && StringUtils.isNotBlank(arrayMsg.getData().getOrDefault("file", null))) {
                String file = arrayMsg.getData().get("file");
                if (needAdapt.test(file)) {
                    if (file.startsWith("http://") || file.startsWith("https://")) {
                        String base64Image = DownloadUtil.downloadIntoMemory(file);
                        arrayMsg.getData().put("file", "base64://" + base64Image);
                    }
                }
            }
        });

        return arrayMsgList;
    }
}
