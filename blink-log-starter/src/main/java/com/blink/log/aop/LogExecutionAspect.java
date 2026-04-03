package com.blink.log.aop;

import com.blink.log.annotation.ConsoleLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * 控制台日志注解 切面
 *
 * @author binblink
 */
@Slf4j
@Aspect
public class LogExecutionAspect {

    @Around("@annotation(consoleLog)")
    public Object logExecution(ProceedingJoinPoint joinPoint, ConsoleLog consoleLog) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = method.getName();

        long startTime = System.currentTimeMillis();

        // 记录请求参数
        if (consoleLog.logRequest()) {

            Object[] args = joinPoint.getArgs();
            String[] paramNames = signature.getParameterNames();
            var params = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                params.append(paramNames[i]).append("=").append(args[i]);
                if (i < args.length - 1) {
                    params.append(System.lineSeparator());
                }
            }
            //文本块
            String logMessage = """
                    
                    ===> 进入方法
                    类名: %s
                    方法名: %s
                    入参: %s
                    """.formatted(className, methodName, params);

            logByLevel(consoleLog.level(), logMessage);
        }

        Object result;
        try {
            result = joinPoint.proceed();

            long costTime = System.currentTimeMillis() - startTime;

            // 记录响应结果
            if (consoleLog.logResponse()) {
                String finishLog = """
                        
                        <=== 完成方法
                        类名: %s
                        方法名: %s
                        出参: %s
                        """.formatted(className, methodName, result);

                if (consoleLog.logCostTime()) {

                    finishLog = """
                            
                            <=== 完成方法
                            类名: %s
                            方法名: %s
                            出参: %s
                            耗时: %s ms
                            """.formatted(className, methodName, result, costTime);
                }

                logByLevel(consoleLog.level(), finishLog);

            } else if (consoleLog.logCostTime()) {
                // 单独记录耗时
                logByLevel(consoleLog.level(), "耗时: {}ms", costTime);
            }


        } catch (Throwable e) {
            long costTime = System.currentTimeMillis() - startTime;

            String exLog = """
                    
                    <=== 退出方法
                    类名: %s
                    方法名: %s
                    异常: %s
                    耗时: %s ms
                    """.formatted(className, methodName, e.getMessage(), costTime);

            log.error(exLog, e);
            throw e;
        }

        return result;
    }

    private void logByLevel(ConsoleLog.LogLevel level, String format, Object... args) {
        switch (level) {
            case DEBUG:
                log.debug(format, args);
                break;
            case INFO:
                log.info(format, args);
                break;
            case WARN:
                log.warn(format, args);
                break;
            case ERROR:
                log.error(format, args);
                break;
            default:
                log.info(format, args);
        }
    }
}