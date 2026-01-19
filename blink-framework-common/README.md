# blink-framework-common

此模块为了把通用类抽出封装为一个整体依赖包，这样封装避免通用类在开发过程中被修改。
在软件开发周期中即使添加或者修改了 也可以发布新版本，这样实现解耦 避免牵一发而动全身
其他的模块的封装也是出于此目的，但是它们面向具体的基础功能，而common包强调全局通用的特性 全局可用。

通用类主要为通用DTO、注解、自定义异常、常量类，公共util类等

Record类是对新特性的尝试，目前未使用上

## 通用请求响应DTO

使用类 通用元数据 + 业务正文的信封模型设计，实现所有 API 请求响应格式的标准化

[RequestDTO](src/main/java/com/blink/framework/common/data/RequestDTO.java) 
[ResponseDTO](src/mainjava/com/blink/framework/common/data/ResponseDTO.java)
[PageDTO](src/mainjava/com/blink/framework/common/data/PageDTO.java)

使用示例
```java

        //只有元数据 空请求体
        RequestDTO.newInstance();
        //设置具体请求体
        RequestDTO.newInstance(T);
        
        //响应成功 业务数据为空时
        ResponseDTO.newSuccessInstance();
        //响应失败
        ResponseDTO.newFailInstance();
        //响应成功 设置具体业务响应体
        ResponseDTO.newSuccessInstance(T)
        
        //分页请求    
        QuerySysConfigReqDTO extends PageDTO

        //分页请求    
        QuerySysConfigReqDTO extends PageDTO
        
        //分页响应
        QuerySysConfigRspDTO extends PageDTO<SysConfigDO>

```

## 自定义运行时异常类

分为普通异常和业务异常 

使用示例
```java
        //业务异常
        BlinkException.throwBusinessException("自定义错误码");
        //普通系统异常
        BlinkException.throwException("自定义错误码");

        //包装原始异常后,抛出
        try{
            //代码
        } catch (Exception e) {
            throw new BlinkException(e,"自定义错误码");
        }

```
规范：捕获原始异常后需要转换为自定义异常抛出 避免对外暴露错误细节
[BlinkException](src/main/java/com/blink/framework/common/exception/BlinkException.java)



## 工具类ApplicationContextUtil

由于ApplicationContextUtil 能够在非spring管理的类中获取bean 使用比较频繁
所以将它归入common中，但是common包有尽量减少依赖项的原则 而ApplicationContextUtil本身需要
注册为bean,目前妥协引入spring boot starter依赖和starter机制 外部引用common时自动注册，否则需要手动注册





暂时放弃模块化 其他依赖不支持

```java
module com.blink.framework.common {
    requires jakarta.validation;
    requires spring.beans;
    requires spring.context;
    exports com.blink.framework.common.annotation;
    exports com.blink.framework.common.constrant;
    exports com.blink.framework.common.context;
    exports com.blink.framework.common.data;
    exports com.blink.framework.common.exception;
    exports com.blink.framework.common.mq;
    exports com.blink.framework.common.utils;

    opens com.blink.framework.common.annotation;
    opens com.blink.framework.common.data;
}
```