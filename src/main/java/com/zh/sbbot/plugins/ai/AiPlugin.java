package com.zh.sbbot.plugins.ai;

import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.enums.AtEnum;
import com.zh.sbbot.annotations.Admin;
import com.zh.sbbot.utils.BotHelper;
import com.zh.sbbot.utils.BotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.regex.Matcher;

import static com.zh.sbbot.utils.BotUtil.getText;

@Shiro
@Component
@Slf4j
@SuppressWarnings("unused")
@RequiredArgsConstructor
public class AiPlugin {
    private final AiService aiService;
    private final BotHelper botHelper;


    @GroupMessageHandler
    @MessageHandlerFilter(at = AtEnum.NEED)
    public void generateAnswer(GroupMessageEvent event) {
        Long groupId = event.getGroupId();
        AiModel aiConfig = aiService.findOne(groupId);

        // 如果配置不存在，则初始化配置
        if (Objects.equals(aiConfig.getIsDisable(), 1)) {
            botHelper.replyForGroup(event, "AI功能已关闭");
            return;
        }

        String text = getText(event.getArrayMsg());
        log.info("问题：{}", text);

        String answer = aiService.getAnswer(aiConfig, text);
        log.info("AI： {}", answer);

        botHelper.replyForGroup(event, answer);
    }

    @Admin
    @AnyMessageHandler
    @MessageHandlerFilter(startWith = ".ai", at = AtEnum.NOT_NEED)
    public void manage(AnyMessageEvent event, Matcher matcher) {
        String param = BotUtil.getParam(matcher);
        if (StringUtils.isBlank(param)) {
            botHelper.reply(event, "请输入参数");
            return;
        }

        // 群号可以由参数指定或者从事件中获取
        Long groupId;
        String[] split = param.split(" ");
        if (split.length < 2) {
            if (event.getGroupId() == null) {
                botHelper.reply(event, "参数错误");
                return;
            }
            groupId = event.getGroupId();
        } else {
            try {
                groupId = Long.parseLong(split[1]);
            } catch (NumberFormatException e) {
                botHelper.reply(event, "群号格式错误");
                return;
            }
        }

        String action = split[0].toLowerCase();
        switch (action) {
            case "init":
                aiService.init(groupId);
                botHelper.reply(event, "初始化成功：" + groupId);
                break;
            case "disable":
                aiService.disable(groupId);
                botHelper.reply(event, "禁用成功：" + groupId);
                break;
            case "enable":
                aiService.enable(groupId);
                botHelper.reply(event, "启用成功：" + groupId);
                break;
            case "get":
                AiModel aiConfig = aiService.findOne(groupId);
                if (aiConfig == null) {
                    botHelper.reply(event, "AI功能未初始化");
                } else {
                    botHelper.reply(event, "AI配置：" + aiConfig);
                }
                break;
            default:
                botHelper.reply(event, "未知操作：" + action);
                break;
        }

    }


}