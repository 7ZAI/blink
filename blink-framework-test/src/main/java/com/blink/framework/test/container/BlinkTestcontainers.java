package com.blink.framework.test.container;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Blink Testcontainers 统一配置
 * 提供容器生命周期管理和连接信息
 *
 * 使用方式：
 * <pre>
 * @IntegrationTest
 * class MyIntegrationTest extends BlinkTestcontainers {
 *     // 自动获得 MySQL 和 Redis 容器
 * }
 * </pre>
 *
 * 注意：继承此类会自动启动 MySQL 和 Redis 容器
 *
 * @author binblink
 * @since 2026-04-16
 */
@Testcontainers
public abstract class BlinkTestcontainers {

    /**
     * MySQL 容器（共享，所有子类共用）
     */
    @Container
    protected static final MySQLContainer<?> MYSQL =
            MySQLContainerConfig.create();

    /**
     * Redis 容器（共享，所有子类共用）
     */
    @Container
    protected static final GenericContainer<?> REDIS =
            RedisContainerConfig.create();

    /**
     * 动态配置 Spring Boot 属性
     * 容器启动后自动注入连接信息
     *
     * @param registry 动态属性注册器
     */
    @DynamicPropertySource
    static void configureContainers(DynamicPropertyRegistry registry) {
        // MySQL 配置
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // Redis 配置
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    /**
     * 获取 MySQL JDBC URL
     *
     * @return JDBC URL
     */
    protected static String getMySQLJdbcUrl() {
        return MYSQL.getJdbcUrl();
    }

    /**
     * 获取 Redis 连接 URL
     *
     * @return Redis URL（格式：redis://host:port）
     */
    protected static String getRedisUrl() {
        return RedisContainerConfig.getRedisUrl(REDIS);
    }
}