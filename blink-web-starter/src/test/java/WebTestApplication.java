import com.blink.framework.core.config.DynamicThreadPoolAutoConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试自动配置
 * @Author binblink
 */
@SpringBootTest(classes = {DynamicThreadPoolAutoConfig.class})
@TestPropertySource(properties = {
        "blink.web.async.thread-pool.io.enabled=false",
        "blink.web.async.thread-pool.core.core-size=2",
        "blink.web.async.thread-pool.core.max-size=4",
})
public class WebTestApplication {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void autoConfig(){
        // 验证核心线程池Bean是否存在
        assertThat(applicationContext.containsBean("ioIntensiveThreadPool"))
                .as("核心线程池Bean应该存在")
                .isTrue();

        // 验证Bean类型
        ThreadPoolTaskExecutor executor = applicationContext.getBean(
                "ioIntensiveThreadPool", ThreadPoolTaskExecutor.class);
        assertThat(executor)
                .as("获取到的Bean应该是ThreadPoolTaskExecutor类型")
                .isNotNull();
    }

}
