package com.blink.job.core.registry;

import com.blink.job.api.dto.JobInfo;
import com.blink.job.api.enums.JobStatus;

import java.util.List;

/**
 * 任务注册中心接口
 * 统一管理所有任务的注册、查询、启停
 *
 * @author binblink
 */
public interface JobRegistry {

    /**
     * 注册任务
     *
     * @param jobInfo 任务信息
     */
    void register(JobInfo jobInfo);

    /**
     * 注销任务
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     */
    void unregister(String jobName, String jobGroup);

    /**
     * 获取所有任务
     *
     * @return 任务列表
     */
    List<JobInfo> getAllJobs();

    /**
     * 根据名称获取任务
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     * @return 任务信息
     */
    JobInfo getJob(String jobName, String jobGroup);

    /**
     * 更新任务状态
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     * @param status   任务状态
     */
    void updateStatus(String jobName, String jobGroup, JobStatus status);

    /**
     * 是否已注册
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     * @return 是否已注册
     */
    boolean isRegistered(String jobName, String jobGroup);
}
