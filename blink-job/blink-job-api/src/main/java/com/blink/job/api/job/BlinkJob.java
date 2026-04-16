package com.blink.job.api.job;

import cn.hutool.core.util.StrUtil;
import com.blink.job.api.dto.JobContext;
import com.blink.job.api.dto.JobExecutionResult;

/**
 * Blink 定时任务接口
 * 复杂任务实现此接口，支持更灵活的控制
 *
 * @author binblink
 */
public interface BlinkJob {

    /**
     * 执行任务
     *
     * @param context 执行上下文
     * @return 执行结果
     */
    JobExecutionResult execute(JobContext context);

    /**
     * 任务名称（默认取类名首字母小写）
     */
    default String getName() {
        return StrUtil.lowerFirst(this.getClass().getSimpleName());
    }

    /**
     * 任务分组
     */
    default String getGroup() {
        return "default";
    }

    /**
     * 任务描述
     */
    default String getDescription() {
        return "";
    }
}
