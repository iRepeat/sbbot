package com.zh.sbbot.plugin.task;

import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.enums.AtEnum;
import com.zh.sbbot.constant.AdminMode;
import com.zh.sbbot.constant.TaskStatus;
import com.zh.sbbot.custom.Admin;
import com.zh.sbbot.plugin.ai.dao.PluginAi;
import com.zh.sbbot.plugin.ai.handler.AiHandler;
import com.zh.sbbot.plugin.ai.handler.AiHandlerSelector;
import com.zh.sbbot.plugin.ai.support.ChatResponse;
import com.zh.sbbot.plugin.ai.support.VendorEnum;
import com.zh.sbbot.task.TaskManager;
import com.zh.sbbot.task.TaskModel;
import com.zh.sbbot.util.BotHelper;
import com.zh.sbbot.util.BotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
public class TaskPlugin {

    private final BotHelper botHelper;
    private final TaskManager taskManager;
    private final AiHandlerSelector aiHandlerSelector;

    @NotNull
    private static String getStatusDisplay(String status) {
        return switch (status) {
            case TaskStatus.ENABLE -> "🟢正常";
            case TaskStatus.DISABLE -> "🟡禁用";
            case TaskStatus.INVALID -> "⚪无效";
            default -> "🔴异常";
        };
    }

    @NotNull
    private static String getTaskDetailDisplay(TaskModel task) {
        String taskDetails = """
                🆔 任务ID:\t %s
                📊 任务状态:\t %s
                ⏰ cron:\t\t %s
                ⏳ 下次执行:\t %s
                💬 目标群聊:\t %s
                👤 目标用户:\t %s
                🤖 目标BOT:\t %s
                ⚙️ 任务参数:\t %s
                ✅ 上次执行:\t %s
                """.formatted(
                task.getKey(),
                getStatusDisplay(task.getStatus()),
                task.getCron(),
                task.getNextTime(),
                task.getGroupId(),
                task.getUserId(),
                task.getBotId(),
                task.getCmd(),
                StringUtils.hasText(task.getLastStatus()) ? task.getLastStatus() : "从未执行");
        if (StringUtils.hasText(task.getLastStatus()) && !task.getLastStatus().equals("ok")) {
            taskDetails += "❌ 失败原因:\t " + task.getLastResult();
        }
        return taskDetails.trim();
    }

    @Admin(mode = AdminMode.GROUP_ADMIN)
    @AnyMessageHandler
    @MessageHandlerFilter(startWith = ".task", at = AtEnum.NOT_NEED)
    public void task(AnyMessageEvent event, Matcher matcher) {
        String commandParam = BotUtil.getCommandParam(matcher);
        switch (commandParam) {
            case "list" -> {
                List<TaskModel> taskModels = taskManager.listSchedulingTask(!Objects.equals(BotUtil.getParam(matcher), "all"), event.getGroupId());
                if (CollectionUtils.isEmpty(taskModels)) {
                    botHelper.reply(event, "无计划任务");
                    return;
                }
                StringBuffer sb = new StringBuffer("✨计划任务列表✨").append("\n");
                taskModels.forEach(taskModel -> {
                    sb.append("\n")
                            .append("【%s】".formatted(taskModel.getKey())).append("\n")
                            .append("📊 任务状态: ").append(getStatusDisplay(taskModel.getStatus())).append("\n")
                            .append("⏳ 下次执行: ").append(taskModel.getNextTime()).append("\n")
                            .append("⚙️ 任务参数: ").append(taskModel.getCmd()).append("\n");
                });
                botHelper.reply(event, sb.toString().trim());
            }
            case "get" -> {
                String key = BotUtil.getParam(matcher);
                if (!StringUtils.hasText(key)) {
                    botHelper.reply(event, "参数错误");
                    return;
                }
                TaskModel task = taskManager.getSchedulingTask(Long.valueOf(key));
                botHelper.reply(event, "✨计划任务详情✨\n\n%s".formatted(getTaskDetailDisplay(task)));
            }
            case "set" -> {
                String messageParam = BotUtil.getParam(matcher);

                if (!StringUtils.hasText(messageParam) || !messageParam.contains(" ")) {
                    botHelper.reply(event, "参数错误");
                    return;
                }

                String[] parts = messageParam.split(" ", 2);
                String key = parts[0].trim();
                String fieldKeyValue = parts[1].trim();

                if (!fieldKeyValue.contains(" ")) {
                    botHelper.reply(event, "参数错误");
                    return;
                }

                String[] fieldParts = fieldKeyValue.split(" ", 2);
                String fieldKey = ShiroUtils.unescape(fieldParts[0].trim());
                String fieldValue = ShiroUtils.unescape(fieldParts[1].trim());

                TaskModel task = taskManager.editTask(Long.parseLong(key), fieldKey, fieldValue);

                botHelper.reply(event, "✅操作成功！\n\n%s".formatted(getTaskDetailDisplay(task)));
            }
            case "reload" -> {
                int i = taskManager.reload(event.getGroupId());
                botHelper.reply(event, "成功加载%s条计划任务！".formatted(i));
            }
            case "exec" -> {
                String key = BotUtil.getParam(matcher);
                if (!StringUtils.hasText(key)) {
                    botHelper.reply(event, "参数错误");
                    return;
                }
                taskManager.execTaskImmediately(Long.valueOf(key));
            }
            case "del" -> {
                String key = BotUtil.getParam(matcher);
                if (!StringUtils.hasText(key)) {
                    botHelper.reply(event, "参数错误");
                    return;
                }
                taskManager.delSchedulingTask(Long.valueOf(key));
                botHelper.reply(event, "✅删除成功！");
            }
            case TaskStatus.ENABLE, TaskStatus.DISABLE -> {
                String key = BotUtil.getParam(matcher);
                if (!StringUtils.hasText(key)) {
                    botHelper.reply(event, "参数错误");
                    return;
                }
                TaskModel task = taskManager.setTaskStatus(Long.valueOf(key), commandParam);
                if (commandParam.equals(task.getStatus())) {
                    botHelper.reply(event, "✅操作成功！\n\n%s".formatted(getTaskDetailDisplay(task)));
                } else {
                    botHelper.reply(event, "❌任务状态更改失败！\n\n%s".formatted(getTaskDetailDisplay(task)));
                }
            }
            case "add" -> {
                String messageParam = BotUtil.getParam(matcher);

                if (!StringUtils.hasText(messageParam) || !messageParam.contains(" ")) {
                    botHelper.reply(event, "参数错误");
                    return;
                }

                String[] parts = messageParam.split(" ", 2);
                String desc = ShiroUtils.unescape(parts[0].trim());
                String cmd = ShiroUtils.unescape(parts[1].trim());

                String cron = generateCron(desc);
                if (cron.equals("no cron") || !CronExpression.isValidExpression(cron)) {
                    botHelper.reply(event, "【%s】不是合法的计划任务描述".formatted(desc));
                    return;
                }

                TaskModel taskModel = TaskModel.builder()
                        .cron(cron)
                        .cmd(cmd)
                        .groupId(event.getGroupId())
                        .userId(event.getUserId())
                        .botId(event.getSelfId())
                        .build();

                taskManager.saveSchedulingTask(taskModel);
                String tip = "✅添加任务成功！\n\n%s".formatted(getTaskDetailDisplay(taskModel));
                botHelper.reply(event, tip);
            }

            default -> {
                String usage = """
                        📖 Usage:
                        - list (all): 查看计划任务列表（使用all查询删除和失效的任务）
                        - get [任务ID]: 查看计划任务详情
                        - reload: 重新加载系统计划任务
                        - %s [任务ID]: 重新启用某个计划任务
                        - %s [任务ID]: 禁用某个计划任务
                        - del [任务ID]: 删除某个计划任务
                        - exec [任务ID]: 立即执行一次某任务
                        - add [任务执行规则描述] [任务参数]: 添加计划任务
                        - set [任务ID] [字段名] [新的值]: 更新目标任务的字段的值
                        
                        📚 Example:
                        .task|add 间隔十五分钟 换头像
                        .task|add 周一到周五11点 艾特我$点外卖
                        .task|add 周四八点半 kfc
                        .task|set 123456789 cmd 艾特我$吃饭
                        .task|set 123456789 cron 0 0 5 12 12 *"""
                        .formatted(TaskStatus.ENABLE, TaskStatus.DISABLE);
                botHelper.reply(event, usage);
            }

        }
    }

    private String generateCron(String desc) {
        AiHandler ai = aiHandlerSelector.get(VendorEnum.openai.name());
        PluginAi option = PluginAi.defaultConfig(null, ai.vendor(), ai.defaultModel());
        String now = DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss");
        String week = switch (LocalDate.now().getDayOfWeek()) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
        HashMap<String, String> cases = new HashMap<>() {{
            put("两个小时后", "你参考当前时间（假设2012-02-14 11:22:33），算出两小时后是2012-02-14 13:22:33，因此你响应：“33 22 13 14 2 *”。");
            put("今天下午一点", "你参考当前时间（假设2012-02-14 11:22:33），算出今天下午一点是2012-02-14 13:00:00，因此你响应：“0 0 13 14 2 *”。");
            put("今天9点", "你参考当前时间（假设2012-02-14 11:22:33），由于上午9点已过，得知用户需求是下午2024-12-12 21:00:00，因此你响应：“0 0 21 14 2 *”。");
            put("每周四8点", "这种case不需要参考当前时间，你响应：“0 0 8 ? * THU”。");
            put("天气很好", "这种是bad case，你响应：“no cron”。");
        }};
        String systemTemplate = """
                你是专业工程师，你现在是职能是写cron表达式（spring schedule的格式: {秒数} {分钟} {小时} {日期} {月份} {星期} {年份(可为空)}，注意cron第一位是秒而不是分）。\
                当用户向你表达“需求”时，你响应对应的cron（不要做任何解释）；\
                你可以参考当前时间{%s}，今天{%s}，从而智能、尽最大努力理解用户的需求；\
                如果无法理解，你应该响应“no cron”。\
                下面是几个case：%s\
                """.formatted(now, week, cases);
        option.setSystemTemplate(systemTemplate);
        ChatResponse chatResponse = ai.chat(option, desc);
        return chatResponse.getResult();
    }


}
