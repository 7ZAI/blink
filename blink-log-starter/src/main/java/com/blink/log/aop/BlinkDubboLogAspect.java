package com.blink.log.aop;

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
 * Dubbo 服务日志切面
 * <p>
 * 自动记录 Dubbo 服务方法的入参、出参、耗时
 * 支持 @DubboService 标注的服务类
 * 支持敏感数据脱敏
 *
 * @author binblink
 */
@Aspect
@Slf4j
public class BlinkDubboLogAspect {

    private final LogProperties properties;

    public BlinkDubboLogAspect(LogProperties properties) {
        this.properties = properties;
    }

    /**
     * 切入点：匹配带有 @DubboService 注解的类的所有方法
     * <p>
     * 只匹配 com.blink 包下的类，避免影响其他框架组件
     */
    @Pointcut("@within(org.apache.dubbo.config.annotation.DubboService) && within(com.blink..*)")
    public void dubboServicePointcut() {
    }

    @Around("dubboServicePointcut()")
    public Object aroundDubboMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = signature.getMethod();
        String methodName = targetMethod.getName();

        // 如果方法标记了 @ConsoleLog，跳过切面逻辑（由 LogExecutionAspect 处理）
        if (targetMethod.isAnnotationPresent(ConsoleLog.class)) {
            log.debug("===== Dubbo 方法 {} 标记了 @ConsoleLog，跳过 AOP 切面逻辑 =====", targetMethod.getName());
            return joinPoint.proceed();
        }

        String className = joinPoint.getTarget().getClass().getName();
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);

        Object[] args = joinPoint.getArgs();
        String argString = buildArgString(args);

        String logMessage = """

                ===> [Dubbo] 进入服务方法
                类名: %s
                方法名: %s
                入参: %s
                """.formatted(simpleClassName, methodName, argString);

        log.info(logMessage);

        long startTime = System.currentTimeMillis();
        Object result = null;
        try {
            result = joinPoint.proceed();
            long costTime = System.currentTimeMillis() - startTime;

            String resultStr = buildResultString(result);

            String outLog = """

                    <=== [Dubbo] 执行完成
                    类名: %s
                    方法名: %s
                    出参: %s
                    耗时: %s ms
                    """.formatted(simpleClassName, methodName, resultStr, costTime);

            log.info(outLog);

        } catch (Throwable e) {
            long costTime = System.currentTimeMillis() - startTime;

            String exLog = """

                    <=== [Dubbo] 退出方法
                    类名: %s
                    方法名: %s
                    执行异常: %s
                    耗时: %s ms
                    """.formatted(simpleClassName, methodName, e.getMessage(), costTime);
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
            } else if (!isSimpleType(arg.getClass())) {
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
