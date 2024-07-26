package com.zh.sbbot.plugin.ai.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zh.sbbot.plugin.ai.support.VendorEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

@Getter
@Setter
public class PluginAi {

    /**
     * 主键，群组id
     */
    private Long groupId;

    /**
     * 系统提示模板
     */
    private String systemTemplate;

    /**
     * 模型
     */
    private String model;

    /**
     * 模型的温度参数
     */
    private String temperature;

    /**
     * 是否禁用。0: 开启 1: 禁用
     */
    private Integer isDisable;

    /**
     * 最大token数
     */
    private Integer maxToken;

    /**
     * 历史消息数
     */
    private Integer lastN;

    /**
     * 启用搜索 0：禁用 1：启用
     */
    private Integer enableSearch;

    /**
     * 当前AI平台
     * {@link VendorEnum}
     */
    private String vendor;

    /**
     * topP参数
     */
    private String topP;

    /**
     * 是否开启tts
     */
    private Integer tts;

    public static PluginAi defaultConfig(Long groupId, String vendor, String model) {
        PluginAi pluginAi = new PluginAi();
        pluginAi.setGroupId(groupId);
        pluginAi.setSystemTemplate("你是一个很有用的群聊助手");
        pluginAi.setModel(model);

        pluginAi.setTemperature("0.9");
        pluginAi.setIsDisable(0);
        pluginAi.setMaxToken(1024);

        pluginAi.setLastN(5);
        pluginAi.setEnableSearch(1);
        pluginAi.setVendor(vendor);

        pluginAi.setTopP("0.9");
        pluginAi.setTts(0);
        return pluginAi;
    }


    @Override
    @SneakyThrows
    public String toString() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
