package com.blink.base.service;

import com.blink.base.dto.req.QuerySysJobLogReq;
import com.blink.base.dto.rsp.SysJobLogRsp;
import com.blink.job.api.dto.JobExecutionResult;

/**
 * 定时任务日志服务接口
 *
 * @author binblink
 */
public interface SysJobLogService {

    /**
     * 分页查询日志列表
     */
    SysJobLogRsp getLogList(QuerySysJobLogReq req);

    /**
     * 保存执行日志
     */
    Long saveLog(Long jobId, String jobName, String jobGroup);

    /**
     * 更新日志结果
     */
    void updateLogResult(Long logId, JobExecutionResult result, long duration);

    /**
     * 清理过期日志
     */
    void cleanExpiredLogs(Integer retentionDays);
}
