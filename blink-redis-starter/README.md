

# blink-redis-starter

针对Redis的配置依赖、使用操作、封装成内部使用的starter

其中包含了基于lua脚本的顺序号生成、Redis Stream相关、redission依赖

提供了同步阻塞的redis操作组件、和非阻塞式响应式操作组件


配置项例子如下
```yaml
blink:
  redis:
    ## sync or reactive
    mode: reactive  
    ## 是否开启本地缓存 使用caffeineCache
    enableLocalCache: false
    ## 分布式id生成器
    idGenerator:
      ## 步进值 也是应用每次缓存顺序号数量 
      key-steps:
        ## 分布式id key名称: 步进数值
        MQ_MSG_ID: 100
```

TODO: 模块化

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