package com.blink.gateway.admin.dto.rsp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 缓存项状态
 *
 * @author binblink
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheItemStatus implements Serializable {

    /**
     * 缓存 key
     */
    private String key;

    /**
     * 状态: MATCH / MISMATCH / MISSING
     */
    private String status;

    /**
     * checksum
     */
    private String checksum;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}