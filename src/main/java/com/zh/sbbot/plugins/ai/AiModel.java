package com.zh.sbbot.plugins.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

@Getter
@Setter
public class AiModel {

    /**
     * 主键，群组id
     */
    private Long groupId;

    /**
     * 系统提示模板
     */
    private String systemTemplate;

    /**
     * 模型。默认gpt-3.5-turbo
     */
    private String model;

    /**
     * 模型的温度参数。默认0.7
     */
    private String temperature;

    /**
     * 是否禁用。0: 开启 1: 禁用
     */
    private Integer isDisable;


    @Override
    @SneakyThrows
    public String toString() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
