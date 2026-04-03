package com.blink.base.config;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.blink.base.constants.CommonConstans;
import com.blink.base.dto.req.QueryOneSysConfigReq;
import com.blink.base.dto.vo.SysConfigVO;
import com.blink.base.entity.SysOperationLogDO;
import com.blink.base.service.SysConfigService;
import com.blink.base.service.SysOperationLogService;
import com.blink.framework.common.context.BlinkRequestContextHolder;

import com.blink.log.constant.LogType;
import com.blink.log.function.LogConverter;
import com.blink.log.function.LogEnabledFunction;
import com.blink.log.function.LogPersistFunction;
import com.blink.log.function.UserInfoProviderFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 日志功能配置类
 * <p>
 * 实现 blink-log-starter 的函数式接口，对接 blink-base-app 的业务实现。
 *
 * @author binblink
 */
@Configuration
public class LogFunctionConfiguration {

    /**
     * 日志持久化实现
     * <p>
     * 将日志持久化到 sys_operation_log 表
     *
     * @param sysOperationLogService 操作日志服务
     * @return 日志持久化函数
     */
    @Bean
    public LogPersistFunction<SysOperationLogDO> logPersistFunction(SysOperationLogService sysOperationLogService) {
        return sysOperationLogService::asyncSaveLog;
    }

    /**
     * 日志开关判断实现
     * <p>
     * 从系统配置中获取日志开关状态
     *
     * @param sysConfigService 系统配置服务
     * @return 日志开关判断函数
     */
    @Bean
    public LogEnabledFunction logEnabledFunction(SysConfigService sysConfigService) {
        return logType -> {
            try {
                String configKey = getConfigKeyByLogType(logType);
                var config = new QueryOneSysConfigReq();
                config.setConfigKey(configKey);
                SysConfigVO logConfig = Optional.ofNullable(sysConfigService.getOneConfigFromCacheOrDataBase(config))
                        .orElseGet(SysConfigVO::new);
                String configValue = logConfig.getConfigValue();
                // 默认启用
                return StrUtil.isBlank(configValue) || Boolean.parseBoolean(configValue);
            } catch (Exception e) {
                // 获取失败时默认启用
                return true;
            }
        };
    }

    /**
     * 用户信息提供实现
     * <p>
     * 从请求上下文中获取当前登录用户信息
     *
     * @return 用户信息提供函数
     */
    @Bean
    public UserInfoProviderFunction userInfoProviderFunction() {
        return () -> {
            try {
                String userIdStr = BlinkRequestContextHolder.getUserId();
                String loginName = BlinkRequestContextHolder.getLoginName();

                if (StrUtil.isBlank(userIdStr)) {
                    return null;
                }

                return new UserInfoProviderFunction.UserInfo(
                        Integer.valueOf(userIdStr),
                        loginName
                );
            } catch (Exception e) {
                return null;
            }
        };
    }

    /**
     * 日志转换器实现
     * <p>
     * 将通用的 OperationLogRecord 转换为 SysOperationLogDO
     *
     * @return 日志转换器
     */
    @Bean
    public LogConverter<SysOperationLogDO> logConverter() {
        return record -> {
            SysOperationLogDO entity = new SysOperationLogDO();
            BeanUtil.copyProperties(record, entity);
            // 设置创建时间
            entity.setCreateTime(LocalDateTime.now());
            return entity;
        };
    }

    /**
     * 根据日志类型获取配置key
     *
     * @param logType 日志类型编码
     * @return 配置key
     */
    private String getConfigKeyByLogType(String logType) {
        // 先检查总开关
        // 这里简化处理，由切面先检查总开关再检查分开关
        if (LogType.LOGIN.getCode().equals(logType)) {
            return CommonConstans.SysConfigKeys.LOG_ENABLE_LOGIN_LOG;
        } else {
            // OPERATION 和 SYSTEM 类型使用操作日志开关
            return CommonConstans.SysConfigKeys.LOG_ENABLE_OPERATION_LOG;
        }
    }
}