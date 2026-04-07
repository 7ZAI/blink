package com.blink.gateway.admin.dto.rsp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 缓存一致性检查响应
 *
 * 检查三方数据的一致性：
 * 1. 数据库源数据
 * 2. Redis 缓存
 * 3. 各 gateway-reactive 实例的本地缓存
 *
 * @author binblink
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheCheckRsp implements Serializable {

    /**
     * 检查类型: channel / route / config
     */
    private String type;

    /**
     * 数据库中的数据（源数据）
     */
    private List<CacheItemStatus> dbItems;

    /**
     * Redis 缓存状态
     */
    private List<CacheItemStatus> redisItems;

    /**
     * 各实例的本地缓存状态
     */
    private List<InstanceCacheStatus> instances;

    /**
     * 检查时间
     */
    private LocalDateTime checkTime;
}