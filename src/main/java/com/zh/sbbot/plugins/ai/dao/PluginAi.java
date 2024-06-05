package com.zh.sbbot.plugins.ai.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
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
     * prompt模板
     */
    private String promptTemplate;

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


    @Override
    @SneakyThrows
    public String toString() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
