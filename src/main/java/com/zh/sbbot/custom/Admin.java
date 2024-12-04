package com.zh.sbbot.custom;

import com.zh.sbbot.constant.AdminMode;

import java.lang.annotation.*;

/**
 * 标识管理员命令，结合消息事件使用
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Admin {
    /**
     * 管理员权限类型
     */
    AdminMode mode() default AdminMode.SU;
}
