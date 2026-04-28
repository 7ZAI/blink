package com.blink.gateway.admin.notification.model;

import cn.hutool.core.util.StrUtil;

/**
 * 通知渠道类型枚举
 *
 * @author binblink
 * @since 2026-04-28
 */
public enum ChannelType {

    /**
     * 站内通知
     */
    IN_APP("站内通知", "in_app"),

    /**
     * 邮件通知
     */
    EMAIL("邮件通知", "email"),

    /**
     * Webhook通知
     */
    WEBHOOK("Webhook通知", "webhook"),

    /**
     * 短信通知（预留）
     */
    SMS("短信通知", "sms"),

    /**
     * 微信通知（预留）
     */
    WECHAT("微信通知", "wechat"),

    /**
     * 钉钉通知（预留）
     */
    DINGTALK("钉钉通知", "dingtalk"),

    /**
     * 飞书通知（预留）
     */
    FEISHU("飞书通知", "feishu");

    /**
     * 渠道名称
     */
    private final String name;

    /**
     * 渠道编码
     */
    private final String code;

    ChannelType(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    /**
     * 根据code查找对应的渠道类型
     *
     * @param code 渠道编码
     * @return 渠道类型，未找到返回null
     */
    public static ChannelType fromCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        for (ChannelType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
