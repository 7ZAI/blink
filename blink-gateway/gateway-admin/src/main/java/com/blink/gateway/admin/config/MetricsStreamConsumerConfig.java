package com.blink.gateway.admin.config;

import com.blink.gateway.admin.service.MetricsStreamConsumer;
import com.blink.gateway.base.service.SysConfigService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis Stream 消费者配置
 *
 * 负责启动和停止 Stream 消费者，消费 gateway-reactive 上报的指标消息
 *
 * 实现 SmartLifecycle 接口，确保在 Redis 连接关闭之前先停止消费者
 *
 * 启用条件：
 * - 数据库配置 monitor.enabled=true（默认开启）
 *
 * 配置来源：
 * - 从数据库 sys_config 表读取 monitor.enabled 配置
 *
 * @author binblink
 * @since 2026-04-14
 */
@Configuration
@EnableConfigurationProperties(MonitorProperties.class)
@Slf4j
public class MetricsStreamConsumerConfig implements SmartLifecycle {

    private static final String MONITOR_ENABLED_KEY = "monitor.enabled";

    private final MonitorProperties monitorProperties;
    private final MetricsStreamConsumer metricsStreamConsumer;
    private final StringRedisTemplate redisTemplate;
    private final RedisConnectionFactory redisConnectionFactory;
    private final SysConfigService sysConfigService;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public MetricsStreamConsumerConfig(MonitorProperties monitorProperties,
                                        MetricsStreamConsumer metricsStreamConsumer,
                                        StringRedisTemplate redisTemplate,
                                        RedisConnectionFactory redisConnectionFactory,
                                        SysConfigService sysConfigService) {
        this.monitorProperties = monitorProperties;
        this.metricsStreamConsumer = metricsStreamConsumer;
        this.redisTemplate = redisTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
        this.sysConfigService = sysConfigService;
    }

    @PostConstruct
    public void init() {
        // 从数据库读取监控开关配置
        if (!isMonitorEnabled()) {
            log.info("[MetricsStream] 监控已禁用，跳过 Stream 消费者启动");
            return;
        }

        startConsumer();
    }

    /**
     * 检查监控是否启用
     * 从数据库读取配置，优先使用数据库配置
     */
    private boolean isMonitorEnabled() {
        Boolean dbEnabled = sysConfigService.getBooleanConfig(MONITOR_ENABLED_KEY, null);
        if (dbEnabled != null) {
            return dbEnabled;
        }
        // 使用配置文件默认值
        return Boolean.TRUE.equals(monitorProperties.getEnabled());
    }

    /**
     * 启动 Stream 消费者
     */
    private void startConsumer() {
        MonitorProperties.StreamConsumeConfig config = monitorProperties.getStreamConsume();
        String streamKey = config.getStreamKey();
        String consumerGroup = config.getConsumerGroup();
        String consumerName = config.getConsumerNamePrefix() + System.currentTimeMillis();

        try {
            // 创建消费者组（如果不存在）
            createConsumerGroupIfNotExists(streamKey, consumerGroup);

            // 创建监听器容器选项
            StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                    StreamMessageListenerContainerOptions.builder()
                            .pollTimeout(Duration.ofMillis(config.getPollIntervalMs()))
                            .batchSize(config.getBatchSize())
                            .executor(taskExecutor())
                            .build();

            // 创建监听器容器
            container = StreamMessageListenerContainer.create(redisConnectionFactory, options);

            // 注册监听器
            container.receive(
                    Consumer.from(consumerGroup, consumerName),
                    StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                    message -> {
                        try {
                            Map<String, String> body = message.getValue();
                            metricsStreamConsumer.processMessage(body);
                            // 确认消息
                            redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, message.getId());
                        } catch (Exception e) {
                            log.error("[MetricsStream] 处理消息失败 | id: {}, error: {}",
                                    message.getId(), e.getMessage());
                        }
                    }
            );

            // 启动容器
            container.start();
            running.set(true);

            log.info("[MetricsStream] Stream 消费者启动成功 | streamKey: {}, consumerGroup: {}, consumerName: {}",
                    streamKey, consumerGroup, consumerName);

        } catch (Exception e) {
            log.error("[MetricsStream] Stream 消费者启动失败 | error: {}", e.getMessage(), e);
        }
    }

    /**
     * 创建消费者组（如果不存在）
     */
    private void createConsumerGroupIfNotExists(String streamKey, String consumerGroup) {
        try {
            // 检查 Stream 是否存在
            Boolean exists = redisTemplate.hasKey(streamKey);
            if (!Boolean.TRUE.equals(exists)) {
                // 创建 Stream 并添加一个初始消息
                redisTemplate.opsForStream().add(streamKey, Map.of("init", "true"));
                log.info("[MetricsStream] 创建 Stream | key: {}", streamKey);
            }

            // 尝试创建消费者组
            try {
                redisTemplate.opsForStream().createGroup(streamKey, consumerGroup);
                log.info("[MetricsStream] 创建消费者组 | streamKey: {}, group: {}", streamKey, consumerGroup);
            } catch (Exception e) {
                // 消费者组已存在
                if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                    log.debug("[MetricsStream] 消费者组已存在 | streamKey: {}, group: {}", streamKey, consumerGroup);
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            log.error("[MetricsStream] 创建消费者组失败 | streamKey: {}, group: {}, error: {}",
                    streamKey, consumerGroup, e.getMessage());
        }
    }

    /**
     * 任务执行器
     */
    private ExecutorService taskExecutor() {
        executorService = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r);
            t.setName("metrics-stream-consumer-" + t.getId());
            t.setDaemon(true);
            return t;
        });
        return executorService;
    }

    /**
     * 动态停止消费者（供外部调用）
     */
    public void stopConsumer() {
        if (container != null && running.get()) {
            try {
                container.stop();
                log.info("[MetricsStream] Stream 消费者已停止");
            } catch (Exception e) {
                log.warn("[MetricsStream] 停止 Stream 消费者时出错 | error: {}", e.getMessage());
            } finally {
                running.set(false);
            }
        }
    }

    /**
     * 动态启动消费者（供外部调用）
     */
    public void startConsumerDynamic() {
        if (!running.get() && isMonitorEnabled()) {
            startConsumer();
        }
    }

    /**
     * 刷新监控状态（供动态配置变更时调用）
     */
    public void refresh() {
        boolean shouldRun = isMonitorEnabled();
        boolean isRunning = running.get();

        if (shouldRun && !isRunning) {
            log.info("[MetricsStream] 监控已启用，启动消费者");
            startConsumer();
        } else if (!shouldRun && isRunning) {
            log.info("[MetricsStream] 监控已禁用，停止消费者");
            stopConsumer();
        }
    }

    // ==================== SmartLifecycle 接口实现 ====================

    /**
     * 启动生命周期组件（由 Spring 容器调用）
     */
    @Override
    public void start() {
        // 已在 @PostConstruct 中启动，此处无需操作
    }

    /**
     * 停止生命周期组件（由 Spring 容器调用）
     * SmartLifecycle 的 stop 方法会在 Redis 等基础设施 Bean 销毁之前调用
     */
    @Override
    public void stop() {
        log.info("[MetricsStream] 开始关闭 Stream 消费者...");

        // 先停止容器
        stopConsumer();

        // 关闭线程池
        if (executorService != null && !executorService.isShutdown()) {
            try {
                executorService.shutdown();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
                log.info("[MetricsStream] 线程池已关闭");
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 是否正在运行
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 返回 false，表示不自动启动（由 @PostConstruct 控制启动）
     */
    @Override
    public boolean isAutoStartup() {
        return false;
    }

    /**
     * 返回阶段值，值越大越先启动、越后停止
     * 使用 Integer.MAX_VALUE 确保在其他 Bean 之前停止
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    /**
     * 停止时的回调（可用于执行停止后的逻辑）
     */
    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }
}
