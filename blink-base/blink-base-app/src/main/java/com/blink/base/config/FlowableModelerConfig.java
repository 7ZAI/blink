package com.blink.base.config;

// import org.flowable.ui.modeler.conf.ApplicationConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// import org.springframework.context.annotation.Import;

/**
 * Flowable Modeler配置类
 * <p>
 * 启用Flowable可视化流程设计器
 * 访问地址：http://localhost:8001/base/flowable-modeler
 * 需要添加依赖: implementation 'org.flowable:flowable-ui-modeler-conf:7.0.1'
 * </p>
 *
 * @author binblink
 */
// @Configuration
// @ConditionalOnProperty(name = "flowable.modeler.enabled", havingValue = "true")
// @Import(value = {
//         ApplicationConfiguration.class
// })
public class FlowableModelerConfig {

}
