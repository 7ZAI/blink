package com.blink.log.aop;

import com.blink.framework.common.data.RequestDTO;

import com.blink.log.annotation.ConsoleLog;
import com.blink.log.config.LogProperties;
import com.blink.log.util.SensitiveUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller 日志切面
 * <p>
 * 自动记录 Controller 方法的入参、出参、耗时
 * 支持敏感数据脱敏
 *
 * @author binblink
 */
@Aspect
@Slf4j
public class BlinkControllerLogAspect {

    private final LogProperties properties;

    public BlinkControllerLogAspect(LogProperties properties) {
        this.properties = properties;
    }

    @Pointcut("execution(* com.blink..*Controller.*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object aroundControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = signature.getMethod();
        String methodName = targetMethod.getName();

        if (targetMethod.isAnnotationPresent(ConsoleLog.class)) {
            log.debug("===== 方法 {} 标记了 @LogExecution，跳过 AOP 切面逻辑 =====", targetMethod.getName());
            return joinPoint.proceed();
        }

        String className = joinPoint.getTarget().getClass().getName();

        Object[] args = joinPoint.getArgs();
        String argString = buildArgString(args);

        String logMessage = """

                ===> 进入方法
                类名: %s
                方法名: %s
                入参: %s
                """.formatted(className, methodName, argString);

        log.info(logMessage);

        long startTime = System.currentTimeMillis();
        Object result = null;
        try {
            result = joinPoint.proceed();
            long costTime = System.currentTimeMillis() - startTime;

            String resultStr = buildResultString(result);

            String outLog = """

                <=== 执行完成
                类名: %s
                方法名: %s
                出参: %s
                耗时: %s ms
                """.formatted(className, methodName, resultStr, costTime);

            log.info(outLog);

        } catch (Throwable e) {
            long costTime = System.currentTimeMillis() - startTime;

            String exLog = """

                <=== 退出方法
                类名: %s
                方法名: %s
                执行异常: %s
                耗时: %s ms
                """.formatted(className, methodName, e.getMessage(), costTime);
            log.error(exLog, e);
            throw e;
        }

        return result;
    }

    private String buildArgString(Object[] args) {
        if (args == null || args.length == 0) {
            return "无参数";
        }

        List<String> argStrings = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null) {
                argStrings.add("null");
            } else if (arg instanceof RequestDTO<?> || !isSimpleType(arg.getClass())) {
                String str = properties.getConsole().isEnableSensitive()
                    ? SensitiveUtils.toSensitiveString(arg)
                    : arg.toString();
                argStrings.add(truncateIfNeeded(str));
            } else {
                argStrings.add(arg.toString());
            }
        }
        return String.join(", ", argStrings);
    }

    private String buildResultString(Object result) {
        if (result == null) {
            return "null";
        }
        String str = properties.getConsole().isEnableSensitive()
            ? SensitiveUtils.toSensitiveString(result)
            : result.toString();
        return truncateIfNeeded(str);
    }

    private String truncateIfNeeded(String str) {
        if (properties.getConsole().isAutoSkip() && str.length() > properties.getConsole().getUpperLimit()) {
            return str.substring(0, properties.getConsole().getUpperLimit()) + "......";
        }
        return str;
    }

    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == String.class ||
                Number.class.isAssignableFrom(clazz) ||
                clazz == Boolean.class ||
                clazz.isEnum();
    }
}
