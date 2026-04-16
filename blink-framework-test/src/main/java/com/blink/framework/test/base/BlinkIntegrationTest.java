package com.blink.framework.test.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 集成测试基类
 * 支持 Testcontainers 和 Spring Boot Test
 *
 * 使用方式：
 * <pre>
 * @IntegrationTest
 * class MyControllerIntegrationTest extends BlinkIntegrationTest {
 *
 *     @Test
 *     void shouldCreateUser_successfully() {
 *         // 使用 restTemplate 进行 HTTP 测试
 *         ResponseEntity<ResponseDTO> response = restTemplate.postForEntity(...);
 *     }
 * }
 * </pre>
 *
 * 注意：需要本地安装 Docker 并启动
 *
 * @author binblink
 * @since 2026-04-16
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public abstract class BlinkIntegrationTest {

    /**
     * MySQL 容器（共享，所有集成测试共用）
     * 使用 MySQL 8.0.33 版本，与生产环境一致
     */
    @Container
    protected static final MySQLContainer<?> MYSQL_CONTAINER =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0.33"))
                    .withDatabaseName("blink_test")
                    .withUsername("blink_test")
                    .withPassword("blink_test")
                    .withReuse(true);  // 容器复用，提高测试速度

    /**
     * Redis 容器（共享）
     * 使用 Redis 7.0 Alpine 版本
     */
    @Container
    protected static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>(DockerImageName.parse("redis:7.0-alpine"))
                    .withExposedPorts(6379)
                    .withReuse(true);

    /**
     * 动态配置数据源和 Redis
     * 容器启动后自动注入连接信息
     */
    @DynamicPropertySource
    static void configureContainers(DynamicPropertyRegistry registry) {
        // MySQL 配置
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // Redis 配置
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
    }

    /**
     * 测试用 RestTemplate
     * 用于进行 HTTP API 测试
     */
    @Autowired
    protected TestRestTemplate restTemplate;

    /**
     * JSON 序列化工具
     * 用于测试中的 JSON 处理
     */
    @Autowired
    protected ObjectMapper objectMapper;
}