package com.zh.sbbot.util;

import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.zh.sbbot.config.QQTTSConfig;
import com.zh.sbbot.constant.DictKey;
import com.zh.sbbot.repository.DictRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Base64;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Component
public class TTSUtil {

    private final DictRepository dictRepository;
    private final BotHelper botHelper;
    private final QQTTSConfig qqConfig;

    /**
     * @return https/http/base64/file协议的音频
     */
    public String generate(String content, AnyMessageEvent event) {
        String res = null;
        switch (dictRepository.get(DictKey.SYSTEM_TTS_TYPE)) {
            case "qq" -> res = generateWithQQ(content, event.getGroupId());
            default -> log.info("not yet implemented");
        }
        return res;
    }

    private String generateWithQQ(String content, Long groupId) {
        Long group = Objects.isNull(groupId) ? qqConfig.getGroup() : groupId;
        String character = qqConfig.getCharacter();
        return botHelper.getAiRecord(group, character, content);
    }


    @SneakyThrows
    public String fileToBase64(String filePath) {
        File file = new File(filePath);
        byte[] fileBytes = FileUtils.readFileToByteArray(file);

        // 将文件字节编码为Base64
        return Base64.getEncoder().encodeToString(fileBytes);
    }
}
