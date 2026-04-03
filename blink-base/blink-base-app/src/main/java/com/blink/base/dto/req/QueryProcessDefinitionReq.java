package com.blink.base.dto.req;

import com.blink.framework.common.data.PageDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询流程定义请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
public class QueryProcessDefinitionReq extends PageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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