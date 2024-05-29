package com.zh.sbbot.plugin.controller;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class InvokeMsgModel extends SimpleMsgModel {
    /**
     * 忽略用户身份，可以调用管理员命令
     */
    private Integer forceSu = 0;

    @Override
    public String toString() {
        return super.toString();
    }
}
