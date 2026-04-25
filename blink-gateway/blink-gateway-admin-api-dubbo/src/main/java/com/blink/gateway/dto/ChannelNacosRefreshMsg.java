package com.blink.gateway.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * 渠道配置刷新消息 DTO
 * 用于通知 gateway-reactive 从 Nacos 拉取最新渠道配置
 *
 * @author binblink
 */
public class ChannelNacosRefreshMsg implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 刷新类型
     * "ALL" - 全量刷新
     * "SINGLE" - 单个渠道刷新
     * "DELETE" - 删除渠道缓存
     */
    private String refreshType;

    /**
     * 渠道标识（单个刷新/删除时使用）
     */
    private String appKey;

    /**
     * 配置版本号（用于防止消息乱序）
     */
    private Long version;

    /**
     * 操作人用户ID
     */
    private Integer operatorUser;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 时间戳
     */
    private Long timestamp;

    // ==================== 刷新类型常量 ====================

    /**
     * 全量刷新
     */
    public static final String REFRESH_TYPE_ALL = "ALL";

    /**
     * 单个刷新
     */
    public static final String REFRESH_TYPE_SINGLE = "SINGLE";

    /**
     * 删除缓存
     */
    public static final String REFRESH_TYPE_DELETE = "DELETE";

    // ==================== 静态工厂方法 ====================

    /**
     * 创建全量刷新消息
     */
    public static ChannelNacosRefreshMsg allRefresh(Integer operatorUser, String operatorName) {
        ChannelNacosRefreshMsg msg = new ChannelNacosRefreshMsg();
        msg.setRefreshType(REFRESH_TYPE_ALL);
        msg.setOperatorUser(operatorUser);
        msg.setOperatorName(operatorName);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    /**
     * 创建单个渠道刷新消息
     */
    public static ChannelNacosRefreshMsg singleRefresh(String appKey, Integer operatorUser, String operatorName) {
        ChannelNacosRefreshMsg msg = new ChannelNacosRefreshMsg();
        msg.setRefreshType(REFRESH_TYPE_SINGLE);
        msg.setAppKey(appKey);
        msg.setOperatorUser(operatorUser);
        msg.setOperatorName(operatorName);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    /**
     * 创建删除渠道缓存消息
     */
    public static ChannelNacosRefreshMsg deleteRefresh(String appKey, Integer operatorUser, String operatorName) {
        ChannelNacosRefreshMsg msg = new ChannelNacosRefreshMsg();
        msg.setRefreshType(REFRESH_TYPE_DELETE);
        msg.setAppKey(appKey);
        msg.setOperatorUser(operatorUser);
        msg.setOperatorName(operatorName);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    // ==================== Getter/Setter ====================

    public String getRefreshType() {
        return refreshType;
    }

    public void setRefreshType(String refreshType) {
        this.refreshType = refreshType;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Integer getOperatorUser() {
        return operatorUser;
    }

    public void setOperatorUser(Integer operatorUser) {
        this.operatorUser = operatorUser;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ChannelNacosRefreshMsg{" +
                "refreshType='" + refreshType + '\'' +
                ", appKey='" + appKey + '\'' +
                ", version=" + version +
                ", operatorUser=" + operatorUser +
                ", operatorName='" + operatorName + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
