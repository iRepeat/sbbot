package com.zh.sbbot.plugins.ai;

import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.response.GetMsgResp;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.enums.AtEnum;
import com.mikuac.shiro.enums.MsgTypeEnum;
import com.zh.sbbot.annotations.Admin;
import com.zh.sbbot.constant.AdminMode;
import com.zh.sbbot.plugins.ai.dao.PluginAi;
import com.zh.sbbot.plugins.ai.dao.PluginAiRepository;
import com.zh.sbbot.plugins.ai.handler.AiHandler;
import com.zh.sbbot.plugins.ai.handler.AiHandlerSelector;
import com.zh.sbbot.plugins.ai.support.ChatResponse;
import com.zh.sbbot.utils.BotHelper;
import com.zh.sbbot.utils.BotUtil;
import com.zh.sbbot.utils.OCRUtil;
import com.zh.sbbot.utils.TTSUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static com.zh.sbbot.utils.BotUtil.getText;

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

    @GroupMessageHandler
    @MessageHandlerFilter(at = AtEnum.NEED)
    public void generateAnswer(GroupMessageEvent event, Bot bot) {
        Long groupId = event.getGroupId();

        // 读取当前群组的配置
        PluginAi pluginAi = pluginAiRepository.findOne(groupId);
        if (pluginAi == null) {
            log.error("AI配置未初始化：{}", groupId);
            return;
        }

        // 群AI已禁用
        if (Objects.equals(pluginAi.getIsDisable(), 1)) {
            botHelper.reply(event, "AI功能已关闭");
            return;
        }

        // 获取当前群组的AI服务
        AiHandler aiHandler = aiHandlerSelector.get(groupId);

        String text = getText(event.getArrayMsg());
        text += ShiroUtils.getMsgImgUrlList(event.getArrayMsg()).stream().map(ocrUtil::baidu).collect(Collectors.joining(
                "\n"));
        log.info("问题：{} ", text);

        // 不允许问题为空
        if (StringUtils.isBlank(text)) {
            botHelper.reply(event, "?");
            return;
        }

        ChatResponse response;
        if (text.startsWith("##")) {
            // 进行单次对话（不携带上下文，不会保留历史）
            response = aiHandler.generateAnswer(pluginAi, text);
            log.info("AI： {}", response);
        } else {
            // 当前用户的会话ID
            String conversationId = groupId + "::" + event.getUserId();

            try {
                // 如果是回复机器人信息，则使用机器人所回复的用户的上下文
                if (BotUtil.isReplyMe(event)) {
                    ActionData<GetMsgResp> msgId =
                            bot.getMsg(Integer.parseInt(event.getArrayMsg().stream().filter(it -> it.getType() == MsgTypeEnum.reply).findFirst().orElseThrow().getData().get("id")));
                    String qq =
                            BotUtil.parseArrayMsg(msgId.getData().getMessage()).stream().filter(it -> it.getType() == MsgTypeEnum.at).findFirst().orElseThrow().getData().get("qq");
                    conversationId = groupId + "::" + qq;
                    log.info("临时切换上下文：{} -> {}", event.getUserId(), qq);
                }
            } catch (Exception ignored) {
            }


            // 清除当前用户上下文
            if (text.startsWith("!!") || text.startsWith("！！")) {
                aiHandler.clear(conversationId);
                text = text.substring(2);
            }

            // 生成AI回复
            response = aiHandler.generateAnswer(pluginAi, text, conversationId);
            log.info("AI： {}", response);
        }

        botHelper.reply(event, response.getResult());
        if (Objects.equals(pluginAi.getTts(), 1)) {
            String base64 = ttsUtil.generateToBase64(response.getResult());
            bot.sendGroupMsg(groupId, ArrayMsgUtils.builder().voice("base64://" + base64).build(), false);
        }

        if (response.isClearHistory()) {
            botHelper.reply(event, "当前会话已结束，原因：" + response.getClearReason());
        }
    }

    @Admin(mode = AdminMode.GROUP_ADMIN)
    @GroupMessageHandler
    @MessageHandlerFilter(startWith = ".ai", at = AtEnum.NOT_NEED)
    public void manage(GroupMessageEvent event, Matcher matcher) {

        String usage = """
                Usage:
                - reset: 重置AI配置表
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
                pluginAiRepository.initTable();
                botHelper.reply(event, "初始化表成功");
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