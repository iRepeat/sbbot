package com.zh.sbbot.plugins.ai;

import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.enums.AtEnum;
import com.zh.sbbot.annotations.Admin;
import com.zh.sbbot.constant.DictKey;
import com.zh.sbbot.plugins.ai.dao.PluginAi;
import com.zh.sbbot.plugins.ai.dao.PluginAiRepository;
import com.zh.sbbot.plugins.ai.handler.AiHandler;
import com.zh.sbbot.plugins.ai.handler.AiHandlerSelector;
import com.zh.sbbot.repository.DictRepository;
import com.zh.sbbot.utils.BotHelper;
import com.zh.sbbot.utils.BotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.regex.Matcher;

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
    private final DictRepository dictRepository;
    private final BotHelper botHelper;


    @GroupMessageHandler
    @MessageHandlerFilter(at = AtEnum.NEED)
    public void generateAnswer(GroupMessageEvent event) {
        Long groupId = event.getGroupId();
        PluginAi pluginAi = pluginAiRepository.findOne(groupId);

        if (pluginAi == null) {
            botHelper.replyForGroup(event, "AI配置未初始化");
            return;
        }

        // 如果配置不存在，则初始化配置
        if (Objects.equals(pluginAi.getIsDisable(), 1)) {
            botHelper.replyForGroup(event, "AI功能已关闭");
            return;
        }

        String conversationId = groupId + "::" + event.getUserId();

        String text = getText(event.getArrayMsg());
        log.info("问题：{}", text);

        // 清除上下文
        if (text.startsWith("!!") || text.startsWith("！！")) {
            aiHandlerSelector.getAiService().clear(conversationId);
            text = text.substring(2);
        }

        String answer = aiHandlerSelector.getAiService().generateAnswer(pluginAi, text,
                conversationId);
        log.info("AI： {}", answer);

        botHelper.replyForGroup(event, answer);
    }

    @Admin
    @GroupMessageHandler
    @MessageHandlerFilter(startWith = ".ai", at = AtEnum.NOT_NEED)
    public void manage(GroupMessageEvent event, Matcher matcher) {

        String usage = """
                Usage:
                - init: 初始化群组的AI功能配置
                - init 0: 初始化AI功能配置表
                - disable: 禁用群组的AI功能
                - enable: 启用群组的AI功能
                - get: 获取当前群组的AI配置信息
                - !! or ！！: 清除指定群组的AI历史记录
                - service [AI服务名]: 切换当前群组的AI服务""";

        String param = BotUtil.getParam(matcher);
        if (StringUtils.isBlank(param)) {
            botHelper.replyForGroup(event, usage);
            return;
        }

        // 群号从事件中获取
        Long groupId = event.getGroupId();

        String[] split = param.split(" ");
        String action = split[0].toLowerCase();
        switch (action) {
            case "init":
                if (split.length == 2 && split[1].equals("0")) {
                    pluginAiRepository.initTable();
                    botHelper.replyForGroup(event, "初始化表成功");
                }
                pluginAiRepository.init(groupId, aiHandlerSelector.getAiService().defaultModel());
                botHelper.replyForGroup(event, "初始化配置成功：" + groupId);
                break;
            case "disable":
                pluginAiRepository.disable(groupId);
                botHelper.replyForGroup(event, "禁用成功：" + groupId);
                break;
            case "enable":
                pluginAiRepository.enable(groupId);
                botHelper.replyForGroup(event, "启用成功：" + groupId);
                break;
            case "get":
                PluginAi pluginAi = pluginAiRepository.findOne(groupId);
                if (pluginAi == null) {
                    botHelper.replyForGroup(event, "AI功能未初始化");
                } else {
                    String value = dictRepository.getValue(DictKey.PLUGIN_AI_USE);
                    botHelper.replyForGroup(event, "当前AI：%s，配置：%s".formatted(value, pluginAi));
                }
                break;
            case "!!": case "！！":
                aiHandlerSelector.getAiService().clearByPrefix(groupId.toString());
                botHelper.replyForGroup(event, "记忆清除成功: " + groupId);
                break;
            case "service":
                dictRepository.setValue(DictKey.PLUGIN_AI_USE, split[1]);
                // 切换当前群聊默认模型
                AiHandler aiService = aiHandlerSelector.getAiService();
                pluginAiRepository.switchModel(groupId, aiService.defaultModel());
                botHelper.replyForGroup(event, "群聊AI已切换：%s，模型：%s".formatted(aiService.vendor(), aiService.defaultModel()));
                break;
            default:
                botHelper.replyForGroup(event, "参数错误：“%s”\n%s".formatted(action, usage));
                break;
        }

    }


}