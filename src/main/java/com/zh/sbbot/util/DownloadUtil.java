package com.zh.sbbot.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

@Slf4j
public class DownloadUtil {

    /**
     * 根据url下载文件。并返回文件base64编码（DataUrl）
     */
    public static String downloadIntoMemory(String url) {
        HttpURLConnection connection = null;
        try {
            URL urlObject = new URL(url);
            connection = (HttpURLConnection) urlObject.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // 检查响应码是否表明请求成功
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage());
            }

            // 使用 Apache Commons IO 读取和转换输入流
            InputStream inputStream = connection.getInputStream();
            byte[] fileBytes = IOUtils.toByteArray(inputStream);

            // 将文件字节编码为Base64
            return Base64.getEncoder().encodeToString(fileBytes);

        } catch (Exception e) {
            log.error("download failed: ", e);
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
