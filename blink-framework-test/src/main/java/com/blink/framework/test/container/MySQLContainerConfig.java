package com.blink.framework.test.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * MySQL 容器配置工厂
 * 提供统一的 MySQL Testcontainers 配置
 *
 * @author binblink
 * @since 2026-04-16
 */
public class MySQLContainerConfig {

    /**
     * MySQL 默认镜像版本（与生产环境一致）
     */
    public static final String DEFAULT_IMAGE = "mysql:8.0.33";

    /**
     * 默认数据库名称
     */
    public static final String DEFAULT_DATABASE = "blink_test";

    /**
     * 默认用户名
     */
    public static final String DEFAULT_USERNAME = "blink_test";

    /**
     * 默认密码
     */
    public static final String DEFAULT_PASSWORD = "blink_test";

    /**
     * 创建默认 MySQL 容器
     * 使用默认配置，容器复用
     *
     * @return MySQLContainer 对象
     */
    public static MySQLContainer<?> create() {
        return create(DEFAULT_IMAGE);
    }

    /**
     * 创建指定版本的 MySQL 容器
     *
     * @param image Docker 镜像版本
     * @return MySQLContainer 对象
     */
    public static MySQLContainer<?> create(String image) {
        return new MySQLContainer<>(DockerImageName.parse(image))
                .withDatabaseName(DEFAULT_DATABASE)
                .withUsername(DEFAULT_USERNAME)
                .withPassword(DEFAULT_PASSWORD)
                .withReuse(true);  // 容器复用，提高测试速度
    }

    /**
     * 创建带初始化脚本的 MySQL 容器
     *
     * @param initScriptPath 初始化脚本路径（相对于 resources 目录）
     * @return MySQLContainer 对象
     */
    public static MySQLContainer<?> createWithInitScript(String initScriptPath) {
        return create()
                .withInitScript(initScriptPath);
    }

    /**
     * 创建自定义配置的 MySQL 容器
     *
     * @param database 数据库名
     * @param username 用户名
     * @param password 密码
     * @return MySQLContainer 对象
     */
    public static MySQLContainer<?> createCustom(String database, String username, String password) {
        return new MySQLContainer<>(DockerImageName.parse(DEFAULT_IMAGE))
                .withDatabaseName(database)
                .withUsername(username)
                .withPassword(password)
                .withReuse(true);
    }
}