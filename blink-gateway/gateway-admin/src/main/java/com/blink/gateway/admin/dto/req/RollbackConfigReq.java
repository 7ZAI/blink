package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 回滚配置请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class RollbackConfigReq implements Serializable {

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
     * 历史版本ID
     */
    private Integer historyId;
}