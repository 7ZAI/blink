package com.blink.base.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询任务列表请求
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuerySysJobReq extends Page {

    private String jobName;

    private String jobGroup;

    private Byte jobStatus;
}
