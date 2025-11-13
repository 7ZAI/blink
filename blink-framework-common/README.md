


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