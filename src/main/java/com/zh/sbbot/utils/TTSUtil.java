package com.zh.sbbot.utils;

import com.zh.sbbot.constant.DictKey;
import com.zh.sbbot.repository.DictRepository;
import io.github.whitemagic2014.tts.TTS;
import io.github.whitemagic2014.tts.TTSVoice;
import io.github.whitemagic2014.tts.bean.Voice;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Base64;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class TTSUtil {

    private final DictRepository dictRepository;

    public static TTSUtil instance() {
        return new TTSUtil(null);
    }

    public String generate(String content) {
        String defaultShortName = "zh-CN-liaoning-XiaobeiNeural";
        String path = "./tts";

        String finalName = Optional.ofNullable(dictRepository)
                .map(repo -> repo.getValue(DictKey.TTS_NAME))
                .filter(StringUtils::isNotBlank)
                .orElse(defaultShortName);

        Voice voice = TTSVoice.provides()
                .stream()
                .filter(v -> v.getShortName().equals(finalName))
                .findFirst()
                .orElse(null);

        String fileName = new TTS(voice, content)
                .fileName(content.substring(0, Math.min(content.length(), 10)) + "." + System.currentTimeMillis())
                .storage(path)
                .trans();
        return path + "/" + fileName;
    }

    @SneakyThrows
    public String generateToBase64(String content) {
        File file = new File(generate(content));
        byte[] fileBytes = FileUtils.readFileToByteArray(file);

        // 将文件字节编码为Base64
        return Base64.getEncoder().encodeToString(fileBytes);
    }
}
