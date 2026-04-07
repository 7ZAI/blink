package com.blink.base.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 查询任务请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
public class QueryTaskReq extends Page {

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