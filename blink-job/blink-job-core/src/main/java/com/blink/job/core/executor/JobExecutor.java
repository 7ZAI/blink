package com.blink.job.core.executor;

import cn.hutool.core.util.ObjectUtil;
import com.blink.job.api.dto.JobContext;
import com.blink.job.api.dto.JobExecutionResult;
import com.blink.job.api.dto.JobInfo;
import com.blink.job.api.enums.JobType;
import com.blink.job.api.job.BlinkJob;
import com.blink.job.core.alarm.JobAlarmHandler;
import com.blink.job.core.constants.JobConstant;
import com.blink.job.core.registry.JobRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 任务执行器
 * 负责任务的实际执行、异常处理、重试逻辑
 *
 * @author binblink
 */
@Slf4j
public class JobExecutor {

    private final JobRegistry jobRegistry;
    private final JobAlarmHandler alarmHandler;
    private final ApplicationContext applicationContext;

    public JobExecutor(JobRegistry jobRegistry,
                       JobAlarmHandler alarmHandler,
                       ApplicationContext applicationContext) {
        this.jobRegistry = jobRegistry;
        this.alarmHandler = alarmHandler;
        this.applicationContext = applicationContext;
    }

    /**
     * 执行任务
     *
     * @param jobInfo 任务信息
     * @param context 执行上下文
     * @return 执行结果
     */
    public JobExecutionResult execute(JobInfo jobInfo, JobContext context) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取执行目标
            Object target = getTarget(jobInfo);

            // 2. 执行任务
            JobExecutionResult result = doExecute(target, jobInfo, context);

            // 3. 失败重试
            if (!Boolean.TRUE.equals(result.getSuccess()) && ObjectUtil.isNotNull(jobInfo.getRetryCount()) && jobInfo.getRetryCount() > 0) {
                result = retryExecute(jobInfo, context, result);
            }

            // 4. 设置耗时
            long duration = System.currentTimeMillis() - startTime;
            result.setDuration(duration);

            return result;

        } catch (Exception e) {
            log.error("[JobExecutor] 任务执行异常 | jobName: {}", jobInfo.getName(), e);
            JobExecutionResult result = JobExecutionResult.failure(e);
            result.setDuration(System.currentTimeMillis() - startTime);
            return result;
        }
    }

    /**
     * 实际执行（支持注解方法和接口实现两种方式）
     */
    private JobExecutionResult doExecute(Object target, JobInfo jobInfo, JobContext context) {
        try {
            if (jobInfo.getType() == JobType.METHOD) {
                // 反射调用注解方法
                return invokeMethod(target, jobInfo.getTargetMethod(), context);
            } else {
                // 调用 BlinkJob 接口
                return ((BlinkJob) target).execute(context);
            }
        } catch (Exception e) {
            log.error("[JobExecutor] 任务执行失败 | jobName: {}, method: {}",
                    jobInfo.getName(), jobInfo.getTargetMethod(), e);
            return JobExecutionResult.failure(e);
        }
    }

    /**
     * 反射调用方法
     */
    private JobExecutionResult invokeMethod(Object target, String methodName, JobContext context) throws Exception {
        Class<?> clazz = target.getClass();
        Method method = findMethod(clazz, methodName);

        if (method == null) {
            return JobExecutionResult.failure("Method not found: " + methodName);
        }

        method.setAccessible(true);

        // 根据方法参数类型决定如何调用
        Class<?>[] paramTypes = method.getParameterTypes();
        Object result;

        if (paramTypes.length == 0) {
            result = method.invoke(target);
        } else if (paramTypes.length == 1 && paramTypes[0] == JobContext.class) {
            result = method.invoke(target, context);
        } else {
            return JobExecutionResult.failure("Unsupported method signature: " + methodName);
        }

        // 处理返回值
        if (result instanceof JobExecutionResult) {
            return (JobExecutionResult) result;
        }
        return JobExecutionResult.success();
    }

    /**
     * 查找方法
     */
    private Method findMethod(Class<?> clazz, String methodName) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        // 尝试在父类中查找
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return findMethod(superClass, methodName);
        }
        return null;
    }

    /**
     * 重试执行
     */
    private JobExecutionResult retryExecute(JobInfo jobInfo, JobContext context,
                                            JobExecutionResult lastResult) {
        int maxRetry = jobInfo.getRetryCount();
        long interval = ObjectUtil.isNotNull(jobInfo.getRetryInterval()) ? jobInfo.getRetryInterval() : JobConstant.DEFAULT_RETRY_INTERVAL_MS;

        for (int i = 1; i <= maxRetry; i++) {
            try {
                Thread.sleep(interval);
                log.info("[JobExecutor] 任务重试 | jobName: {}, retryCount: {}/{}",
                        jobInfo.getName(), i, maxRetry);

                Object target = getTarget(jobInfo);
                JobContext retryContext = context.withExecuteCount(i);
                JobExecutionResult result = doExecute(target, jobInfo, retryContext);

                if (Boolean.TRUE.equals(result.getSuccess())) {
                    log.info("[JobExecutor] 任务重试成功 | jobName: {}, retryCount: {}",
                            jobInfo.getName(), i);
                    return result;
                }
                lastResult = result;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[JobExecutor] 任务重试被中断 | jobName: {}", jobInfo.getName());
                break;
            }
        }

        log.error("[JobExecutor] 任务重试耗尽 | jobName: {}, maxRetry: {}",
                jobInfo.getName(), maxRetry);
        return lastResult;
    }

    /**
     * 获取执行目标 Bean
     */
    private Object getTarget(JobInfo jobInfo) {
        return applicationContext.getBean(jobInfo.getTargetBean());
    }

    /**
     * 构建执行上下文
     */
    public JobContext buildContext(JobInfo jobInfo) {
        return JobContext.builder()
                .jobId(UUID.randomUUID().toString())
                .jobName(jobInfo.getName())
                .jobGroup(jobInfo.getGroup())
                .triggerTime(LocalDateTime.now())
                .executeCount(0)
                .build();
    }
}
