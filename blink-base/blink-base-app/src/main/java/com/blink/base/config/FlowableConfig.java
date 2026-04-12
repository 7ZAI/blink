package com.blink.base.config;

import com.blink.framework.common.exception.BlinkException;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Flowable工作流引擎配置类
 * <p>
 * 配置说明：
 * 1. 自动建表：首次启动时自动创建Flowable所需的数据库表
 * 2. 历史记录：开启完整历史记录级别，记录所有流程实例、任务、变量等信息
 * 3. 字体配置：设置流程图生成的中文字体，避免中文乱码问题
 * 4. 事件监听器：配置全局事件监听器，用于流程生命周期管理
 * </p>
 *
 * @author binblink
 */
@Slf4j
@Configuration
public class FlowableConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {

    /**
     * 数据源
     */
    private final DataSource dataSource;

    /**
     * 构造函数注入数据源
     *
     * @param dataSource 数据源实例
     */
    public FlowableConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 配置Flowable流程引擎
     * <p>
     * 主要配置项：
     * - 数据源：使用项目统一配置的数据源
     * - 自动部署：关闭自动部署，避免重复部署流程定义
     * - 历史级别：设置为FULL，记录完整的流程历史信息
     * - 字体设置：配置中文字体，解决流程图中文乱码问题
     * </p>
     *
     * @param processEngineConfiguration 流程引擎配置对象
     */
    @Override
    public void configure(SpringProcessEngineConfiguration processEngineConfiguration) {
        try {
            // 设置数据源
            processEngineConfiguration.setDataSource(dataSource);
            
            // 关闭自动部署，避免每次启动都部署流程定义
            processEngineConfiguration.setDisableIdmEngine(false);
            
            // 设置历史记录级别为FULL，记录所有流程相关信息
            // 可选值：NONE, ACTIVITY, AUDIT, FULL
            processEngineConfiguration.setHistoryLevel(HistoryLevel.FULL);
            //禁用异步任务
            processEngineConfiguration.setAsyncExecutorActivate(false);


            // 设置数据库表结构自动更新策略
            // true: 自动创建和更新表结构（开发环境推荐）
            // false: 不自动创建表结构（生产环境推荐）
            processEngineConfiguration.setDatabaseSchemaUpdate("true");
            
            // 配置流程图生成时的字体，解决中文乱码问题
            processEngineConfiguration.setActivityFontName("宋体");
            processEngineConfiguration.setLabelFontName("宋体");
            processEngineConfiguration.setAnnotationFontName("宋体");
            
            // 设置异步执行器配置
            processEngineConfiguration.setAsyncExecutorActivate(false);
            processEngineConfiguration.setAsyncExecutorCorePoolSize(2);
            processEngineConfiguration.setAsyncExecutorMaxPoolSize(10);
            processEngineConfiguration.setAsyncExecutorThreadKeepAliveTime(300);
            
            log.info("Flowable流程引擎配置完成");
            
        } catch (Exception e) {
            // 将运行时异常转换为BlinkException
            throw new BlinkException("Flowable流程引擎配置失败: " + e.getMessage(), e, "FLOWABLE_CONFIG_ERROR");
        }
    }
}
