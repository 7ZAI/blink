package com.blink.job.core.registry;

import com.blink.job.api.dto.JobInfo;
import com.blink.job.api.enums.JobStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认任务注册中心实现（内存版）
 *
 * @author binblink
 */
@Slf4j
@Component
public class DefaultJobRegistry implements JobRegistry {

    private final Map<String, JobInfo> jobMap = new ConcurrentHashMap<>();

    private String buildKey(String jobName, String jobGroup) {
        return jobGroup + ":" + jobName;
    }

    @Override
    public void register(JobInfo jobInfo) {
        String key = buildKey(jobInfo.getName(), jobInfo.getGroup());
        JobInfo existing = jobMap.put(key, jobInfo);
        if (existing != null) {
            log.info("[JobRegistry] 任务已更新 | jobName: {}, jobGroup: {}",
                    jobInfo.getName(), jobInfo.getGroup());
        } else {
            log.info("[JobRegistry] 任务已注册 | jobName: {}, jobGroup: {}, cron: {}",
                    jobInfo.getName(), jobInfo.getGroup(), jobInfo.getCron());
        }
    }

    @Override
    public void unregister(String jobName, String jobGroup) {
        String key = buildKey(jobName, jobGroup);
        jobMap.remove(key);
        log.info("[JobRegistry] 任务已注销 | jobName: {}, jobGroup: {}", jobName, jobGroup);
    }

    @Override
    public List<JobInfo> getAllJobs() {
        return List.copyOf(jobMap.values());
    }

    @Override
    public JobInfo getJob(String jobName, String jobGroup) {
        String key = buildKey(jobName, jobGroup);
        return jobMap.get(key);
    }

    @Override
    public void updateStatus(String jobName, String jobGroup, JobStatus status) {
        JobInfo jobInfo = getJob(jobName, jobGroup);
        if (jobInfo != null) {
            jobInfo.setStatus(status);
            log.info("[JobRegistry] 任务状态已更新 | jobName: {}, status: {}", jobName, status);
        }
    }

    @Override
    public boolean isRegistered(String jobName, String jobGroup) {
        String key = buildKey(jobName, jobGroup);
        return jobMap.containsKey(key);
    }
}
