package com.zh.sbbot.constant;

import com.zh.sbbot.config.SystemSetting;

/**
 * 权限类型枚举
 */
public enum AdminMode {
    /**
     * 超级用户{@linkplain  SystemSetting#getSuperUser() 读取自SystemSetting }
     */
    SU,
    /**
     * 超级用户、群主和群管理员
     */
    GROUP_ADMIN,
    /**
     * 超级用户和群主
     */
    GROUP_OWNER,
}
