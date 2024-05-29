package com.zh.sbbot.annotations;

import java.lang.annotation.*;

/**
 * 标识管理员命令
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Admin {
}
