package com.zh.sbbot.plugin.system;

import com.alibaba.fastjson2.TypeReference;
import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.enums.AtEnum;
import com.zh.sbbot.custom.Admin;
import com.zh.sbbot.config.SystemSetting;
import com.zh.sbbot.constant.AdminMode;
import com.zh.sbbot.constant.DictKey;
import com.zh.sbbot.repository.AliasRepository;
import com.zh.sbbot.repository.DictRepository;
import com.zh.sbbot.util.BotHelper;
import com.zh.sbbot.util.BotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * 封装Bot管理、数据库管理、插件管理等命令
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
    private final com.zh.sbbot.util.TTSUtil TTSUtil;

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
     * 黑名单
     */
    @AnyMessageHandler
    @Admin(mode = AdminMode.GROUP_ADMIN)
    @MessageHandlerFilter(startWith = {".ban", ".block"}, at = AtEnum.NOT_NEED)
    public void blackList(AnyMessageEvent event, Matcher matcher, Bot bot) {

        Set<Long> paramIds = Optional.ofNullable(BotUtil.getParam(matcher))
                .map(param -> ShiroUtils.unescape(param).split(","))
                .map(Set::of)
                .orElse(Collections.emptySet())
                .stream()
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(Long::parseLong)
                .collect(Collectors.toSet());


        if (CollectionUtils.isEmpty(paramIds)) {
            botHelper.reply(event, "未给定ID");
            return;
        }

        Set<Long> dbIds = dictRepository.get(DictKey.SYSTEM_BLOCK_IDS, new TypeReference<>() {
        });
        if (CollectionUtils.isEmpty(dbIds)) {
            dbIds = new HashSet<>();
        }

        // 移除黑名单
        if (BotUtil.getCommandParam(matcher).equals("del")) {
            dbIds.removeAll(paramIds);
            dictRepository.setOrRemove(DictKey.SYSTEM_BLOCK_IDS, dbIds);
            botHelper.reply(event, "已移除黑名单：" + paramIds);
            return;
        }

        if (CollectionUtils.containsAny(paramIds, Set.of(systemSetting.getSuperUser()))) {
            if (botHelper.isSuperUser(event.getUserId())) {
                botHelper.reply(event, "超级用户无法被拉黑");
                paramIds.removeAll(Set.of(systemSetting.getSuperUser()));
            } else {
                botHelper.reply(event, "监测到非法调用，您将被拉黑");
                bot.setGroupBan(event.getGroupId(), event.getUserId(), 10 * 60);
                paramIds = Set.of(event.getUserId());
            }
        }

        dbIds.addAll(paramIds);
        dictRepository.setOrRemove(DictKey.SYSTEM_BLOCK_IDS, dbIds);
        botHelper.reply(event, "已拉黑：" + paramIds);

    }

}