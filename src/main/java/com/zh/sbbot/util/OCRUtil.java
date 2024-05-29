package com.zh.sbbot.util;

import com.baidu.aip.ocr.AipOcr;
import com.zh.sbbot.config.BaiduOCRConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class OCRUtil {

    private final BaiduOCRConfig baiduConfig;

    public String baidu(String url) {
        try {
            // 初始化一个AipOcr
            AipOcr client = new AipOcr(baiduConfig.getAppId(), baiduConfig.getApiKey(), baiduConfig.getSecretKey());

            // 可选：设置网络连接参数
            client.setConnectionTimeoutInMillis(2000);
            client.setSocketTimeoutInMillis(60000);

            // 调用接口
            JSONObject res = baiduConfig.isAccurate() ?
                    client.basicAccurateGeneralUrl(url, new HashMap<>()) : client.basicGeneralUrl(url, new HashMap<>());

            log.info("baidu ocr result: {}", res.toString(2));

            JSONArray wordsResult = res.getJSONArray("words_result");
            return wordsResult.toList().stream().map(jsonObject -> ((HashMap<?, ?>) jsonObject).get(
                    "words")).map(Object::toString).collect(Collectors.joining("，"));
        } catch (JSONException e) {
            log.info("baidu ocr error: ", e);
            return StringUtils.EMPTY;
        }
    }
}
