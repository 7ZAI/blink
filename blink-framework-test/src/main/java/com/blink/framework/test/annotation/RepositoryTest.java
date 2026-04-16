package com.blink.framework.test.annotation;

import org.junit.jupiter.api.Tag;
import java.lang.annotation.*;

/**
 * 数据层测试标签
 * 使用 H2 内存数据库进行 Mapper/Repository 层测试
 *
 * @author binblink
 * @since 2026-04-16
 */
@Tag("repository")
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RepositoryTest {
    /**
     * 测试描述
     */
    String description() default "";
}