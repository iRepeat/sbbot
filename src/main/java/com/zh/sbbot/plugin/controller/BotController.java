package com.zh.sbbot.plugin.controller;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.mikuac.shiro.common.utils.EventUtils;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.common.ActionList;
import com.mikuac.shiro.dto.action.common.MsgId;
import com.mikuac.shiro.dto.action.response.FriendInfoResp;
import com.mikuac.shiro.dto.action.response.GroupInfoResp;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.handler.injection.InjectionHandler;
import com.zh.sbbot.util.AnnotationHandlerContainer;
import com.zh.sbbot.util.BotHelper;
import com.zh.sbbot.util.BotUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping()
public class BotController {
    private static final Logger log = LoggerFactory.getLogger(BotController.class);
    private final InjectionHandler injectionHandler;
    private final EventUtils eventUtils;
    private final AnnotationHandlerContainer annotationHandlerContainer;
    private final BotHelper botHelper;

    private static ResponseEntity<String> sendPrivate(SimpleMsgModel model, Bot bot) {
        ActionList<FriendInfoResp> friendList = bot.getFriendList();
        if (friendList.getData().stream().anyMatch(friendInfoResp -> friendInfoResp.getUserId().equals(model.getUser()))) {
            ActionData<MsgId> actionData = bot.sendPrivateMsg(model.getUser(), ShiroUtils.rawToArrayMsg(model.getText()), false);
            log.info("push private message: {}. result: {}", model, actionData);
            return ResponseEntity.ok(JSONObject.toJSONString(actionData));
        } else return ResponseEntity.badRequest().body("I have no friend: " + model.getUser());
    }

    private static ResponseEntity<String> sendGroup(SimpleMsgModel model, Bot bot) {
        ActionList<GroupInfoResp> groupList = bot.getGroupList();
        if (groupList.getData().stream().anyMatch(friendInfoResp -> friendInfoResp.getGroupId().equals(model.getGroup()))) {
            ActionData<MsgId> actionData = bot.sendGroupMsg(model.getGroup(), ShiroUtils.rawToArrayMsg(model.getText()), false);
            log.info("push group message: {}. result: {}", model, actionData);
            return ResponseEntity.ok(JSONObject.toJSONString(actionData));
        } else return ResponseEntity.badRequest().body("I have no group: " + model.getUser());
    }

    @RequestMapping(value = "/push/simple")
    public ResponseEntity<String> pushMsg(@ModelAttribute @Validated SimpleMsgModel model) {
        if (StringUtils.isBlank(model.getText())) {
            return ResponseEntity.badRequest().body("text is empty");
        }
        if (model.getUser() == null && model.getGroup() == null) {
            return ResponseEntity.badRequest().body("user and group cannot both be empty");
        }
        Bot bot = model.getBot() == null ? null : botHelper.getBot(model.getBot());
        if (bot == null) {
            return ResponseEntity.badRequest().body("bot is illegal");
        }
        return model.getGroup() == null ? sendPrivate(model, bot) : sendGroup(model, bot);
    }

    /**
     * 伪装消息事件
     * <p>
     * 可用于触发插件命令
     */
    @RequestMapping(value = "/invoke/simple")
    public ResponseEntity<String> invoke(@ModelAttribute @Validated InvokeMsgModel model) {
        if (StringUtils.isBlank(model.getText())) {
            return ResponseEntity.badRequest().body("text is empty");
        }
        if (model.getUser() == null && model.getGroup() == null) {
            return ResponseEntity.badRequest().body("user and group cannot both be empty");
        }
        Bot bot = model.getBot() == null ? null : botHelper.getBot(model.getBot());
        if (bot == null) {
            return ResponseEntity.badRequest().body("bot is illegal");
        }
        boolean isGroup = model.getGroup() != null;

        GroupMessageEvent event = new GroupMessageEvent();
        event.setFont(0);
        event.setMessage(model.getText());
        event.setRawMessage(model.getText());
        event.setMessageType(isGroup ? "group" : "private");
        event.setPostType("message");
        event.setSelfId(bot.getSelfId());
        GroupMessageEvent.GroupSender sender = new GroupMessageEvent.GroupSender();
        sender.setNickname("by invoker");
        sender.setSex("unknown");
        sender.setUserId(model.getUser());
        event.setSender(sender);
        event.setTime(System.currentTimeMillis() / 1000);
        event.setUserId(model.getUser());
        event.setGroupId(model.getGroup());
        event.setArrayMsg(ShiroUtils.rawToArrayMsg(model.getText()));

        // 执行消息过滤
        if (eventUtils.getInterceptor(bot.getBotMessageEventInterceptor()).preHandle(bot, event)) {
            // 强制可执行管理员命令
            if (1 == model.getForceSu()) {
                bot.setAnnotationHandler(annotationHandlerContainer.getAnnotationHandler());
            }
            // 执行消息事件处理
            injectionHandler.invokeAnyMessage(bot, BotUtil.castToAnyMessageEvent(event));
            if (isGroup) {
                // 群聊事件还要执行群聊消息处理
                injectionHandler.invokeGroupMessage(bot, event);
            }
        }

        return ResponseEntity.ok(JSONObject.toJSONString(event, JSONWriter.Feature.PrettyFormat));
    }
}
