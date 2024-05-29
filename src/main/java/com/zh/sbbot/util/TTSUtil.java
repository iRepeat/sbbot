package com.zh.sbbot.util;

import com.zh.sbbot.repository.DictRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Base64;

@Slf4j
@RequiredArgsConstructor
@Component
public class TTSUtil {

    private final DictRepository dictRepository;

    public static TTSUtil instance() {
        return new TTSUtil(null);
    }

    /**
     * @return 音频文件路径
     */
    public String generate(String content) {
        log.info("not yet implemented");
        return null;
    }

    @SneakyThrows
    public String generateToBase64(String content) {
        File file = new File(generate(content));
        byte[] fileBytes = FileUtils.readFileToByteArray(file);

        // 将文件字节编码为Base64
        return Base64.getEncoder().encodeToString(fileBytes);
    }
}
