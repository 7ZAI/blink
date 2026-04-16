package com.blink.job.api.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务执行结果
 *
 * @author binblink
 */
@Data
public class JobExecutionResult {

    /** 是否成功 */
    private Boolean success;

    /**
     * 判断是否成功
     * 提供 isSuccess() 方法以符合布尔属性命名习惯
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(this.success);
    }

    /** 执行耗时（毫秒） */
    private Long duration;

    /** 结果消息 */
    private String message;

    /** 异常信息（失败时） */
    private String errorMessage;

    /** 异常堆栈（失败时） */
    private String errorStackTrace;

    /** 扩展数据（可记录到日志） */
    private Map<String, Object> data;

    // ========== 静态工厂方法 ==========

    public static JobExecutionResult success() {
        JobExecutionResult result = new JobExecutionResult();
        result.setSuccess(true);
        return result;
    }

    public static JobExecutionResult success(String message) {
        JobExecutionResult result = success();
        result.setMessage(message);
        return result;
    }

    public static JobExecutionResult failure(String errorMessage) {
        JobExecutionResult result = new JobExecutionResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }

    public static JobExecutionResult failure(Throwable e) {
        JobExecutionResult result = new JobExecutionResult();
        result.setSuccess(false);
        result.setErrorMessage(e.getMessage());
        result.setErrorStackTrace(getStackTrace(e));
        return result;
    }

    // ========== 链式设置方法 ==========

    public JobExecutionResult setMessage(String message) {
        this.message = message;
        return this;
    }

    public JobExecutionResult setData(Map<String, Object> data) {
        this.data = data;
        return this;
    }

    public JobExecutionResult putData(String key, Object value) {
        if (this.data == null) {
            this.data = new HashMap<>();
        }
        this.data.put(key, value);
        return this;
    }

    private static String getStackTrace(Throwable e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
