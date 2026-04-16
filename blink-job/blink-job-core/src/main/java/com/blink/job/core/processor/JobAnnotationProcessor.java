package com.blink.job.core.processor;

import com.blink.job.api.annotation.BlinkScheduled;
import com.blink.job.api.dto.JobInfo;
import com.blink.job.api.enums.JobStatus;
import com.blink.job.api.enums.JobType;
import com.blink.job.core.registry.JobRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * @BlinkScheduled 注解处理器
 * 扫描 Spring Bean 中的注解方法并注册到注册中心
 *
 * @author binblink
 */
@Slf4j
@Component
public class JobAnnotationProcessor implements BeanPostProcessor {

    private final JobRegistry jobRegistry;

    public JobAnnotationProcessor(JobRegistry jobRegistry) {
        this.jobRegistry = jobRegistry;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();

        // 遍历所有方法，查找 @BlinkScheduled 注解
        for (Method method : clazz.getDeclaredMethods()) {
            BlinkScheduled annotation = method.getAnnotation(BlinkScheduled.class);
            if (annotation != null) {
                JobInfo jobInfo = buildJobInfo(annotation, beanName, method);
                jobRegistry.register(jobInfo);

                log.info("[JobAnnotationProcessor] 注册注解任务 | jobName: {}, bean: {}, method: {}, cron: {}",
                        annotation.name(), beanName, method.getName(), annotation.cron());
            }
        }

        return bean;
    }

    /**
     * 构建任务信息
     */
    private JobInfo buildJobInfo(BlinkScheduled annotation, String beanName, Method method) {
        JobInfo jobInfo = new JobInfo();
        jobInfo.setName(annotation.name());
        jobInfo.setGroup(annotation.group());
        jobInfo.setDescription(annotation.description());
        jobInfo.setCron(annotation.cron());
        jobInfo.setEnabled(annotation.enabled());
        jobInfo.setTimeout(annotation.timeout());
        jobInfo.setRetryCount(annotation.retryCount());
        jobInfo.setRetryInterval(annotation.retryInterval());
        jobInfo.setType(JobType.METHOD);
        jobInfo.setTargetBean(beanName);
        jobInfo.setTargetMethod(method.getName());
        jobInfo.setStatus(JobStatus.NORMAL);
        jobInfo.setCreateTime(LocalDateTime.now());
        jobInfo.setUpdateTime(LocalDateTime.now());

        return jobInfo;
    }
}
