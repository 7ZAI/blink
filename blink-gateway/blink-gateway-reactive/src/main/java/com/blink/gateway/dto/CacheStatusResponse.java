package com.blink.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 缓存状态响应
 *
 * @author binblink
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStatusResponse {

    /**
     * 实例 ID
     */
    private String instanceId;

    /**
     * 缓存类型: channel / route / config
     */
    private String type;

    /**
     * 查询时间
     */
    private LocalDateTime timestamp;

    /**
     * 缓存项列表
     */
    private List<CacheItemDTO> items;
}