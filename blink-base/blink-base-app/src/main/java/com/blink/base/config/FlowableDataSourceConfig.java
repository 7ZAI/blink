package com.blink.base.config;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 数据源配置类
 * <p>
 * 配置业务数据源和Flowable流程引擎数据源，实现数据隔离。
 * 业务数据存储在 blink 数据库，流程引擎数据存储在 blink_flowable 数据库。
 * </p>
 * <p>
 * 关键配置：
 * 1. 通过 EngineConfigurationConfigurer&lt;SpringAppEngineConfiguration&gt; 配置 AppEngine 使用独立数据源
 * 2. AppEngine 是 Flowable 的顶层引擎，它初始化所有子引擎（ProcessEngine, DMN, CMMN 等）
 * 3. 所有子引擎都会继承 AppEngine 的数据源配置
 * </p>
 *
 * @author binblink
 */
@Slf4j
@Configuration
public class FlowableDataSourceConfig {

    /**
     * 创建业务主数据源
     * <p>
     * 使用 @Primary 标记为主数据源，MyBatis-Plus 等组件默认使用此数据源。
     * 数据源配置从 spring.datasource 前缀读取，使用 Druid 连接池。
     * </p>
     *
     * @return 业务数据源实例
     */
    @Primary
    @Bean(name = "businessDataSource", destroyMethod = "close")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource businessDataSource() {
        log.info("初始化业务主数据源 (Druid)");
        return new DruidDataSource();
    }

    /**
     * 创建Flowable专用数据源
     * <p>
     * 数据源配置从 spring.datasource.flowable 前缀读取，
     * 使用 Druid 连接池，用于流程引擎相关操作。
     * </p>
     *
     * @return Flowable数据源实例
     */
    @Bean(name = "flowableDataSource", destroyMethod = "close")
    @ConfigurationProperties(prefix = "spring.datasource.flowable")
    public DataSource flowableDataSource() {
        log.info("初始化Flowable流程引擎数据源 (Druid)");
        return new DruidDataSource();
    }

    /**
     * AppEngine配置器 - 配置Flowable AppEngine使用独立数据源
     * <p>
     * SpringAppEngineConfiguration 是 Flowable 的顶层引擎配置，
     * 它会初始化所有子引擎（ProcessEngine, DMN, CMMN 等）。
     * 必须在这里设置数据源，才能让所有子引擎使用正确的数据库。
     * </p>
     *
     * @param flowableDataSource Flowable专用数据源
     * @return AppEngine配置器
     */
    @Bean
    public EngineConfigurationConfigurer<SpringAppEngineConfiguration> appEngineConfigurer(
            @Qualifier("flowableDataSource") DataSource flowableDataSource) {
        return appEngineConfiguration -> {
            // 设置Flowable专用数据源（所有子引擎都会继承此数据源）
            appEngineConfiguration.setDataSource(flowableDataSource);
            // 设置数据库表结构自动更新策略
            appEngineConfiguration.setDatabaseSchemaUpdate("true");
            // 禁用IDM引擎（用户身份管理使用系统自带，不使用Flowable的用户体系）
            appEngineConfiguration.setDisableIdmEngine(true);
            log.info("Flowable AppEngine配置完成，使用独立数据源 blink_flowable");
        };
    }

    /**
     * ProcessEngine配置器 - 配置ProcessEngine历史级别等
     * <p>
     * ProcessEngine 是 AppEngine 的子引擎，会继承 AppEngine 的数据源。
     * 这里配置历史记录级别、字体等参数。
     * </p>
     *
     * @param flowableDataSource Flowable专用数据源
     * @return ProcessEngine配置器
     */
    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> processEngineConfigurer(
            @Qualifier("flowableDataSource") DataSource flowableDataSource) {
        return processEngineConfiguration -> {
            // 确保使用正确的数据源
            processEngineConfiguration.setDataSource(flowableDataSource);
            // 设置数据库表结构自动更新策略
            processEngineConfiguration.setDatabaseSchemaUpdate("true");
            // 设置历史记录级别为FULL，记录所有流程相关信息
            processEngineConfiguration.setHistoryLevel(HistoryLevel.FULL);
            // 禁用IDM引擎
            processEngineConfiguration.setDisableIdmEngine(true);
            // 禁用异步执行器
            processEngineConfiguration.setAsyncExecutorActivate(false);
            // 配置流程图生成时的字体，解决中文乱码问题
            processEngineConfiguration.setActivityFontName("宋体");
            processEngineConfiguration.setLabelFontName("宋体");
            processEngineConfiguration.setAnnotationFontName("宋体");
            log.info("Flowable ProcessEngine配置完成，使用独立数据源 blink_flowable");
        };
    }
}
