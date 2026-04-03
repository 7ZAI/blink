package com.blink.base.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.base.mapper.SysUserRoleRelaMapper;
import com.blink.datasource.data.UserDataScopeInfo;
import com.blink.datasource.handler.RuleHandler;
import com.blink.datasource.interceptor.DataScopeInterceptor;
import com.blink.base.datascope.handler.*;
import com.blink.base.datascope.merge.RuleMergeStrategy;
import com.blink.base.service.UserDataScopeCacheService;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.apache.ibatis.plugin.Interceptor;

/**
 * 数据范围权限自动配置类
 * 注册MyBatis拦截器，并确保执行顺序：PageInterceptor（外层） -> DataScopeInterceptor（内层）
 *
 * 执行链：请求 → PageInterceptor → DataScopeInterceptor → StatementHandler
 *
 * @author binblink
 */
@Configuration
public class DataScopeAutoConfiguration {

    private UserDataScopeCacheService userDataScopeCacheService;

    private SysUserRoleRelaMapper sysUserRoleRelaMapper;

    @Autowired
    public void setUserDataScopeCacheService(@Lazy UserDataScopeCacheService userDataScopeCacheService) {
        this.userDataScopeCacheService = userDataScopeCacheService;
    }

    @Autowired
    public void setSysUserRoleRelaMapper(@Lazy SysUserRoleRelaMapper sysUserRoleRelaMapper) {
        this.sysUserRoleRelaMapper = sysUserRoleRelaMapper;
    }

    @Bean
    public DataScopeInterceptor dataScopeInterceptor() {
        List<RuleHandler> ruleHandlers = new ArrayList<>();

        ruleHandlers.add(new CreatorFilterHandler(sysUserRoleRelaMapper));
        ruleHandlers.add(new CustomSqlHandler());
        ruleHandlers.add(new DateRangeFilterHandler());
        ruleHandlers.add(new FieldFilterHandler());
        ruleHandlers.add(new RelationFilterHandler());

        RuleMergeStrategy ruleMergeStrategy = new RuleMergeStrategy();

        return new DataScopeInterceptor(ruleHandlers, getUserDataScopeInfo(), ruleMergeStrategy);
    }



    /**
     * 获取用户数据范围权限信息
     * 优先从 Redis 缓存获取，缓存不存在时实时构建
     *
     * @return UserDataScopeInfo 供应器
     */
    public Supplier<UserDataScopeInfo> getUserDataScopeInfo() {
        return () -> {
            String userIdStr = com.blink.framework.common.context.BlinkRequestContextHolder.getUserId();
            if (StrUtil.isBlank(userIdStr)) {
                return new UserDataScopeInfo();
            }

            Integer userId = Integer.valueOf(userIdStr);

            // 优先从缓存获取
            UserDataScopeInfo cached = userDataScopeCacheService.getFromCache(userId);
            if (cached != null) {
                return cached;
            }

            // 缓存不存在时实时构建（并缓存）
            return userDataScopeCacheService.buildAndCache(userId);
        };
    }


    /**
     * 注册拦截器到SqlSessionFactory
     * 执行顺序原理：MyBatis 拦截器链是逆序执行，后注册的先执行
     * 目标：PageInterceptor 先执行（外层），DataScopeInterceptor 后执行（内层）
     * 做法：DataScopeInterceptor 先注册，PageInterceptor 后注册
     */
    @Bean
    public CommandLineRunner dataScopeInterceptorRegistrar(
            List<SqlSessionFactory> sqlSessionFactoryList,
            DataScopeInterceptor dataScopeInterceptor) {
        return args -> {
            if (CollUtil.isEmpty(sqlSessionFactoryList)) {
                return;
            }
            for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
                var configuration = sqlSessionFactory.getConfiguration();

                // 检查是否已注册
                boolean alreadyRegistered = configuration.getInterceptors().stream()
                        .anyMatch(i -> i instanceof DataScopeInterceptor);
                if (alreadyRegistered) {
                    continue;
                }

                // 获取当前所有拦截器（此时应该包含 PageInterceptor）
                List<Interceptor> existingInterceptors = new ArrayList<>(configuration.getInterceptors());

                // 策略：先清空，然后按【目标内层顺序】添加，最后 MyBatis 会逆序执行
                // 目标执行顺序：PageInterceptor（外层） -> DataScopeInterceptor（内层）
                // 注册顺序：DataScopeInterceptor（先） -> PageInterceptor（后）
                configuration.getInterceptors().clear();

                // 1. 后添加其他拦截器（让它们在外层，先执行）
                for (Interceptor interceptor : existingInterceptors) {
                    configuration.addInterceptor(interceptor);
                }

                // 2. 先添加 DataScopeInterceptor（让它在内层，后执行）
                configuration.addInterceptor(dataScopeInterceptor);

            }
        };
    }
}
