package com.blink.gateway.admin.dto.rsp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 实例缓存状态
 *
 * @author binblink
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstanceCacheStatus implements Serializable {

    /**
     * 实例 ID
     */
    private String instanceId;

    /**
     * IP 地址
     */
    private String ip;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 各缓存项状态
     */
    private List<CacheItemStatus> items;
}