package com.blink.gateway.base.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 查询流程定义请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
public class QueryProcessDefinitionReq extends Page {

    /**
     * 流程名称（模糊查询）
     */
    private String name;

    /**
     * 流程KEY
     */
    private String key;

    /**
     * 是否最新版本
     */
    private Boolean latestVersion;
}