package com.blink.framework.core.config;

import com.blink.framework.core.config.prop.ThreadPoolProperties;
import com.blink.framework.core.data.CoreConstant;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置 配合@Async使用
 * @author binblink
 */
@EnableAsync
@AutoConfiguration
@EnableConfigurationProperties({ThreadPoolProperties.class})
public class DynamicThreadPoolAutoConfig {
    
    @Resource
    private ThreadPoolProperties threadPoolProperties;
    
    // 获取CPU核心数
    private int getCpuCores() {
        return Runtime.getRuntime().availableProcessors();
    }
    
    /**
     * CPU密集型任务线程池
     * 适用于计算密集型任务，如数据处理、复杂计算
     */
    @Bean(CoreConstant.CPU_THREADPOOL)
    @ConditionalOnProperty(prefix = "blink.web.async.thread-pool.core",name = "enabled", havingValue = "true")
    public Executor cpuIntensiveThreadPool() {
        ThreadPoolProperties.PoolConfig config = calculateOptimalConfig(threadPoolProperties.getCore());

        // 对于CPU密集型任务，进一步优化参数
        if (config.getDynamicBasedOnCpu()) {
            int cpuCores = getCpuCores();
            // CPU密集型任务：线程数 ≈ CPU核心数 + 1
            config.setCoreSize(Math.min(cpuCores + 1, config.getMaxLimit()));
            // 最大线程数稍大一些，但不要太大
            config.setMaxSize(Math.min(cpuCores * 2, config.getMaxLimit()));
            // 使用有界队列，防止任务积压
            config.setQueueCapacity(config.getCoreSize() * 50);
        }
        ThreadPoolTaskExecutor executor = getThreadPoolTaskExecutor(config,"cpu-intensive-");
        // 拒绝策略
        // CallerRunsPolicy: 由调用线程处理该任务
        // AbortPolicy: 丢弃任务并抛出异常
        // DiscardPolicy: 丢弃任务，不抛出异常
        // DiscardOldestPolicy: 丢弃队列最前面的任务，然后重新尝试执行任务
        // CPU密集型任务使用CallerRunsPolicy，避免线程过多导致CPU过载
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        printPoolConfig("CPU密集型线程池", executor, config);
        executor.initialize();
        return executor;
    }



    /**
     * IO密集型任务线程池
     * 适用于网络请求、数据库操作、文件读写等
     */
    @Bean(CoreConstant.IO_THREADPOOL)
    @ConditionalOnProperty(prefix = "blink.web.async.thread-pool.io",name = "enabled", havingValue = "true")
    public Executor ioIntensiveThreadPool() {
        ThreadPoolProperties.PoolConfig config = calculateOptimalConfig(threadPoolProperties.getIo());
        // 对于IO密集型任务，进一步优化参数
        if (config.getDynamicBasedOnCpu()) {
            int cpuCores = getCpuCores();
            // IO密集型任务：线程数 ≈ CPU核心数 * 2 ~ 4
            config.setCoreSize(Math.min(cpuCores * 2, config.getMaxLimit()));
            // 可以设置更多线程处理并发IO
            config.setMaxSize(Math.min(cpuCores * 4, config.getMaxLimit()));
            // IO任务可能较多，使用较大的队列
            config.setQueueCapacity(config.getCoreSize() * 100);
        }
        ThreadPoolTaskExecutor executor = getThreadPoolTaskExecutor(config,"io-intensive-");

        // IO密集型任务可以使用DiscardPolicy，避免内存溢出
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        printPoolConfig("IO密集型线程池", executor, config);
        executor.initialize();
        return executor;
    }

    /**
     * 定时任务线程池
     */
    @Bean(CoreConstant.SCHEDULED_THREADPOOL)
    @ConditionalOnProperty(prefix = "blink.web.async.thread-pool.scheduled",name = "enabled", havingValue = "true")
    public Executor scheduledThreadPool() {
        ThreadPoolProperties.PoolConfig config = calculateOptimalConfig(threadPoolProperties.getScheduled());

        // 定时任务不需要太多线程
        if (config.getDynamicBasedOnCpu()) {
            int cpuCores = getCpuCores();
            // 定时任务：使用较少的线程
            config.setCoreSize(Math.max(1, cpuCores / 2));
            config.setMaxSize(cpuCores);
            config.setQueueCapacity(50);
        }
        
        ThreadPoolTaskExecutor executor = getThreadPoolTaskExecutor(config,"scheduled-");
        // 定时任务使用AbortPolicy，任务丢弃时需要记录日志
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        printPoolConfig("定时任务线程池", executor, config);
        executor.initialize();
        return executor;
    }

    // 根据CPU核心数智能计算线程数
    private ThreadPoolProperties.PoolConfig calculateOptimalConfig(
            ThreadPoolProperties.PoolConfig config) {

        if (config == null) {
            config = new ThreadPoolProperties.PoolConfig();
        }

        int cpuCores = getCpuCores();

        // 如果启用动态计算
        if (config.getDynamicBasedOnCpu()) {
            // 根据CPU核心数计算推荐值
            if (config.getCoreSize() == null) {
                // CPU密集型：核心数 + 1
                // IO密集型：核心数 * 2
                int calculatedCoreSize = (int) Math.max(
                        config.getMinCoreSize(),
                        Math.ceil(cpuCores * config.getCoreMultiplier())
                );
                // 不超过上限
                config.setCoreSize(Math.min(calculatedCoreSize, config.getMaxLimit()));
            }

            if (config.getMaxSize() == null) {
                int calculatedMaxSize = (int) Math.max(
                        config.getCoreSize(),
                        Math.ceil(cpuCores * config.getMaxMultiplier())
                );
                config.setMaxSize(Math.min(calculatedMaxSize, config.getMaxLimit()));
            }
        }

        // 设置默认值
        if (config.getQueueCapacity() == null) {
            // 队列容量基于核心线程数动态调整
            config.setQueueCapacity(config.getCoreSize() * 100);
        }

        if (config.getKeepAliveSeconds() == null) {
            config.setKeepAliveSeconds(60);
        }

        return config;
    }

    /**
     * 创建线程池设置配置
     * @param config
     * @param defaultName
     * @return
     */
    private static ThreadPoolTaskExecutor getThreadPoolTaskExecutor(ThreadPoolProperties.PoolConfig config,String defaultName) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getCoreSize());
        executor.setMaxPoolSize(config.getMaxSize());
        executor.setQueueCapacity(config.getQueueCapacity());
        executor.setKeepAliveSeconds(config.getKeepAliveSeconds());
        executor.setThreadNamePrefix(config.getThreadNamePrefix() != null ?
                config.getThreadNamePrefix() : defaultName);

        return executor;
    }


    
    /**
     * 打印线程池配置信息
     */
    private void printPoolConfig(String poolName, ThreadPoolTaskExecutor executor, 
                                ThreadPoolProperties.PoolConfig config) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println(poolName + "配置信息:");
        System.out.println("CPU核心数: " + getCpuCores());
        System.out.println("核心线程数: " + executor.getCorePoolSize());
        System.out.println("最大线程数: " + executor.getMaxPoolSize());
        System.out.println("队列容量: " + executor.getQueueCapacity());
        System.out.println("线程名前缀: " + executor.getThreadNamePrefix());
        System.out.println("是否动态计算: " + config.getDynamicBasedOnCpu());
        System.out.println("=".repeat(50) + "\n");
    }
}