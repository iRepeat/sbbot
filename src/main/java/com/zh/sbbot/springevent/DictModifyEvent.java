package com.zh.sbbot.springevent;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 数据字典表中数据变更事件
 */
@Getter
public class DictModifyEvent extends ApplicationEvent {
    private final String key;

    public DictModifyEvent(Object source, String key) {
        super(source);
        this.key = key;
    }
}
