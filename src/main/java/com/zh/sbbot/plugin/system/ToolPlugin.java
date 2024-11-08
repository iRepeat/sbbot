package com.zh.sbbot.plugin.system;

import com.alibaba.fastjson2.JSONObject;
import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.enums.AtEnum;
import com.mikuac.shiro.enums.MsgTypeEnum;
import com.mikuac.shiro.model.ArrayMsg;
import com.zh.sbbot.annotation.Admin;
import com.zh.sbbot.constant.AdminMode;
import com.zh.sbbot.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * 封装一些交互式的命令。
 * <p>
 * 大部分仅允许管理员使用，少部分可由普通用户触发
 */
@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class ToolPlugin {

    private final TTSUtil ttsUtil;
    private final BotHelper botHelper;

    /**
     * 让bot重复你的话
     */
    @AnyMessageHandler
    @Admin(mode = AdminMode.GROUP_ADMIN)
    @MessageHandlerFilter(startWith = ".say", at = AtEnum.NOT_NEED)
    public void say(Bot bot, AnyMessageEvent event, Matcher matcher) {
        Optional.ofNullable(BotUtil.getParam(matcher)).ifPresent(s -> {
            switch (BotUtil.getCommandParam(matcher)) {
                case "plain" -> {
                    // 文本形式发送，媒体信息或表情等会转译为CQ码
                    bot.sendMsg(event, ShiroUtils.unescape(s), true);
                }
                case "json" -> {
                    // JSON卡片
                    List<ArrayMsg> json = ArrayMsgUtils.builder().json(ShiroUtils.unescape(s)).build();
                    bot.sendMsg(event, json, false);
                }
                default -> {
                    // 原样复读
                    List<ArrayMsg> msgList = ShiroUtils.rawToArrayMsg(ShiroUtils.unescape(s));
                    bot.sendMsg(event, msgList, false);
                }
            }
        });
    }

    /**
     * 执行拓展API
     */
    @AnyMessageHandler
    @Admin(mode = AdminMode.GROUP_ADMIN)
    @MessageHandlerFilter(startWith = ".custom", at = AtEnum.NOT_NEED)
    public void sendCustomRequest(AnyMessageEvent event, Matcher matcher) {
        Optional.ofNullable(BotUtil.getParam(matcher)).ifPresent(s -> {
            int i = s.indexOf(' ');
            if (i > -1) {
                String key = ShiroUtils.unescape(s.substring(0, i).trim());
                String value = ShiroUtils.unescape(s.substring(i + 1).trim());
                ActionData<?> actionData = botHelper.sendCustomRequest(key, value);
                botHelper.sendEmojiLike(String.valueOf(event.getMessageId()), "124");
                Optional.ofNullable(actionData)
                        .map(ActionData::getData)
                        .map(JSONObject::toJSONString)
                        .ifPresent(data -> botHelper.reply(event, data));
            }
        });
    }


    /**
     * 执行系统shell命令
     */
    @AnyMessageHandler
    @Admin
    @MessageHandlerFilter(startWith = ".exec", at = AtEnum.NOT_NEED)
    public void exec(AnyMessageEvent event, Bot bot, Matcher matcher) {
        String param = BotUtil.getParam(matcher);
        if (StringUtils.isBlank(param)) {
            botHelper.reply(event, "请给定命令");
            return;
        }

        // 执行命令
        String result = CommandExecutor.execute(ShiroUtils.unescape(param), 30000);
        // 表情回应
        botHelper.sendEmojiLike(String.valueOf(event.getMessageId()), "124");

        if (StringUtils.isBlank(result)) {
            log.info("（命令返回空）");
            return;
        }
        // 发送json卡片
        if (result.trim().startsWith("{\"app\"")) {
            log.info("发送json数据：{}", result);
            bot.sendMsg(event, ArrayMsgUtils.builder().json(result).build(), false);
            return;
        }
        // 语音消息无法被引用，因此不能使用reply
        if (result.contains("[CQ:record") || result.contains("[CQ:video")) {
            bot.sendMsg(event, result, false);
            return;
        }
        // 生成tts并发送
        if (BotUtil.getCommandParam(matcher).equals("tts")) {
            String voice = ttsUtil.generate(result);
            bot.sendMsg(event, ArrayMsgUtils.builder().voice(voice).build(), false);
            return;
        }

        botHelper.reply(event, result);

    }


    /**
     * 设置头像
     */
    @AnyMessageHandler
    @Admin(mode = AdminMode.GROUP_ADMIN)
    @MessageHandlerFilter(startWith = ".avatar", at = AtEnum.NOT_NEED)
    public void avatar(AnyMessageEvent event, Matcher matcher) {
        String image = ShiroUtils.getMsgImgUrlList(event.getArrayMsg()).stream()
                .findFirst().or(() -> Optional.ofNullable(BotUtil.getParam(matcher))).orElse(null);
        Assert.hasText(image, "头像（图片或URL）为空！");
        if (StringUtils.isNoneBlank(image)) {
            String b64 = null;
            try {
                b64 = DownloadUtil.downloadIntoMemory(image);
            } catch (Exception e) {
                log.error("头像下载失败");
            }
            if (StringUtils.isNotBlank(b64)) {
                if (BotUtil.getCommandParam(matcher).equals("group")) {
                    Assert.notNull(event.getGroupId(), "cannot change group avatar, group id is null");
                    // 更换群头像
                    botHelper.setGroupAvatar("base64://" + b64, event.getGroupId());
                } else {
                    // 更换机器人头像
                    botHelper.setSelfAvatar("base64://" + b64);
                }
                log.info("头像已更换：{}", image);
            }
        }
    }

    /**
     * 获取消息ID
     */
    @Admin(mode = AdminMode.GROUP_ADMIN)
    @AnyMessageHandler
    @MessageHandlerFilter(endWith = ".msg_id", types = {MsgTypeEnum.reply}, at = AtEnum.NOT_NEED)
    public void getMsgId(AnyMessageEvent event, Bot bot) {
        String msgId = event.getArrayMsg()
                .stream()
                .filter(it -> it.getType() == MsgTypeEnum.reply)
                .findFirst()
                .orElseThrow()
                .getData()
                .get("id");
        botHelper.reply(event, msgId);
    }

    /**
     * 解码/编码base64
     * 【】表示编码，】【表示解码
     */
    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {"】", "【"}, endWith = {"】", "【"})
    public void base64(AnyMessageEvent event) {
        String param = BotUtil.base64(ShiroUtils.unescape(event.getMessage()));
        botHelper.reply(event, param, true);
    }


    /**
     * 发送表情回复
     */
    @AnyMessageHandler
    @MessageHandlerFilter(startWith = ".emoji")
    public void emojiLike(AnyMessageEvent event) {
        event.getArrayMsg().stream()
                .filter(m -> m.getType() == MsgTypeEnum.face)
                .map(ArrayMsg::getData)
                .map(m -> m.getOrDefault("id", null))
                .forEach(m -> {
                    botHelper.sendEmojiLike(String.valueOf(event.getMessageId()), m);
                });
    }


}