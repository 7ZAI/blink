package com.blink.framework.test.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Redis 容器配置工厂
 * 提供统一的 Redis Testcontainers 配置
 *
 * @author binblink
 * @since 2026-04-16
 */
public class RedisContainerConfig {

    /**
     * Redis 默认镜像版本
     */
    public static final String DEFAULT_IMAGE = "redis:7.0-alpine";

    /**
     * Redis 默认端口
     */
    public static final int DEFAULT_PORT = 6379;

    /**
     * 创建默认 Redis 容器
     * 使用 Alpine 版本，体积小启动快
     *
     * @return GenericContainer 对象
     */
    public static GenericContainer<?> create() {
        return create(DEFAULT_IMAGE);
    }

    /**
     * 创建指定版本的 Redis 容器
     *
     * @param image Docker 镜像版本
     * @return GenericContainer 对象
     */
    public static GenericContainer<?> create(String image) {
        return new GenericContainer<>(DockerImageName.parse(image))
                .withExposedPorts(DEFAULT_PORT)
                .withReuse(true);  // 容器复用，提高测试速度
    }

    /**
     * 创建带密码的 Redis 容器
     *
     * @param password Redis 密码
     * @return GenericContainer 对象
     */
    public static GenericContainer<?> createWithPassword(String password) {
        return new GenericContainer<>(DockerImageName.parse(DEFAULT_IMAGE))
                .withExposedPorts(DEFAULT_PORT)
                .withCommand("redis-server", "--requirepass", password)
                .withReuse(true);
    }

    /**
     * 获取容器的映射端口
     * 容器启动后调用
     *
     * @param container Redis 容器
     * @return 映射到宿主机的端口
     */
    public static int getMappedPort(GenericContainer<?> container) {
        return container.getMappedPort(DEFAULT_PORT);
    }

    /**
     * 获取 Redis 连接 URL
     * 格式：redis://host:port
     *
     * @param container Redis 容器
     * @return Redis 连接 URL
     */
    public static String getRedisUrl(GenericContainer<?> container) {
        return "redis://" + container.getHost() + ":" + getMappedPort(container);
    }
}