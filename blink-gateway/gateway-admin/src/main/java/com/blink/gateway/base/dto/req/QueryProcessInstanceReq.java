package com.blink.gateway.base.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 查询流程实例请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
public class QueryProcessInstanceReq extends Page {

    /**
     * 流程定义KEY
     */
    private String processDefinitionKey;

    /**
     * 发起人ID
     */
    private String startUserId;

    /**
     * 流程实例状态（running-运行中, completed-已完成, all-全部）
     */
    private String status = "all";
}