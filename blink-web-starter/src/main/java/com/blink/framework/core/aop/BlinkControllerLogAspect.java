package com.blink.framework.core.aop;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.core.annotation.LogExecution;
import com.blink.framework.core.config.prop.BlinkWebAppConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * controller 日志切面
 *
 * @Author binblink
 */
@Aspect
@Slf4j
public class BlinkControllerLogAspect {

    private BlinkWebAppConfigProperties properties;

    public BlinkControllerLogAspect(BlinkWebAppConfigProperties properties) {
        this.properties = properties;
    }

    /**
     * 定义切点：匹配 com.blink 包及其任意子包下，所有类名以 Controller 结尾的类的所有方法
     * 切点表达式说明：
     * - execution：匹配方法执行
     * - *：返回值任意
     * - com.blink..*Controller：com.blink 任意子包下，类名以 Controller 结尾的类（推荐，更精准）
     * - *(..)：任意方法名，任意参数
     * <p>
     * 【备选方案】若要匹配 com.blink 包下所有含 "controller" 包名的路径（如 com.blink.xxx.controller）：
     *
     * @Pointcut("execution(* com.blink..controller..*(..))")
     */
    @Pointcut("execution(* com.blink..*Controller.*(..))")
    public void controllerPointcut() {
        // 切点方法仅作标记，无业务逻辑
    }

    @Around("controllerPointcut()")
    public Object aroundControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取目标方法基础信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = signature.getMethod();
        String methodName = targetMethod.getName();

        // 判断：如果方法上有 @LogExecution 注解，直接执行原方法，跳过切面逻辑
        if (targetMethod.isAnnotationPresent(LogExecution.class)) {
            log.debug("===== 方法 {} 标记了 @LogExecution，跳过 AOP 切面逻辑 =====", targetMethod.getName());
            // 直接执行原方法并返回结果，不执行后续切面逻辑
            return joinPoint.proceed();
        }

        String className = joinPoint.getTarget().getClass().getName();

        Object[] args = joinPoint.getArgs();
        String argString = null;
        //  记录方法进入日志
        if (args != null && args.length > 0) {
            //如果不属于RequestDTO类型 如上传文件 则不记录入参值
            if (args[0] instanceof RequestDTO<?>) {
                 argString = args[0].toString();
                //开启了自动省略
                if (properties.getLog().getAutoSkip()) {
                    Integer val = properties.getLog().getUpperLimit();
                    //大于配置的上限值
                    if (argString.length() > val) {
                        argString = argString.substring(0, val);
                        //省略
                        argString = argString + "......";

                    }
                }
            }
        }
        // Java 15+ 文本块
        String logMessage = """
                
                ===> 进入方法
                类名: %s
                方法名: %s
                入参: %s
                """.formatted(className, methodName, argString);

        log.info(logMessage);

        // 统计耗时 + 执行目标方法
        long startTime = System.currentTimeMillis();
        Object result = null;
        try {
            // 执行原方法（核心，不可省略）
            result = joinPoint.proceed();
            long costTime = System.currentTimeMillis() - startTime;

            String resultStr = result.toString();

            //开启了自动省略
            if (properties.getLog().getAutoSkip()) {
                Integer val = properties.getLog().getUpperLimit();
                //大于配置的上限值
                if (resultStr.length() > val) {
                    resultStr = resultStr.substring(0, val);
                    //省略
                    resultStr = resultStr + "......";
                }
            }

            //  正常退出日志
            String outLog = """
                
                <=== 执行完成
                类名: %s
                方法名: %s
                出参: %s
                耗时: %s ms
                """.formatted(className, methodName, resultStr,costTime);

            log.info(outLog);

        } catch (Throwable e) {
            long costTime = System.currentTimeMillis() - startTime;

            String exLog = """
                
                <=== 退出方法
                类名: %s
                方法名: %s
                执行异常: %s
                耗时: %s ms
                """.formatted(className, methodName,e.getMessage(),costTime);
            log.error(exLog,e);
            // 重新抛出异常，不影响原业务的异常处理
            throw e;
        }

        return result;
    }

}
