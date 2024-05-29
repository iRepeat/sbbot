package com.zh.sbbot.plugin.ocr;


import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.enums.AtEnum;
import com.zh.sbbot.util.BotHelper;
import com.zh.sbbot.util.OCRUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class OcrPlugin {
    private final OCRUtil ocrUtil;
    private final BotHelper botHelper;

    @AnyMessageHandler
    @MessageHandlerFilter(startWith = ".ocr", at = AtEnum.NOT_NEED)
    public void ocr(Bot bot, AnyMessageEvent event) {
        List<String> urlList = ShiroUtils.getMsgImgUrlList(event.getArrayMsg());
        String result = urlList.stream().map(ocrUtil::baidu).collect(Collectors.joining("\n"));
        botHelper.reply(event, result);
    }
}
