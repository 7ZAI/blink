package com.blink.datasource;

import com.blink.datasource.config.MybatisPlusConfiguration;
import com.blink.datasource.data.RuleMerge;
import com.blink.datasource.data.UserDataScopeInfo;
import com.blink.datasource.interceptor.DataScopeInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.function.Supplier;

/**
 * 集成测试配置类
 *
 * @author binblink
 * @since 2026-04-12
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@Import({
        MybatisPlusConfiguration.class
})
public class IntegrationTestConfig {

    /**
     * H2 内存数据源
     */
    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("testdb")
                .addScript("classpath:schema-test.sql")
                .build();
    }

    /**
     * SqlSessionFactory
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTypeAliasesPackage("com.blink.datasource");

        // 添加 MyBatis 配置
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setCallSettersOnNulls(true);
        factory.setConfiguration(configuration);

        // 加载 mapper xml 文件（如果有的话）
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            factory.setMapperLocations(resolver.getResources("classpath*:mapper/**/*.xml"));
        } catch (Exception e) {
            // 没有mapper xml文件时忽略
        }

        return factory.getObject();
    }

    /**
     * 用户信息供应器 - 测试用
     * 默认返回测试用户信息
     */
    @Bean
    public Supplier<UserDataScopeInfo> userInfoSupplier() {
        return () -> {
            UserDataScopeInfo info = new UserDataScopeInfo();
            info.setUserId(1);
            info.setLoginName("testUser");
            info.setSuperFlag(0);
            info.setDeptId(1);
            return info;
        };
    }

    /**
     * 规则合并器
     */
    @Bean
    public RuleMerge ruleMerge() {
        return rules -> rules.get(0); // 简单实现：返回第一个规则
    }

    /**
     * DataScopeInterceptor
     */
    @Bean
    public DataScopeInterceptor dataScopeInterceptor(
            Supplier<UserDataScopeInfo> userInfoSupplier,
            RuleMerge ruleMerge) {
        return new DataScopeInterceptor(Collections.emptyList(), userInfoSupplier, ruleMerge);
    }
}
