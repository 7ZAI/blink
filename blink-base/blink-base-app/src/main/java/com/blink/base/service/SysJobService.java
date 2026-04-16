package com.blink.base.service;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.SysJobRsp;

/**
 * 定时任务服务接口
 *
 * @author binblink
 */
public interface SysJobService {

    /**
     * 分页查询任务列表
     */
    SysJobRsp getJobList(QuerySysJobReq req);

    /**
     * 新增任务
     */
    void addJob(AddSysJobReq req);

    /**
     * 更新任务
     */
    void updateJob(UpdateSysJobReq req);

    /**
     * 删除任务
     */
    void deleteJob(DeleteSysJobReq req);

    /**
     * 暂停任务
     */
    void pauseJob(JobIdReq req);

    /**
     * 恢复任务
     */
    void resumeJob(JobIdReq req);

    /**
     * 立即执行一次
     */
    void triggerJob(JobIdReq req);
}
