package com.blink.framework.test.annotation;

import org.junit.jupiter.api.Tag;
import java.lang.annotation.*;

/**
 * 集成测试标签
 * 需要 Testcontainers 或 Spring Boot Test 环境
 * 标识需要完整 Spring 容器或外部依赖的测试
 *
 * @author binblink
 * @since 2026-04-16
 */
@Tag("integration")
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IntegrationTest {
    /**
     * 测试描述
     */
    String description() default "";
}