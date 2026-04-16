package com.blink.base.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 查询任务日志请求
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuerySysJobLogReq extends Page {

    private Long jobId;

    private String jobName;

    private Byte status;

    private LocalDateTime triggerTimeStart;

    private LocalDateTime triggerTimeEnd;
}
