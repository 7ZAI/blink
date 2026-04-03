package com.blink.framework.common.jwt;

import lombok.Data;

import java.util.*;

/**
 * 自定义JWT载荷DTO
 * 对应JJWT官方Claims的完整核心字段，隐藏JJWT专属API，对外提供统一、稳定的载荷数据
 *
 * @author binblink
 */
@Data
public class JwtInfo {

    // ==================== 官方核心字段 Getter & Setter ====================
    /**
     * JWT唯一标识（jti）
     * 用于防止令牌重放攻击，唯一标识每一个JWT
     */
    private String jwtId;

    /**
     * 签发者（iss）
     * 标识该JWT的签发主体（如系统名称、服务地址）
     */
    private String issuer;

    /**
     * 主题（sub）
     * 标识JWT的核心主题（通常为用户ID、业务唯一标识等）
     */
    private String subject;

    /**
     * 受众（aud）
     * 标识JWT的接收对象/目标群体（可多个，如多个微服务）
     */
    private Set<String> audience;

    /**
     * 过期时间（exp）
     * 标识JWT的失效时间，过期后令牌将无法通过验证
     */
    private Date expiration;

    /**
     * 生效时间（nbf）
     * 标识JWT的生效时间，在此时间之前令牌无法通过验证
     */
    private Date notBefore;

    /**
     * 签发时间（iat）
     * 标识JWT的创建时间，用于记录令牌生成时间戳
     */
    private Date issuedAt;


    /**
     * 自定义业务数据
     * 存储除官方核心字段外的业务自定义信息（如角色、权限、用户名等）
     */
    private Map<String, Object> customData;

    // ==================== 自定义数据 ====================

    /** 剩余有效期（毫秒） */
    private long remainingValidity;

    /** Token类型 (access 或 refresh) */
    private String tokenType;

    /** 用户角色 */
    private List<String> roles;
    /**
     * 无参构造器
     * 初始化集合和Map，避免空指针异常
     */
    public JwtInfo() {
        this.audience = new HashSet<>();
        this.customData = new HashMap<>();
    }


    // ==================== 自定义业务数据 Getter & Setter & 便捷方法 ====================
    /**
     * 获取所有自定义业务数据
     */
    public Map<String, Object> getCustomData() {
        return customData;
    }

    /**
     * 设置所有自定义业务数据
     */
    public void setCustomData(Map<String, Object> customData) {
        this.customData = customData;
    }

    /**
     * 便捷方法：获取单个自定义业务数据
     * 泛型封装，避免外部模块强制类型转换，简化使用
     * @param key 自定义数据key
     * @param <T> 自定义数据类型
     * @return 自定义数据值（无对应key返回null）
     */
    @SuppressWarnings("unchecked")
    public <T> T getCustomValue(String key) {
        return (T) this.customData.get(key);
    }

    /**
     * 便捷方法：添加单个自定义业务数据
     * 避免外部模块直接操作Map，提高数据安全性
     * @param key 自定义数据key
     * @param value 自定义数据value
     */
    public void addCustomValue(String key, Object value) {
        this.customData.put(key, value);
    }


}