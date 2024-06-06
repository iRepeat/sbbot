package com.zh.sbbot.plugins.ai.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zh.sbbot.plugins.ai.support.VendorEnum;
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


    @Override
    @SneakyThrows
    public String toString() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
