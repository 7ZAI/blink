package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 缓存一致性检查请求
 *
 * @author binblink
 */
@Data
public class CacheCheckReq implements Serializable {

    /**
     * 检查类型: channel / route / config
     */
    private String type;

    /**
     * 指定检查的 key 列表，为空则检查全部
     */
    private List<String> keys;
}