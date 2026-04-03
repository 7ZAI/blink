package com.blink.gateway.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询单个系统配置请求参数
 *
 * @author binblink
 */
@Data
public class QueryChannelConfigReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Integer id;

    /**
     * 参数键名
     */
    private String configKey;
}