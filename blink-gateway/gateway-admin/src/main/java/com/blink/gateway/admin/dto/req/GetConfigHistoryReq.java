package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 获取配置历史请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class GetConfigHistoryReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置DataId
     */
    private String dataId;

    /**
     * 配置Group
     */
    private String group;

    /**
     * 查询数量限制
     */
    private Integer limit;
}