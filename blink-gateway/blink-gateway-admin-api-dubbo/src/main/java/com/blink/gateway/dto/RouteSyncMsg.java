package com.blink.gateway.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 路由同步消息 DTO
 * 支持广播模式和指定实例推送
 *
 * @author binblink
 */
@Getter
@Setter
public class RouteSyncMsg implements Serializable {
    @Serial
    private static final long serialVersionUID = 6707034006158344769L;

    /**
     * 动态路由 Redis Key（Redis模式）
     */
    private String dynamicRouteKey;

    /**
     * 存储方式: redis / nacos
     */
    private String storageMode;

    /**
     * Nacos dataId（Nacos模式）
     */
    private String dataId;

    /**
     * Nacos group（Nacos模式）
     */
    private String group;

    /**
     * 推送模式: broadcast / specified
     */
    private String pushMode;

    /**
     * 目标实例ID列表（指定实例模式）
     */
    private List<String> targetInstanceIds;

    @Override
    public String toString() {
        return "RouteSyncMsg{" +
                "dynamicRouteKey='" + dynamicRouteKey + '\'' +
                ", storageMode='" + storageMode + '\'' +
                ", dataId='" + dataId + '\'' +
                ", group='" + group + '\'' +
                ", pushMode='" + pushMode + '\'' +
                ", targetInstanceIds=" + targetInstanceIds +
                '}';
    }
}