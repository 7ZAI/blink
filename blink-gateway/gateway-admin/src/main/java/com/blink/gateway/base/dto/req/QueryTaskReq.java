package com.blink.gateway.base.dto.req;

import com.blink.framework.common.data.PageDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询任务请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
public class QueryTaskReq extends PageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 任务名称（模糊查询）
     */
    private String taskName;

    /**
     * 流程定义KEY
     */
    private String processDefinitionKey;
}