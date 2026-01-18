# blink-web-starter

提供web应用的常见非业务功能如： 全局异常处理  MVC配置 自定义缓存加载组件 应用上下文 日志等等

## 全局异常处理

[GlobalExceptionHandler](src/main/java/com/blink/framework/core/config/GlobalExceptionHandler.java)


抛出的自定义的BlinkException会携带 异常代码 利用缓存组件根据异常代码和当前语言环境获取友好的错误信息 组装统一响应体返回
注：业务错误 http状态码依然为200
规范：如果在业务代码中捕获的是其他异常 请转为BlinkException类型后再抛出


## 上下文

上下文是为了不显式的在方法中传递一些公共变量或者参数 方便在当前请求中跨方法获取

BlinkRequestContextHolder
[BlinkRequestContextHolder](src/main/java/com/blink/framework/core/util/BlinkRequestContextHolder.java)

## 日志MDC
全局添加追踪链路Id 用户id


## 预加载组件
使用@PreHeatData 修饰的类或方法 会在spring context准备好 应用启动前（tomcat） 执行注解上配置的方法
用来预加载数据 或则做一些前置操作
[RedisCachePreHeatRunner](src/main/java/com/blink/framework/core/component/RedisCachePreHeatRunner.java)


## AOP
TODO 对controller进行切面 做日志和性能统计