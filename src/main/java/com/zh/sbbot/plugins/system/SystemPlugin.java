package com.zh.sbbot.plugins.system;

import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.enums.AtEnum;
import com.zh.sbbot.annotations.Admin;
import com.zh.sbbot.configs.SystemSetting;
import com.zh.sbbot.utils.BotHelper;
import com.zh.sbbot.utils.BotUtil;
import com.zh.sbbot.utils.CommandExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * 管理员插件
 */
@Shiro
@Component
@Slf4j
@Admin
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class SystemPlugin {

    private final SystemSetting systemSetting;
    private final JdbcTemplate jdbcTemplate;
    private final BotHelper botHelper;

    @AnyMessageHandler
    @MessageHandlerFilter(startWith = ".say", at = AtEnum.NOT_NEED)
    public void say(Bot bot, AnyMessageEvent event, Matcher matcher) {
        Optional.ofNullable(BotUtil.getParam(matcher)).ifPresent(s -> bot.sendMsg(event,
                ShiroUtils.rawToArrayMsg(ShiroUtils.unescape(s)), false));
    }

    @AnyMessageHandler
    @MessageHandlerFilter(startWith = ".echo", at = AtEnum.NOT_NEED)
    public void echo(Bot bot, AnyMessageEvent event, Matcher matcher) {
        Optional.ofNullable(BotUtil.getParam(matcher)).ifPresent(s -> bot.sendMsg(event,
                ShiroUtils.unescape(s), true));
    }

    @AnyMessageHandler
    @MessageHandlerFilter(cmd = ".up", at = AtEnum.NOT_NEED)
    public void up(AnyMessageEvent event) {
        systemSetting.setEnable(true);
        log.info("bot已全局开启（临时）");
        botHelper.reply(event, "bot已全局开启（临时）");
    }


    @AnyMessageHandler
    @MessageHandlerFilter(cmd = ".down", at = AtEnum.NOT_NEED)
    public void down(AnyMessageEvent event) {
        systemSetting.setEnable(false);
        log.info("bot已全局禁用（临时）");
        botHelper.reply(event, "bot已全局禁用（临时）");
    }


    @AnyMessageHandler
    @MessageHandlerFilter(startWith = ".exec", at = AtEnum.NOT_NEED)
    public void exec(AnyMessageEvent event, Matcher matcher) {
        String param = BotUtil.getParam(matcher);
        if (StringUtils.isBlank(param)) {
            botHelper.reply(event, "请给定命令");
        }
        log.info("尝试执行命令：{}", param);
        try {
            String result = CommandExecutor.execute(param);
            botHelper.reply(event, StringUtils.isBlank(result) ? "（命令返回空）" : result);
        } catch (Exception e) {
            log.error("执行命令失败！", e);
            botHelper.reply(event, "执行命令失败！\n" + e.getMessage());
        }
    }

    @AnyMessageHandler
    @MessageHandlerFilter(startWith = ".db", at = AtEnum.NOT_NEED)
    public void dbExec(AnyMessageEvent event, Matcher matcher) {
        String sql = BotUtil.getParam(matcher);
        if (StringUtils.isBlank(sql)) {
            botHelper.reply(event, "请给定SQL语句");
            return;
        }

        log.info("尝试执行：{}", sql);
        String msg;
        try {
            sql = sql.trim().toLowerCase();
            if (sql.startsWith("insert") || sql.startsWith("update") || sql.startsWith("delete")) {
                int update = jdbcTemplate.update(sql);
                msg = String.format("执行更新语句：“%s”成功！影响行数：%d", sql, update);
            } else if (sql.startsWith("select")) {
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

}