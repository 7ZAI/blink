package com.blink.job.xxljob.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.blink.job.api.dto.JobContext;
import com.blink.job.api.dto.JobExecutionResult;
import com.blink.job.api.dto.JobInfo;
import com.blink.job.api.job.BlinkJob;
import com.blink.job.core.executor.JobExecutor;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * XXL-Job 统一处理器
 * 作为 XXL-Job 的入口，将调用委托给 Blink JobExecutor
 *
 * @author binblink
 */
@Slf4j
@Component
public class BlinkXxlJobHandler {

    private final JobExecutor jobExecutor;
    private final Map<String, JobInfo> jobInfoMap = new HashMap<>();
    private final Map<String, BlinkJob> blinkJobMap = new HashMap<>();

    public BlinkXxlJobHandler(JobExecutor jobExecutor) {
        this.jobExecutor = jobExecutor;
    }

    /**
     * XXL-Job 统一入口
     * 任务参数格式: {"jobName":"xxx","jobGroup":"xxx"} 或直接 jobName
     */
    @XxlJob("blinkJobExecutor")
    public void execute() {
        String jobParam = XxlJobHelper.getJobParam();
        log.info("[BlinkXxlJobHandler] 收到 XXL-Job 调度 | param: {}", jobParam);

        try {
            // 解析任务参数
            String jobName = parseJobName(jobParam);
            String jobGroup = parseJobGroup(jobParam);

            // 查找任务
            JobInfo jobInfo = findJobInfo(jobName, jobGroup);
            if (jobInfo == null) {
                XxlJobHelper.handleFail("任务未找到: " + jobName);
                return;
            }

            // 构建执行上下文
            JobContext context = JobContext.builder()
                    .jobId(String.valueOf(XxlJobHelper.getJobId()))
                    .jobName(jobInfo.getName())
                    .jobGroup(jobInfo.getGroup())
                    .triggerTime(LocalDateTime.now())
                    .executeCount(0)
                    .build();

            // 执行任务
            JobExecutionResult result = jobExecutor.execute(jobInfo, context);

            // 返回结果给 XXL-Job
            if (result.isSuccess()) {
                String msg = StrUtil.isNotBlank(result.getMessage()) ? result.getMessage() : "执行成功";
                XxlJobHelper.handleSuccess(msg);
            } else {
                XxlJobHelper.handleFail(result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("[BlinkXxlJobHandler] 执行异常", e);
            XxlJobHelper.handleFail(e.getMessage());
        }
    }

    /**
     * 解析任务名称
     */
    private String parseJobName(String jobParam) {
        if (StrUtil.isBlank(jobParam)) {
            return "";
        }
        // 尝试解析 JSON
        if (jobParam.startsWith("{")) {
            Map<String, Object> map = JSONUtil.toBean(jobParam, Map.class);
            return (String) map.getOrDefault("jobName", "");
        }
        // 直接是任务名称
        return jobParam;
    }

    /**
     * 解析任务分组
     */
    private String parseJobGroup(String jobParam) {
        if (StrUtil.isBlank(jobParam) || !jobParam.startsWith("{")) {
            return "default";
        }
        Map<String, Object> map = JSONUtil.toBean(jobParam, Map.class);
        return (String) map.getOrDefault("jobGroup", "default");
    }

    /**
     * 查找任务信息
     */
    private JobInfo findJobInfo(String jobName, String jobGroup) {
        String key = jobGroup + ":" + jobName;
        return jobInfoMap.get(key);
    }

    /**
     * 注册任务（供初始化器调用）
     */
    public void registerJob(JobInfo jobInfo) {
        String key = jobInfo.getGroup() + ":" + jobInfo.getName();
        jobInfoMap.put(key, jobInfo);
        log.info("[BlinkXxlJobHandler] 注册任务 | jobName: {}, jobGroup: {}", jobInfo.getName(), jobInfo.getGroup());
    }

    /**
     * 注册 BlinkJob（供初始化器调用）
     */
    public void registerBlinkJob(BlinkJob blinkJob) {
        String key = blinkJob.getGroup() + ":" + blinkJob.getName();
        blinkJobMap.put(key, blinkJob);
    }
}
