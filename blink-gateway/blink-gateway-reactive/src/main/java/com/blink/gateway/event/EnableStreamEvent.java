package com.blink.gateway.event;

import org.springframework.context.ApplicationEvent;

/**
 * 开启或者关闭stream 监听事件
 * @author binblink
 */
public class EnableStreamEvent extends ApplicationEvent {

    private final Boolean newValue;

    public EnableStreamEvent( Boolean newValue) {
        super(newValue);
        this.newValue = newValue;
    }

    public Boolean getNewValue() {
        return newValue;
    }
}