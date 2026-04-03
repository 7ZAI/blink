package com.blink.framework.common.factory;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 可命名的线程工厂实现类
 * 支持设置线程名前缀、守护线程模式、优先级、异常处理器等
 *
 * @author binblink
 */
public class BlinkNamedThreadFactory implements ThreadFactory {

    // 线程编号计数器（线程安全）
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    // 线程组（可为null）
    private final ThreadGroup group;

    // 线程名前缀
    private final String namePrefix;

    // 是否为守护线程
    private final boolean daemon;

    // 线程优先级
    private final int priority;

    // 未捕获异常处理器
    private final UncaughtExceptionHandler exceptionHandler;

    /**
     * 私有构造器，通过Builder构建
     */
    private BlinkNamedThreadFactory(Builder builder) {
        this.group = builder.group;
        this.namePrefix = builder.namePrefix;
        this.daemon = builder.daemon;
        this.priority = builder.priority;
        this.exceptionHandler = builder.exceptionHandler;
    }

    @Override
    public Thread newThread(Runnable task) {
        // 创建线程，格式：前缀 + 线程编号
        String threadName = namePrefix + "-" + threadNumber.getAndIncrement();
        Thread thread = new Thread(group, task, threadName);

        // 设置守护线程属性
        thread.setDaemon(daemon);

        // 设置线程优先级
        if (priority > 0) {
            thread.setPriority(Math.min(priority, Thread.MAX_PRIORITY));
        }

        // 设置未捕获异常处理器
        if (exceptionHandler != null) {
            thread.setUncaughtExceptionHandler(exceptionHandler);
        }

        return thread;
    }

    /**
     * 快捷方法：创建具有指定前缀的线程工厂（非守护线程，默认优先级）
     */
    public static BlinkNamedThreadFactory create(String namePrefix) {
        return new Builder(namePrefix).build();
    }

    /**
     * 快捷方法：创建用于IO密集型任务的线程工厂
     */
    public static BlinkNamedThreadFactory createForIO(String moduleName) {
        return new Builder(moduleName + "-IO")
                // 非守护线程
                .daemon(false)
                .priority(Thread.NORM_PRIORITY)
                .exceptionHandler((t, e) -> {
                    System.err.printf("[%s] IO操作异常: %s%n", t.getName(), e.getMessage());
                })
                .build();
    }

    /**
     * 快捷方法：创建用于计算密集型任务的线程工厂
     */
    public static BlinkNamedThreadFactory createForCompute(String moduleName) {
        return new Builder(moduleName + "-Compute")
                .daemon(false)
                .priority(Thread.NORM_PRIORITY)
                .build();
    }

    /**
     * 建造者模式，用于灵活配置线程工厂
     */
    public static class Builder {
        // 必需参数
        private final String namePrefix;

        // 可选参数（带默认值）
        private ThreadGroup group = null;
        private boolean daemon = false;
        private int priority = Thread.NORM_PRIORITY; // 默认优先级
        private UncaughtExceptionHandler exceptionHandler = null;

        /**
         * 构造器
         *
         * @param namePrefix 线程名前缀，必填。示例："Order-Processor"
         */
        public Builder(String namePrefix) {
            if (namePrefix == null || namePrefix.trim().isEmpty()) {
                throw new IllegalArgumentException("线程名前缀不能为空！");
            }
            this.namePrefix = namePrefix.trim();
        }

        public Builder group(ThreadGroup group) {
            this.group = group;
            return this;
        }

        public Builder daemon(boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder exceptionHandler(UncaughtExceptionHandler handler) {
            this.exceptionHandler = handler;
            return this;
        }

        public BlinkNamedThreadFactory build() {
            return new BlinkNamedThreadFactory(this);
        }
    }


}