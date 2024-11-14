package com.zh.sbbot.plugin.ai;

import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.response.GetMsgResp;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.enums.AtEnum;
import com.mikuac.shiro.enums.MsgTypeEnum;
import com.zh.sbbot.annotation.Admin;
import com.zh.sbbot.config.QQTTSConfig;
import com.zh.sbbot.constant.AdminMode;
import com.zh.sbbot.plugin.ai.dao.PluginAi;
import com.zh.sbbot.plugin.ai.dao.PluginAiRepository;
import com.zh.sbbot.plugin.ai.handler.AiHandler;
import com.zh.sbbot.plugin.ai.handler.AiHandlerSelector;
import com.zh.sbbot.plugin.ai.support.ChatResponse;
import com.zh.sbbot.repository.DictRepository;
import com.zh.sbbot.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.zh.sbbot.util.BotUtil.getText;

@Shiro
@Component
@Slf4j
@SuppressWarnings("unused")
@RequiredArgsConstructor
@Transactional
public class AiPlugin {
    private final AiHandlerSelector aiHandlerSelector;
    private final PluginAiRepository pluginAiRepository;
    private final BotHelper botHelper;
    private final TTSUtil ttsUtil;
    private final OCRUtil ocrUtil;
    private final DictRepository dictRepository;
    private final QQTTSConfig config;

    /**
     * 获取用户上下文标识
     */
    private static @NotNull String getConversationId(GroupMessageEvent event, Bot bot, Long groupId) {
        // 当前用户的会话ID
        String conversationId = groupId + "::" + event.getUserId();

        try {
            // 如果是回复机器人信息，则使用机器人所回复的用户的上下文
            ActionData<GetMsgResp> msgId = bot.getMsg(Integer.parseInt(event.getArrayMsg()
                    .stream()
                    .filter(it -> it.getType() == MsgTypeEnum.reply)
                    .findFirst()
                    .orElseThrow()
                    .getData()
                    .get("id")));
            String qq = BotUtil.parseArrayMsg(msgId.getData().getMessage())
                    .stream()
                    .filter(it -> it.getType() == MsgTypeEnum.at)
                    .findFirst()
                    .orElseThrow()
                    .getData()
                    .get("qq");
            conversationId = event.getGroupId() + "::" + qq;
            log.info("临时切换上下文：{} -> {}", event.getUserId(), qq);
        } catch (Exception ignored) {
        }
        return conversationId;
    }

    @GroupMessageHandler
    @MessageHandlerFilter(at = AtEnum.NEED)
    public void generateAnswer(GroupMessageEvent event, Bot bot) {
        Long groupId = event.getGroupId();

        // 读取当前群组的配置
        PluginAi pluginAi = getAiOption(event, groupId);
        if (pluginAi == null) return;

        // 获取当前群组的AI服务
        AiHandler aiHandler = aiHandlerSelector.get(groupId);

        // 解析消息
        String text = parseText(event);

        // 解析消息中的图片，转为base64
        Function<String, String> urlToBase64 = url -> {
            try {
                String b64 = DownloadUtil.downloadIntoMemory(url);
                return "data:image/jpeg;base64," + b64;
            } catch (Exception ignored) {
                log.info("图片下载失败：{}", url);
            }
            return StringUtils.EMPTY;
        };
        List<String> images = ShiroUtils.getMsgImgUrlList(event.getArrayMsg()).stream().map(urlToBase64).filter(StringUtils::isNoneBlank).toList();

        // 不允许文字和图片同时为空
        if (StringUtils.isBlank(text) && CollectionUtils.isEmpty(images)) {
            botHelper.reply(event, "?");
            return;
        }

        if (CollectionUtils.isEmpty(images)) {
            log.info("问题：{} ", text);
        } else {
            log.info("问题：{}，图片：{}", text, images);
        }

        ChatResponse response;
        if (text.startsWith("##")) {
            // 进行单次对话（不携带上下文，不会保留历史）
            response = aiHandler.chat(pluginAi, text, images);
        } else {
            // 获取当前用户的会话ID
            String conversationId = getConversationId(event, bot, groupId);
            // 清除当前用户上下文
            if (text.startsWith("!!") || text.startsWith("！！")) {
                aiHandler.clear(conversationId);
                text = text.substring(2);
            }
            // 生成AI回复
            response = aiHandler.chat(pluginAi, text, images, conversationId);
        }

        log.info("AI： {}", response);
        // 发送消息
        botHelper.reply(event, response.getResult());

        // TTS回复
        if (Objects.equals(pluginAi.getTts(), 1)) {
            botHelper.sendGroupAiRecord(groupId, config.getCharacter(), response.getResult());
        }

        if (response.isClearHistory()) {
            botHelper.reply(event, "当前会话已结束，原因：" + response.getClearReason());
        }
    }

    /**
     * 读取当前群组AI配置
     */
    private @Nullable PluginAi getAiOption(GroupMessageEvent event, Long groupId) {
        PluginAi pluginAi = pluginAiRepository.findOne(groupId);
        if (pluginAi == null) {
            log.error("AI配置未初始化：{}", groupId);
            return null;
        }

        // 群AI已禁用
        if (Objects.equals(pluginAi.getIsDisable(), 1)) {
            botHelper.reply(event, "AI功能已关闭");
            return null;
        }
        return pluginAi;
    }

    /**
     * 解析消息内容
     * 1. 对图片进行OCR文本提取
     * 2. 将``包裹的消息内容作为系统命令执行，并替换为执行结果
     */
    private String parseText(GroupMessageEvent event) {
        String text = getText(event.getArrayMsg());
        Matcher execMatcher = Pattern.compile("`(.*?)`").matcher(text);

        if (execMatcher.find()) {
            String cmd = execMatcher.group(1);
            try {
                if (!botHelper.isSuperUser(event.getUserId()))
                    throw new RuntimeException("非SU不能执行命令！");
                String result = CommandExecutor.execute(cmd, 10 * 1000);
                text = text.replace(execMatcher.group(0), result);
            } catch (Exception e) {
                botHelper.reply(event, "命令执行失败：【%s】 => 【%s】".formatted(cmd, e.getMessage()));
                return StringUtils.EMPTY;
            }
        }
        return text;
    }

    @Admin(mode = AdminMode.GROUP_ADMIN)
    @GroupMessageHandler
    @MessageHandlerFilter(startWith = ".ai", at = AtEnum.NOT_NEED)
    public void manage(GroupMessageEvent event, Matcher matcher) {

        String usage = """
                Usage:
                - reset: 重置AI配置表（仅SU可用）
                - init: 初始化当前群组的AI配置
                - disable: 禁用群组的AI功能
                - enable: 启用群组的AI功能
                - get: 获取群组的AI配置
                - set [字段] [值]: 更新群组的AI配置
                - !! or ！！: 清除群组的AI历史记录
                - service [AI服务名]: 切换群组的AI服务""";

        String param = BotUtil.getParam(matcher);
        if (StringUtils.isBlank(param)) {
            botHelper.reply(event, usage);
            return;
        }

        // 群号从事件中获取
        Long groupId = event.getGroupId();

        String[] split = ShiroUtils.unescape(param).split(" ");
        String action = split[0].toLowerCase();
        switch (action) {
            case "reset":
                if (botHelper.isSuperUser(event.getUserId())) {
                    pluginAiRepository.resetTable();
                    botHelper.reply(event, "初始化表成功");
                }
                break;
            case "init":
                AiHandler defaultAIHandler = aiHandlerSelector.getDefault();
                aiHandlerSelector.set(groupId, defaultAIHandler.vendor());
                pluginAiRepository.init(groupId, defaultAIHandler.defaultModel(), defaultAIHandler.vendor());
                botHelper.reply(event, "初始化配置成功：%s，平台：%s，模型：%s".formatted(groupId,
                        defaultAIHandler.vendor(), defaultAIHandler.defaultModel()));
                break;
            case "disable":
                pluginAiRepository.disable(groupId);
                botHelper.reply(event, "禁用成功：" + groupId);
                break;
            case "enable":
                pluginAiRepository.enable(groupId);
                botHelper.reply(event, "启用成功：" + groupId);
                break;
            case "get":
                PluginAi pluginAi = pluginAiRepository.findOne(groupId);
                if (pluginAi == null) {
                    botHelper.reply(event, "AI配置未初始化");
                } else {
                    botHelper.reply(event, "当前AI配置：%s".formatted(pluginAi));
                }
                break;
            case "set":
                int i = pluginAiRepository.update(split[1], split[2], groupId);
                if (split[1].equals("vendor")) aiHandlerSelector.set(groupId, split[2]);
                PluginAi config = pluginAiRepository.findOne(groupId);
                botHelper.reply(event, "配置更新成功，影响行数：%s！当前AI配置：%s".formatted(i, config));
                break;
            case "!!":
            case "！！":
                aiHandlerSelector.get(groupId).clearByPrefix(groupId.toString());
                botHelper.reply(event, "记忆清除成功: " + groupId);
                break;
            case "service":
                // 切换当前群聊AI
                AiHandler aiHandler = aiHandlerSelector.set(groupId, split[1]);
                pluginAiRepository.switchAi(groupId, aiHandler.vendor(), aiHandler.defaultModel());
                aiHandler.clearByPrefix(groupId.toString());
                botHelper.reply(event, "群聊AI已切换：%s，模型：%s".formatted(aiHandler.vendor(), aiHandler.defaultModel()));
                break;
            default:
                botHelper.reply(event, "参数错误：“%s”\n%s".formatted(action, usage));
                break;
        }

    }


}