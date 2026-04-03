package com.blink.gateway.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author binblink
 * @Date 2025/11/5
 */
public class RouteSyncMsg implements Serializable {
    @Serial
    private static final long serialVersionUID = 6707034006158344769L;

    private String dynamicRouteKey;

    public String getDynamicRouteKey() {
        return dynamicRouteKey;
    }

    public void setDynamicRouteKey(String dynamicRouteKey) {
        this.dynamicRouteKey = dynamicRouteKey;
    }

    @Override
    public String toString() {
        return "RouteSyncMsgDTO{" +
                "dynamicRouteKey='" + dynamicRouteKey + '\'' +
                '}';
    }
}