package com.zh.sbbot.plugins.push;

import com.alibaba.fastjson2.JSONObject;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.common.ActionList;
import com.mikuac.shiro.dto.action.common.MsgId;
import com.mikuac.shiro.dto.action.response.FriendInfoResp;
import com.mikuac.shiro.dto.action.response.GroupInfoResp;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequiredArgsConstructor
@RequestMapping("/msg")
public class MsgPushController {
    private static final Logger log = LoggerFactory.getLogger(MsgPushController.class);
    private final BotContainer botContainer;

    private static ResponseEntity<String> sendPrivate(SimplePushModel model, Bot bot) {
        ActionList<FriendInfoResp> friendList = bot.getFriendList();
        if (friendList.getData().stream().anyMatch(friendInfoResp -> friendInfoResp.getUserId().equals(model.getTarget()))) {
            ActionData<MsgId> actionData = bot.sendPrivateMsg(model.getTarget(), ShiroUtils.rawToArrayMsg(model.getText()), false);
            log.info("push private message: {}. result: {}", model, actionData);
            return ResponseEntity.ok(JSONObject.toJSONString(actionData));
        } else return ResponseEntity.badRequest().body("I have no friend: " + model.getTarget());
    }

    private static ResponseEntity<String> sendGroup(SimplePushModel model, Bot bot) {
        ActionList<GroupInfoResp> groupList = bot.getGroupList();
        if (groupList.getData().stream().anyMatch(friendInfoResp -> friendInfoResp.getGroupId().equals(model.getTarget()))) {
            ActionData<MsgId> actionData = bot.sendGroupMsg(model.getTarget(), ShiroUtils.rawToArrayMsg(model.getText()), false);
            log.info("push group message: {}. result: {}", model, actionData);
            return ResponseEntity.ok(JSONObject.toJSONString(actionData));
        } else return ResponseEntity.badRequest().body("I have no group: " + model.getTarget());
    }

    @RequestMapping(method = RequestMethod.POST, value = "/simple")
    public ResponseEntity<String> pushMsg(@RequestBody @Validated SimplePushModel model) {
        if (StringUtils.isBlank(model.getText())) {
            return ResponseEntity.badRequest().body("text is empty");
        }
        Bot bot = botContainer.robots.get(model.getBot());
        if (bot == null) {
            return ResponseEntity.badRequest().body("bot is illegal");
        }
        if (MsgType.PRIVATE.name().equalsIgnoreCase(model.getType())) {
            return sendPrivate(model, bot);
        } else if (MsgType.GROUP.name().equalsIgnoreCase(model.getType())) {
            return sendGroup(model, bot);
        } else {
            return ResponseEntity.badRequest().body("type is illegal. should be " + Arrays.toString(MsgType.values()));
        }
    }
}
