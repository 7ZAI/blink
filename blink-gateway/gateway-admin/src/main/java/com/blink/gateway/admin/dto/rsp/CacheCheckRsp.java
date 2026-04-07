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
 * @author binblink
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheCheckRsp implements Serializable {

    /**
     * 检查类型
     */
    private String type;

    /**
     * 数据库中的数据
     */
    private List<CacheItemStatus> dbItems;

    /**
     * 各实例的缓存状态
     */
    private List<InstanceCacheStatus> instances;

    /**
     * 检查时间
     */
    private LocalDateTime checkTime;
}