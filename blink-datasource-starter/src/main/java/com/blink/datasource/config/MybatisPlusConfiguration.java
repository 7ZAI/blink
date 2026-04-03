package com.blink.datasource.config;


import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.blink.datasource.component.DataScopeEntityScanner;
import com.blink.datasource.handler.MyMetaObjectHandler;
import com.blink.datasource.interceptor.DataScopeInterceptor;
import com.github.pagehelper.PageInterceptor;
import org.apache.ibatis.session.LocalCacheScope;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.Properties;

import org.apache.ibatis.plugin.Interceptor;


/**
 * @author binblink
 */
@AutoConfiguration
@MapperScan("com.blink.**.mapper")
public class MybatisPlusConfiguration {
    /**
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisConfiguration mybatisConfiguration() {
        var mybatisConfiguration = new MybatisConfiguration();

        //添加插件 目前采用mybatis plus MetaHandler的方式处理
//        mybatisConfiguration.addInterceptor(new NormalFieldInterceptor());
        //不映射空值
        mybatisConfiguration.setCallSettersOnNulls(true);
        //打开驼峰格式
        mybatisConfiguration.setMapUnderscoreToCamelCase(true);
        //打印日志
//        mybatisConfiguration.setLogImpl(StdOutImpl.class);

        //一级缓存是默认开启的 默认范围为同一个sqlsession中 在微服务中关闭一级缓存 因为别的服务可能会改变数据库数据 导致相同的session中查找出过期数据
        mybatisConfiguration.setLocalCacheScope(LocalCacheScope.STATEMENT);

        //二级缓存 以同mapper范围 是可以跨sqlsession的
        //缓存是以namespace为单位的，不同namespace下的操作互不影响。
        //insert,update,delete操作会清空所在namespace下的全部缓存。对于频繁修改数据的来说 缓存形同虚设
        //通常使用MyBatis Generator生成的代码中，都是各个表独立的，每个表都有自己的namespace。
        //多表操作一定不要使用二级缓存，因为多表操作进行更新操作，一定会产生脏数据。

        //综上 单体环境可以开启一级缓存，微服务或者分布式架构下 关闭mybatis缓存
        //真要建议上redis 针对热点查询 进行缓存 更加可控 更贴合实际业务
        mybatisConfiguration.setCacheEnabled(false);

        mybatisConfiguration.addInterceptor(pageInterceptor());

        return mybatisConfiguration;
    }

    @Bean
    public MyMetaObjectHandler metaObjectHandler() {
        return new MyMetaObjectHandler();
    }

    @Bean
    public DataScopeEntityScanner dataScopeEntityScanner(){
        return new DataScopeEntityScanner();
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalConfig globalConfig() {
        var globalConfig = new GlobalConfig();
        globalConfig.setDbConfig(dbConfig());
        return globalConfig;
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalConfig.DbConfig dbConfig() {
        var dbConfig = new GlobalConfig.DbConfig();
        //逻辑删除配置
        dbConfig.setLogicDeleteField("delFlag")
                .setLogicDeleteValue("1")
                .setLogicNotDeleteValue("0");

        return dbConfig;
    }

    /**
     * 配置自定义插件
     * @return
     */
    // @Bean
    // public Interceptor normalFieldInterceptor(){
    //     return new NormalFieldInterceptor();
    // }

    /**
     * 配置pageHelper分页插件
     */
    @Bean
    @ConditionalOnMissingBean
    public PageInterceptor pageInterceptor() {
        var pageHelper = new PageInterceptor();
        var prop = new Properties();
        prop.setProperty("defaultCount", "true");
        prop.setProperty("reasonable", "false");
//        prop.setProperty("dialect","mysql");
        pageHelper.setProperties(prop);
        return pageHelper;
    }


}
