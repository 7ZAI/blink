package com.blink.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 缓存项 DTO
 *
 * @author binblink
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheItemDTO {

    /**
     * 缓存 key
     */
    private String key;

    /**
     * 数据 checksum (MD5)
     */
    private String checksum;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}