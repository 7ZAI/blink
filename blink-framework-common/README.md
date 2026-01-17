##
此包为了把通用类抽出作为一个整体依赖包，这样封装避免通用类在开发过程中被修改

通用类主要为通用DTO、注解、自定义异常、常量类；

Record类是对新特性的尝试，目前未使用上


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