package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 缓存同步请求
 *
 * @author binblink
 */
@Data
public class CacheSyncReq implements Serializable {

    /**
     * 同步类型: channel / route / config
     */
    private String type;

    /**
     * 指定同步的 key 列表
     */
    private List<String> keys;

    /**
     * 是否全量同步
     */
    private Boolean syncAll;
}