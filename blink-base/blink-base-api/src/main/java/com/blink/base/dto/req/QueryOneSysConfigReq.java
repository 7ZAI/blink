package com.blink.base.dto.req;

import lombok.Data;

/**
 * @Author binblink
 * @Date 2025/10/15
 */
@Data
public class QueryOneSysConfigReq {

    /**
     * 主键ID
     */
    private Integer id;


    /**
     * 参数键名
     */
    private String configKey;
}
