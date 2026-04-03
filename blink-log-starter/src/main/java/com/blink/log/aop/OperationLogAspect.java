package com.blink.log.aop;

import cn.hutool.core.util.StrUtil;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.log.annotation.RecordLog;
import com.blink.log.config.LogProperties;
import com.blink.log.constant.LogType;
import com.blink.log.function.LogConverter;
import com.blink.log.function.LogEnabledFunction;
import com.blink.log.function.LogPersistFunction;
import com.blink.log.function.UserInfoProviderFunction;
import com.blink.log.model.OperationLogRecord;
import com.blink.log.util.ClientIpUtils;
import com.blink.log.util.LogSensitiveUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 操作日志切面 入库日志 日志持久化
 * <p>
 * 拦截带有 @OperationLog 注解的方法，自动记录操作日志。
 * 通过函数式接口与具体业务解耦，支持灵活扩展。
 *
 * @author binblink
 */
@Aspect
@Slf4j
@Order(100)
public class OperationLogAspect<T> {

    /**
     * 日志配置属性
     */
    private final LogProperties logProperties;

    /**
     * 日志持久化函数
     */
    private final LogPersistFunction<T> logPersistFunction;

    /**
     * 日志开关判断函数
     */
    private final LogEnabledFunction logEnabledFunction;

    /**
     * 用户信息提供函数
     */
    private final UserInfoProviderFunction userInfoProviderFunction;

    /**
     * 日志转换函数
     */
    private final LogConverter<T> logConverter;

    /**
     * 构造函数
     *
     * @param logProperties            日志配置属性
     * @param logPersistFunction       日志持久化函数
     * @param logEnabledFunction       日志开关判断函数
     * @param userInfoProviderFunction 用户信息提供函数
     * @param logConverter             日志转换函数
     */
    public OperationLogAspect(
            LogProperties logProperties,
            LogPersistFunction<T> logPersistFunction,
            LogEnabledFunction logEnabledFunction,
            UserInfoProviderFunction userInfoProviderFunction,
            LogConverter<T> logConverter) {
        this.logProperties = logProperties;
        this.logPersistFunction = logPersistFunction;
        this.logEnabledFunction = logEnabledFunction;
        this.userInfoProviderFunction = userInfoProviderFunction;
        this.logConverter = logConverter;
    }

    /**
     * 定义切点：拦截所有带有 @OperationLog 注解的方法
     */
    @Pointcut("@annotation(com.blink.log.annotation.RecordLog)")
    public void operationLogPointcut() {
    }

    /**
     * 环绕通知：记录操作日志
     *
     * @param joinPoint 连接点
     * @return 方法返回值
     * @throws Throwable 异常
     */
    @Around("operationLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取 @OperationLog 注解
        RecordLog operationLog = method.getAnnotation(RecordLog.class);
        if (operationLog == null) {
            return joinPoint.proceed();
        }

        // 检查日志开关
        if (!isLogEnabled(operationLog.type())) {
            log.debug("日志开关已关闭，跳过日志记录");
            return joinPoint.proceed();
        }

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 创建日志记录
        OperationLogRecord logRecord = new OperationLogRecord();

        // 填充基础信息
        fillBasicInfo(logRecord, operationLog);

        // 填充请求信息
        fillRequestInfo(logRecord, joinPoint, operationLog);

        // 执行目标方法
        Object result = null;
        Exception exception = null;
        try {
            result = joinPoint.proceed();
            // 记录成功状态
            logRecord.setExecuteStatus(0);
            // 填充响应信息
            fillResponseInfo(logRecord, result, operationLog);
        } catch (Exception e) {
            exception = e;
            // 记录失败状态
            logRecord.setExecuteStatus(1);
            // 记录错误信息
            int maxErrorLength = logProperties.getRecord().getMaxErrorMsgLength();
            logRecord.setErrorMsg(StrUtil.sub(e.getMessage(), 0, maxErrorLength));
        }

        // 计算执行时长
        long executeTime = System.currentTimeMillis() - startTime;
        logRecord.setExecuteTimeMs((int) executeTime);

        // 异步保存日志
        asyncSaveLog(logRecord);

        // 如果有异常，继续抛出
        if (exception != null) {
            throw exception;
        }

        return result;
    }

    /**
     * 检查日志开关是否启用
     *
     * @param logType 日志类型
     * @return true-启用 false-禁用
     */
    private boolean isLogEnabled(LogType logType) {
        if (logEnabledFunction == null) {
            return true;
        }
        try {
            return logEnabledFunction.isEnabled(logType.getCode());
        } catch (Exception e) {
            log.warn("检查日志开关失败，默认启用: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 填充基础信息
     *
     * @param logRecord    日志记录
     * @param operationLog 操作日志注解
     */
    private void fillBasicInfo(OperationLogRecord logRecord, RecordLog operationLog) {
        // 设置日志类型
        logRecord.setLogType(operationLog.type().getCode());

        // 设置操作描述
        logRecord.setDescription(operationLog.description());

        // 设置操作时间
        logRecord.setOperationTime(LocalDateTime.now());

        // 填充用户信息
        fillUserInfo(logRecord);
    }

    /**
     * 填充用户信息
     *
     * @param logRecord 日志记录
     */
    private void fillUserInfo(OperationLogRecord logRecord) {
        if (userInfoProviderFunction == null) {
            return;
        }

        try {
            UserInfoProviderFunction.UserInfo userInfo = userInfoProviderFunction.getCurrentUser();
            if (userInfo != null) {
                logRecord.setUserId(userInfo.userId());
                logRecord.setLoginName(userInfo.loginName());
            }
        } catch (Exception e) {
            log.debug("获取当前用户信息失败: {}", e.getMessage());
        }
    }

    /**
     * 填充请求信息
     *
     * @param logRecord    日志记录
     * @param joinPoint    连接点
     * @param operationLog 操作日志注解
     */
    private void fillRequestInfo(OperationLogRecord logRecord, ProceedingJoinPoint joinPoint, RecordLog operationLog) {
        // 获取 HTTP 请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // 设置请求URL
            logRecord.setRequestUrl(request.getRequestURI());

            // 设置请求方法
            logRecord.setRequestMethod(request.getMethod());

            // 设置IP地址
            logRecord.setIpAddress(ClientIpUtils.getClientIp(request));

            // 设置User-Agent
            String userAgent = request.getHeader("User-Agent");
            if (StrUtil.isNotBlank(userAgent)) {
                int maxUaLength = logProperties.getRecord().getMaxUserAgentLength();
                logRecord.setUserAgent(StrUtil.sub(userAgent, 0, maxUaLength));
            }
        }

        // 设置请求参数
        boolean shouldSaveRequest = operationLog.saveRequest() && logProperties.getRecord().isSaveRequest();
        if (shouldSaveRequest) {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                // 查找 RequestDTO 类型的参数
                for (Object arg : args) {
                    if (arg instanceof RequestDTO<?> requestDTO) {
                        // 使用 LogSensitiveUtils 进行脱敏处理（带安全截断）
                        int maxLength = logProperties.getRecord().getMaxRequestLength();
                        String params = LogSensitiveUtils.toSensitiveString(requestDTO.getBody(), maxLength);
                        logRecord.setRequestParams(params);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 填充响应信息
     *
     * @param logRecord    日志记录
     * @param result       方法返回值
     * @param operationLog 操作日志注解
     */
    private void fillResponseInfo(OperationLogRecord logRecord, Object result, RecordLog operationLog) {
        boolean shouldSaveResponse = operationLog.saveResponse() && logProperties.getRecord().isSaveResponse();
        if (!shouldSaveResponse || result == null) {
            return;
        }

        try {
            int maxLength = logProperties.getRecord().getMaxResponseLength();

            // 如果是 ResponseDTO 类型，提取 body
            if (result instanceof ResponseDTO<?> responseDTO) {
                Object body = responseDTO.getBody();
                if (body != null) {
                    // 使用 LogSensitiveUtils 进行脱敏处理（带安全截断）
                    String responseData = LogSensitiveUtils.toSensitiveString(body, maxLength);
                    logRecord.setResponseData(responseData);
                }
            } else {
                // 使用 LogSensitiveUtils 进行脱敏处理（带安全截断）
                String responseData = LogSensitiveUtils.toSensitiveString(result, maxLength);
                logRecord.setResponseData(responseData);
            }
        } catch (Exception e) {
            log.debug("记录响应数据失败: {}", e.getMessage());
        }
    }

    /**
     * 异步保存日志
     *
     * @param logRecord 日志记录
     */
    @SuppressWarnings("unchecked")
    private void asyncSaveLog(OperationLogRecord logRecord) {
        if (logPersistFunction == null || logConverter == null) {
            log.debug("日志持久化函数未配置，跳过日志保存");
            return;
        }

        try {
            T entity = logConverter.convert(logRecord);
            if (Objects.nonNull(entity)) {
                logPersistFunction.persist(entity);
            }
        } catch (Exception e) {
            log.error("异步保存操作日志失败: {}", e.getMessage(), e);
        }
    }
}