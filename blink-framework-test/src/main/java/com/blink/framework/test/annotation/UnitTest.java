package com.blink.framework.test.annotation;

import org.junit.jupiter.api.Tag;
import java.lang.annotation.*;

/**
 * 单元测试标签
 * 用于快速测试筛选，标识纯单元测试（不依赖 Spring 容器）
 *
 * @author binblink
 * @since 2026-04-16
 */
@Tag("unit")
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UnitTest {
    /**
     * 测试描述
     */
    String description() default "";
}