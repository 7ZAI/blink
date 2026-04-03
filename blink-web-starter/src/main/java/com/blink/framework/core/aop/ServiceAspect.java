package com.blink.framework.core.aop;


import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Aspect
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ServiceAspect {
}
