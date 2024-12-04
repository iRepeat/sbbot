package com.zh.sbbot.plugin.watcher;


import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Order;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.enums.AtEnum;
import com.mikuac.shiro.enums.MsgTypeEnum;
import com.mikuac.shiro.model.ArrayMsg;
import com.zh.sbbot.custom.Admin;
import com.zh.sbbot.constant.DictKey;
import com.zh.sbbot.plugin.ai.dao.PluginAi;
import com.zh.sbbot.plugin.ai.dao.PluginAiRepository;
import com.zh.sbbot.plugin.ai.handler.AiHandler;
import com.zh.sbbot.plugin.ai.handler.AiHandlerSelector;
import com.zh.sbbot.plugin.ai.support.ChatResponse;
import com.zh.sbbot.repository.DictRepository;
import com.zh.sbbot.util.BotHelper;
import com.zh.sbbot.util.BotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;

import static com.mikuac.shiro.enums.MsgTypeEnum.*;

/**
 * 监听全部消息。
 * 一般只在命中规则时执行操作（如复读）
 */
@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class MessageWatcherPlugin {

    private final DictRepository dictRepository;
    private final AiHandlerSelector aiHandlerSelector;
    private final PluginAiRepository pluginAiRepository;
    private final BotHelper botHelper;

    /**
     * 存储私聊/群聊的复读模式
     */
    private final Map<String, EchoMode> echoModeConfig = new HashMap<>();

    /*
     * AI复读
     */
    @GroupMessageHandler
    @MessageHandlerFilter(at = AtEnum.NOT_NEED)
    public void repeatByAi(GroupMessageEvent event, Bot bot) {
        String[] list = dictRepository.get(DictKey.PLUGIN_REPEAT_MSG_RULE, String.class).split(",");
        List<ArrayMsg> msg = event.getArrayMsg();
        for (int i = msg.size() - 1; i >= 0; i--) {
            ArrayMsg arrayMsg = msg.get(i);
            if (arrayMsg.getType().equals(text)) {
                for (String word : list) {
                    if (arrayMsg.getData().get("text").endsWith(word)) {
                        AiHandler defaultHandler = aiHandlerSelector.getDefault();
                        // 读取当前群组的配置。如果群聊没有配置，则使用默认配置
                        PluginAi pluginAi = Optional.ofNullable(pluginAiRepository.findOne(event.getGroupId()))
                                .orElse(PluginAi.defaultConfig(event.getGroupId(), defaultHandler.vendor(), defaultHandler.defaultModel()));
                        if (Objects.equals(pluginAi.getIsDisable(), 0)) {
                            pluginAi.setSystemTemplate("你的职能是为用户的消息添加emoji，直接回复添加emoji后的消息即可，不要做任何解释");
                            ChatResponse chatResponse = aiHandlerSelector.get(pluginAi.getVendor()).chat(pluginAi
                                    , "用户的消息是：" + BotUtil.getText(msg));
                            bot.sendGroupMsg(event.getGroupId(), chatResponse.getResult(), false);
                        }
                        return;
                    }
                }
            }
        }
    }

    /*
     * 单表情复读
     */
    @GroupMessageHandler
    @MessageHandlerFilter(at = AtEnum.NOT_NEED)
    public void repeatEmoji(GroupMessageEvent event, Bot bot) {
        List<MsgTypeEnum> typeEnums = event.getArrayMsg().stream().map(ArrayMsg::getType).distinct().toList();
        if (typeEnums.size() == 1 && Arrays.asList(face, dice, new_dice, basketball, rps, new_rps, mface, marketface, at)
                .contains(typeEnums.get(0))) {
            bot.sendGroupMsg(event.getGroupId(), event.getArrayMsg(), false);
        }
    }


    /*
     * 复读模式
     */
    @Order(-1)
    @AnyMessageHandler
    @Admin
    @MessageHandlerFilter(at = AtEnum.NOT_NEED, startWith = StringUtils.EMPTY)
    public void echo(AnyMessageEvent event, Bot bot, Matcher matcher) {
        Optional.ofNullable(BotUtil.getParam(matcher, 0)).ifPresent(s -> {
            String key = event.getMessageType() + event.getUserId() + event.getGroupId();

            switch (s) {
                case ".echo plain" -> {
                    echoModeConfig.put(key, EchoMode.PLAIN);
                    botHelper.reply(event, "开启复读模式（PLAIN）");
                }
                case ".echo", ".echo on", ".echo keep as is" -> {
                    echoModeConfig.put(key, EchoMode.KEEP_AS_IS);
                    botHelper.reply(event, "开启复读模式");
                }
                case ".echo off" -> {
                    echoModeConfig.put(key, EchoMode.OFF);
                    botHelper.reply(event, "关闭复读模式");
                }
                default -> {
                    EchoMode echoMode = echoModeConfig.getOrDefault(key, EchoMode.OFF);
                    switch (echoMode) {
                        case PLAIN -> {
                            bot.sendMsg(event, ShiroUtils.unescape(s), true);
                        }
                        case KEEP_AS_IS -> {
                            List<ArrayMsg> msgList = ShiroUtils.rawToArrayMsg(ShiroUtils.unescape(s));
                            bot.sendMsg(event, msgList, false);
                        }
                        case OFF -> {
                        }
                        default -> {
                        }
                    }
                }
            }

        });
    }

    /**
     * 复读模式
     */
    enum EchoMode {
        /**
         * 以纯文本方式复读
         */
        PLAIN,
        /**
         * 保持原样复读
         */
        KEEP_AS_IS,
        /**
         * 关闭复读
         */
        OFF
    }

}
