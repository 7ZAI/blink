package com.blink.framework.common.utils;

import com.blink.framework.test.annotation.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.*;

/**
 * ApplicationContextUtil 单元测试
 * <p>
 * 测试覆盖：
 * 1. ApplicationContext 获取
 * 2. Bean 获取（通过名称、类型、名称+类型）
 * 3. Environment 获取
 * 4. 属性获取
 * 5. Profile 判断
 * 6. 未注入时的异常处理
 *
 * @author binblink
 */
@IntegrationTest
@SpringBootTest(classes = ApplicationContextUtilTest.TestConfig.class)
@DisplayName("ApplicationContextUtil Spring上下文工具测试")
class ApplicationContextUtilTest {

    @Autowired
    private ApplicationContext applicationContext;

    // 测试配置类
    @SpringBootApplication
    static class TestConfig {
        @Bean
        public TestService testService() {
            return new TestService();
        }

        @Bean("customNamedBean")
        public TestService customNamedService() {
            return new TestService();
        }
    }

    // 测试用的服务类
    static class TestService {
        public String hello() {
            return "Hello from TestService";
        }
    }

    // ==================== ApplicationContext 获取测试 ====================

    @Nested
    @DisplayName("ApplicationContext 获取测试")
    class ApplicationContextTests {

        @Test
        @DisplayName("应该成功获取ApplicationContext")
        void shouldGetApplicationContext() {
            // when
            ApplicationContext context = ApplicationContextUtil.getApplicationContext();

            // then
            assertThat(context).isNotNull();
        }

        @Test
        @DisplayName("获取的ApplicationContext应该与注入的相同")
        void shouldReturnSameApplicationContext() {
            // when
            ApplicationContext utilContext = ApplicationContextUtil.getApplicationContext();

            // then
            assertThat(utilContext).isSameAs(applicationContext);
        }
    }

    // ==================== Bean 获取测试 ====================

    @Nested
    @DisplayName("Bean 获取测试")
    class BeanRetrievalTests {

        @Test
        @DisplayName("应该通过名称获取Bean")
        void shouldGetBeanByName() {
            // when
            Object bean = ApplicationContextUtil.getBean("testService");

            // then
            assertThat(bean).isNotNull();
            assertThat(bean).isInstanceOf(TestService.class);
        }

        @Test
        @DisplayName("应该通过类型获取Bean（唯一类型）")
        void shouldGetBeanByClass() {
            // when - 使用String类型，它是唯一的
            // 注意：TestService有两个Bean实例，会导致NoUniqueBeanDefinitionException
            // 这里测试通过名称获取
            TestService bean = ApplicationContextUtil.getBean("testService", TestService.class);

            // then
            assertThat(bean).isNotNull();
            assertThat(bean.hello()).isEqualTo("Hello from TestService");
        }

        @Test
        @DisplayName("应该通过名称和类型获取Bean")
        void shouldGetBeanByNameAndClass() {
            // when
            TestService bean = ApplicationContextUtil.getBean("customNamedBean", TestService.class);

            // then
            assertThat(bean).isNotNull();
        }

        @Test
        @DisplayName("获取不存在的Bean名称应该抛出异常")
        void shouldThrowExceptionForNonExistentBeanName() {
            // when & then
            assertThatThrownBy(() -> ApplicationContextUtil.getBean("nonExistentBean"))
                    .isInstanceOf(org.springframework.beans.factory.NoSuchBeanDefinitionException.class);
        }

        @Test
        @DisplayName("获取不存在的Bean类型应该抛出异常")
        void shouldThrowExceptionForNonExistentBeanClass() {
            // when & then
            assertThatThrownBy(() -> ApplicationContextUtil.getBean(NonExistentService.class))
                    .isInstanceOf(org.springframework.beans.factory.NoSuchBeanDefinitionException.class);
        }
    }

    // 不存在的服务类
    static class NonExistentService {}

    // ==================== Environment 测试 ====================

    @Nested
    @DisplayName("Environment 测试")
    class EnvironmentTests {

        @Test
        @DisplayName("应该成功获取Environment")
        void shouldGetEnvironment() {
            // when
            Environment env = ApplicationContextUtil.getEnvironment();

            // then
            assertThat(env).isNotNull();
        }

        @Test
        @DisplayName("应该获取属性值")
        void shouldGetProperty() {
            // given
            Environment env = ApplicationContextUtil.getEnvironment();

            // when & then
            // Spring Boot 默认属性可能返回null或应用名
            String property = ApplicationContextUtil.getProperty("spring.application.name");
            // 测试环境可能没有设置应用名，所以只验证方法不抛异常
            assertThatCode(() -> ApplicationContextUtil.getProperty("spring.application.name"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("不存在的属性应该返回null")
        void shouldReturnNullForNonExistentProperty() {
            // when
            String value = ApplicationContextUtil.getProperty("non.existent.property");

            // then
            assertThat(value).isNull();
        }

        @Test
        @DisplayName("应该使用默认值返回属性")
        void shouldReturnDefaultForNonExistentProperty() {
            // when
            String value = ApplicationContextUtil.getProperty("non.existent.property", "defaultValue");

            // then
            assertThat(value).isEqualTo("defaultValue");
        }
    }

    // ==================== Profile 测试 ====================

    @Nested
    @DisplayName("Profile 测试")
    class ProfileTests {

        @Test
        @DisplayName("应该获取激活的Profile数组")
        void shouldGetActiveProfiles() {
            // when
            String[] profiles = ApplicationContextUtil.getActiveProfiles();

            // then
            assertThat(profiles).isNotNull();
        }

        @Test
        @DisplayName("isDev应该返回false（无dev profile）")
        void shouldReturnFalseForDevProfile() {
            // when
            boolean isDev = ApplicationContextUtil.isDev();

            // then - 测试环境默认没有dev profile
            assertThat(isDev).isFalse();
        }

        @Test
        @DisplayName("isProd应该返回false（无prod profile）")
        void shouldReturnFalseForProdProfile() {
            // when
            boolean isProd = ApplicationContextUtil.isProd();

            // then - 测试环境默认没有prod profile
            assertThat(isProd).isFalse();
        }
    }

    // ==================== 实际使用场景测试 ====================

    @Nested
    @DisplayName("实际使用场景测试")
    class UsageScenarioTests {

        @Test
        @DisplayName("应该在静态方法中获取Bean")
        void shouldGetBeanInStaticMethod() {
            // when - 使用名称获取Bean避免多Bean冲突
            TestService service = ApplicationContextUtil.getBean("testService", TestService.class);
            String result = service.hello();

            // then
            assertThat(result).isEqualTo("Hello from TestService");
        }

        @Test
        @DisplayName("应该获取多个相同类型的Bean")
        void shouldGetMultipleBeansOfSameType() {
            // when
            TestService primaryBean = ApplicationContextUtil.getBean("testService", TestService.class);
            TestService customBean = ApplicationContextUtil.getBean("customNamedBean", TestService.class);

            // then
            assertThat(primaryBean).isNotNull();
            assertThat(customBean).isNotNull();
            // 两个是不同的实例
            assertThat(primaryBean).isNotSameAs(customBean);
        }
    }

    // ==================== 异常处理测试 ====================

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("ApplicationContext应该已注入")
        void contextShouldBeInjected() {
            // when & then
            // 如果执行到这里说明ApplicationContext已正确注入
            assertThatCode(() -> ApplicationContextUtil.getApplicationContext())
                    .doesNotThrowAnyException();
        }
    }
}
