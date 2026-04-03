package com.blink.gateway.base.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author binblink
 * @Date 2025/10/15
 */
@Data
public class QueryOneSysConfigReq implements Serializable {

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
