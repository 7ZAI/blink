package com.blink.framework.core.util;

import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * 支持 MDC 上下文传递的自定义线程池
 * 核心：提交任务时自动复制父线程 MDC，子线程执行时设置 MDC，执行后清空
 */
public class MdcThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

    // 处理 Callable 任务（有返回值）
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        // 1. 获取父线程的 MDC 上下文（traceId/userId 等）
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        // 2. 包装任务，子线程中设置 MDC
        Callable<T> wrappedTask = () -> {
            try {
                // 子线程设置父线程的 MDC 上下文
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }
                // 执行原任务
                return task.call();
            } finally {
                // 3. 子线程任务结束后清空 MDC（关键：避免线程复用导致脏数据）
                MDC.clear();
            }
        };
        return super.submit(wrappedTask);
    }

    // 处理 Runnable 任务（无返回值）
    @Override
    public void execute(Runnable task) {
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        Runnable wrappedRunnable = () -> {
            try {
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }
                task.run();
            } finally {
                MDC.clear();
            }
        };
        super.execute(wrappedRunnable);
    }

    // 兼容 submit(Runnable) 方法（可选，确保全覆盖）
    @Override
    public Future<?> submit(Runnable task) {
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        Runnable wrappedRunnable = () -> {
            try {
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }
                task.run();
            } finally {
                MDC.clear();
            }
        };
        return super.submit(wrappedRunnable);
    }
}