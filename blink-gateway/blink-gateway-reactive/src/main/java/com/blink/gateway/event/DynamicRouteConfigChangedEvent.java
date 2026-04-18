package com.blink.gateway.event;

import com.blink.gateway.config.prop.BlinkGatewayProperties.DynamicRoute;
import org.springframework.context.ApplicationEvent;

/**
 * 动态路由配置变更事件
 *
 * @author binblink
 */
public class DynamicRouteConfigChangedEvent extends ApplicationEvent {

    private final DynamicRoute oldValue;

    private final DynamicRoute newValue;

    private final ChangeType changeType;

    public DynamicRouteConfigChangedEvent(DynamicRoute oldValue, DynamicRoute newValue, ChangeType changeType) {
        super(changeType);
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changeType = changeType;
    }

    public DynamicRoute getOldValue() {
        return oldValue;
    }

    public DynamicRoute getNewValue() {
        return newValue;
    }

    public ChangeType getChangeType() {
        return changeType;
    }
}