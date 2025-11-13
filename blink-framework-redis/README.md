暂时放弃模块化 其他依赖不支持

```java
module com.blink.framework.redis {
    requires static lombok;
    requires org.slf4j;
    requires jakarta.annotation;
    requires spring.core;
    requires spring.boot;
    requires spring.data.commons;
    requires spring.boot.autoconfigure;
    requires spring.data.redis;

//    requires fastjson2.extension.spring6;
    requires com.alibaba.fastjson2;
    requires com.alibaba.fastjson2.extension;
    requires lettuce.core;
    requires com.github.benmanes.caffeine;
    requires reactor.core;
    requires org.reactivestreams;
    requires cn.hutool.core;
    requires com.blink.framework.common;
    requires spring.context;
    requires spring.beans;
    requires redisson;
    requires fastjson2.extension.spring6;
//    requires spring6;


    exports com.blink.framework.redis.component;
    exports com.blink.framework.redis.mq;
    exports com.blink.framework.redis.id;
    exports com.blink.framework.redis.lock;
}
}
```