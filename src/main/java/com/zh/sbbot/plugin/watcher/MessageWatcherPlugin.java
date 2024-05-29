package com.zh.sbbot.plugin.watcher;


import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.enums.AtEnum;
import com.mikuac.shiro.enums.MsgTypeEnum;
import com.mikuac.shiro.model.ArrayMsg;
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
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
                            ChatResponse chatResponse = aiHandlerSelector.get(pluginAi.getVendor()).generateAnswer(pluginAi
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

}
