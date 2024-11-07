package com.zh.sbbot.plugin.system;

import com.alibaba.fastjson2.JSONObject;
import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.enums.AtEnum;
import com.mikuac.shiro.enums.MsgTypeEnum;
import com.mikuac.shiro.model.ArrayMsg;
import com.zh.sbbot.annotation.Admin;
import com.zh.sbbot.config.SystemSetting;
import com.zh.sbbot.constant.AdminMode;
import com.zh.sbbot.repository.AliasRepository;
import com.zh.sbbot.repository.DictRepository;
import com.zh.sbbot.util.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.*;
import java.util.function.Function;
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
public class SystemPlugin {

    private final SystemSetting systemSetting;
    private final JdbcTemplate jdbcTemplate;
    private final BotHelper botHelper;
    private final DictRepository dictRepository;
    private final AliasRepository aliasRepository;
    private final TTSUtil ttsUtil;
    private final com.zh.sbbot.util.TTSUtil TTSUtil;

    /**
     * 让bot重复你的话
     */
    @AnyMessageHandler
    @Admin(mode = AdminMode.GROUP_ADMIN)
    @MessageHandlerFilter(startWith = ".say", at = AtEnum.NOT_NEED)
    public void say(Bot bot, AnyMessageEvent event, Matcher matcher) {
        Optional.ofNullable(BotUtil.getParam(matcher)).ifPresent(s -> {
            List<ArrayMsg> msgList = ShiroUtils.rawToArrayMsg(ShiroUtils.unescape(s));
            bot.sendMsg(event, msgList, false);
        });
    }

    /**
     * 让bot发送一条json卡片信息
     */
    @AnyMessageHandler
    @Admin(mode = AdminMode.GROUP_ADMIN)
    @MessageHandlerFilter(startWith = ".json", at = AtEnum.NOT_NEED)
    public void json(Bot bot, AnyMessageEvent event, Matcher matcher) {
        Optional.ofNullable(BotUtil.getParam(matcher)).ifPresent(s -> {
            List<ArrayMsg> json = ArrayMsgUtils.builder().json(ShiroUtils.unescape(s)).build();
            bot.sendMsg(event, json, false);
        });
    }

    /**
     * 让bot重复你的话（文本形式发送，媒体信息或表情等会转译为CQ码）
     */
    @AnyMessageHandler
    @Admin(mode = AdminMode.GROUP_ADMIN)
    @MessageHandlerFilter(startWith = ".echo", at = AtEnum.NOT_NEED)
    public void echo(Bot bot, AnyMessageEvent event, Matcher matcher) {
        Optional.ofNullable(BotUtil.getParam(matcher)).ifPresent(s -> bot.sendMsg(event,
                ShiroUtils.unescape(s), true));
    }

    /**
     * 全局启用（当system.enable=false时，可用该命令临时启用bot）
     */
    @AnyMessageHandler
    @Admin
    @MessageHandlerFilter(cmd = ".up", at = AtEnum.NOT_NEED)
    public void up(AnyMessageEvent event) {
        systemSetting.setEnable(true);
        log.info("bot已全局开启（临时）");
        botHelper.reply(event, "bot已全局开启（临时）");
    }


    /**
     * 全局禁用（当system.enable=true时，可用该命令临时禁用bot）
     */
    @AnyMessageHandler
    @Admin
    @MessageHandlerFilter(cmd = ".down", at = AtEnum.NOT_NEED)
    public void down(AnyMessageEvent event) {
        systemSetting.setEnable(false);
        log.info("bot已全局禁用（临时）");
        botHelper.reply(event, "bot已全局禁用（临时）");
    }


    /**
     * 执行系统shell命令
     */
    @AnyMessageHandler
    @Admin
    @MessageHandlerFilter(startWith = ".exec", at = AtEnum.NOT_NEED)
    public void exec(AnyMessageEvent event, Bot bot, Matcher matcher) {
        String param = BotUtil.getParam(matcher);
        if (StringUtils.isBlank(param) || param.equals("|tts")) {
            botHelper.reply(event, "请给定命令");
            return;
        }

        boolean tts = false;
        if (param.startsWith("|tts")) {
            param = param.replaceFirst("\\|tts", "").trim();
            tts = true;
        }

        // 去除转义
        param = ShiroUtils.unescape(param);

        try {
            String result = CommandExecutor.execute(param, 30000);
            if (StringUtils.isBlank(result)) log.info("（命令返回空）");
            else if (result.trim().startsWith("{\"app\"")) {
                log.info("发送json数据：{}", result);
                bot.sendMsg(event, ArrayMsgUtils.builder().json(result).build(), false);
            } else if (result.contains("[CQ:record") || result.contains("[CQ:video")) {
                // 语音消息无法被引用，因此不能使用reply
                bot.sendMsg(event, result, false);
            } else if (tts) {
                String voice = TTSUtil.generate(result);
                bot.sendMsg(event, ArrayMsgUtils.builder().voice(voice).build(), false);
            }else {
                botHelper.reply(event, result);
            }
            botHelper.sendEmojiLike(String.valueOf(event.getMessageId()), "124");
        } catch (Exception e) {
            log.error("执行命令失败！", e);
            botHelper.reply(event, "执行命令失败！\n" + e.getMessage());
        }
    }

    /**
     * 执行sql
     */
    @AnyMessageHandler
    @Admin
    @MessageHandlerFilter(startWith = ".db", at = AtEnum.NOT_NEED)
    public void dbExec(AnyMessageEvent event, Matcher matcher) {
        String sql = BotUtil.getParam(matcher);
        if (StringUtils.isBlank(sql)) {
            botHelper.reply(event, "请给定SQL语句");
            return;
        }

        sql = ShiroUtils.unescape(sql);
        log.info("尝试执行：{}", sql);
        String msg;

        try {
            String finalSql = sql.trim();
            Function<String, Boolean> matchIgnoreCase = (keyword) -> finalSql.matches("(?i)^" + keyword + "\\s.*");
            if (matchIgnoreCase.apply("insert") || matchIgnoreCase.apply("update") || matchIgnoreCase.apply("delete")) {
                int update = jdbcTemplate.update(sql);
                msg = String.format("执行更新语句：“%s”成功！影响行数：%d", sql, update);
            } else if (matchIgnoreCase.apply("select")) {
                List<Map<String, Object>> maps = jdbcTemplate.queryForList(sql);
                msg = String.format("执行查询语句：“%s”成功！\n结果：%s", sql, maps);
            } else {
                jdbcTemplate.execute(sql);
                msg = String.format("执行语句：“%s”成功！", sql);
            }
        } catch (Exception e) {
            log.error("执行失败！", e);
            msg = String.format("执行语句：“%s”失败！\n%s", sql, e.getMessage());
        }

        botHelper.reply(event, msg);
    }

    /**
     * 设置字典值
     */
    @AnyMessageHandler
    @Admin
    @MessageHandlerFilter(startWith = ".set", at = AtEnum.NOT_NEED)
    public void set(AnyMessageEvent event, Matcher matcher) {
        Optional.ofNullable(BotUtil.getParam(matcher)).ifPresent(s -> {
            int i = s.indexOf(' ');
            if (i > -1) {
                String key = ShiroUtils.unescape(s.substring(0, i).trim());
                String value = ShiroUtils.unescape(s.substring(i + 1).trim());
                if (Objects.equals(value, "del")) {
                    dictRepository.setOrRemove(key, null);
                    botHelper.reply(event, "移除(" + key + ")成功！");
                } else {
                    dictRepository.setOrRemove(key, value);
                    botHelper.reply(event, "设置成功！" + key + " = " + value);
                }
            }
        });
    }

    /**
     * 获取字典值
     */
    @AnyMessageHandler
    @Admin
    @MessageHandlerFilter(startWith = ".get", at = AtEnum.NOT_NEED)
    public void get(AnyMessageEvent event, Matcher matcher) {
        Optional.ofNullable(BotUtil.getParam(matcher)).ifPresentOrElse(param -> {
            String unescapedParam = ShiroUtils.unescape(param);
            String dictValue = dictRepository.get(unescapedParam);
            String aliasValue = aliasRepository.get(unescapedParam);
            StringJoiner responseJoiner = new StringJoiner("\n");

            if (StringUtils.isNotBlank(dictValue)) {
                responseJoiner.add("【dict】")
                        .add(unescapedParam + " = " + dictValue);
            }
            if (StringUtils.isNotBlank(aliasValue)) {
                responseJoiner.add("【alias】")
                        .add(unescapedParam + " = " + aliasValue);
            }
            if (responseJoiner.length() == 0) {
                responseJoiner.add("无数据");
            }
            botHelper.reply(event, responseJoiner.toString().trim(), true);
        }, () -> {
            List<String> dictKeys = dictRepository.getAll();
            List<String> aliasKeys = aliasRepository.getAll();
            StringJoiner responseJoiner = new StringJoiner("\n");

            if (!CollectionUtils.isEmpty(dictKeys)) {
                responseJoiner.add("【dict】")
                        .add(String.join("\n", dictKeys));
            }
            if (!CollectionUtils.isEmpty(aliasKeys)) {
                responseJoiner.add("【alias】")
                        .add(String.join("\n", aliasKeys));
            }
            if (responseJoiner.length() == 0) {
                responseJoiner.add("无数据");
            }
            botHelper.reply(event, responseJoiner.toString().trim());
        });
    }


    /**
     * 命令别名
     */
    @AnyMessageHandler
    @Admin
    @MessageHandlerFilter(startWith = ".alias", at = AtEnum.NOT_NEED)
    public void aliSet(AnyMessageEvent event, Matcher matcher) {
        Optional.ofNullable(BotUtil.getParam(matcher)).ifPresent(s -> {
            int i = s.indexOf(' ');
            if (i > -1) {
                String key = ShiroUtils.unescape(s.substring(0, i).trim());
                if (key.contains("【参数】")) {
                    botHelper.reply(event, "key不能包含【参数】");
                    return;
                }
                String value = ShiroUtils.unescape(s.substring(i + 1).trim());
                if (Objects.equals(value, "del")) {
                    aliasRepository.setOrRemove(key, null);
                    botHelper.reply(event, "移除(" + key + ")成功！");
                } else {
                    aliasRepository.setOrRemove(key, value);
                    botHelper.reply(event, "别名设置成功！“%s” => “%s”".formatted(key, value));
                }
            }
        });
    }


    /**
     * 发送拓展API
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
     * 设置头像
     */
    @SneakyThrows
    @AnyMessageHandler
    @Admin(mode = AdminMode.GROUP_ADMIN)
    @MessageHandlerFilter(startWith = ".avatar", at = AtEnum.NOT_NEED)
    public void avatar(AnyMessageEvent event, Matcher matcher) {
        String image = ShiroUtils.getMsgImgUrlList(event.getArrayMsg()).stream()
                .findFirst().or(() -> Optional.ofNullable(BotUtil.getParam(matcher))).orElse(null);
        if (StringUtils.isBlank(image)) {
            // 获取今天周几
            int week = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1) % 7;
            // 文件转base64
            String file = "./image/avatar_week" + week + ".jpg";
            String base64Img = new String(Base64.getEncoder().encode(FileUtils.readFileToByteArray(new File(file))));
            botHelper.setSelfAvatar("base64://" + base64Img);
            log.info("更换每日头像：{}", file);
        } else {
            String b64 = DownloadUtil.downloadIntoMemory(image);
            botHelper.setSelfAvatar("base64://" + b64);
            log.info("头像已更换：{}", image);
        }
    }

    /**
     * 设置群聊头像
     */
    @SneakyThrows
    @GroupMessageHandler
    @Admin(mode = AdminMode.GROUP_ADMIN)
    @MessageHandlerFilter(startWith = ".group_avatar", at = AtEnum.NOT_NEED)
    public void group_avatar(GroupMessageEvent event, Matcher matcher) {
        String image = ShiroUtils.getMsgImgUrlList(event.getArrayMsg()).stream()
                .findFirst().or(() -> Optional.ofNullable(BotUtil.getParam(matcher))).orElse(null);
        if (StringUtils.isBlank(image)) {
            // 获取今天周几
            int week = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1) % 7;
            // 文件转base64
            String file = "./image/avatar_group_week" + week + ".jpg";
            String base64Img = new String(Base64.getEncoder().encode(FileUtils.readFileToByteArray(new File(file))));
            botHelper.setGroupAvatar("base64://" + base64Img, String.valueOf(event.getGroupId()));
            log.info("群聊更换每日头像：{}", file);
        } else {
            String b64 = DownloadUtil.downloadIntoMemory(image);
            botHelper.setGroupAvatar("base64://" + b64, String.valueOf(event.getGroupId()));
            log.info("群聊头像已更换：{}", image);
        }
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