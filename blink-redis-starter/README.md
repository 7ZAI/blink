

# blink-redis-starter

封装Redis的依赖、基于lua脚本的分布式序列号生成组件、 Redis Stream生产者 和消息体封装
提供了同步阻塞的redis操作组件、和非阻塞式响应式操作组件
并封装成自动配置的starter

## 自动化配置

模块会根据web应用的类型(servlet或者 reactive)进行对应的配置;
其中缓存组件CacheComponent只有在servlet环境中生效 

[BlinkRedisAutoConfiguration](src/main/java/com/blink/framework/redis/config/BlinkRedisAutoConfiguration.java)


## 外部引用
 将本模块打包发布后引入依赖

 ```gradle
 implementation 'com.blink:blink-redis-spring-boot-starter:1.0.0-SNAPSHOT'
 ```
并配置 redis lettuce连接
```yml
 spring: 
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: 123456
      database: 0 # 数据库索引
      lettuce:
        pool:
          max-active: 8 # 连接池最大活跃连接数（使用负值表示没有限制）
          max-idle: 8 # 连接池最大空闲连接数
          max-wait: -1ms # 连接池最大阻塞等待时间 毫秒（使用负值表示没有限制）
          min-idle: 0 # 最小空闲连接数

```
直接使用注解声明自动注入即可使用
```java
    @Resource
    private ReactiveRedisClient reactiveRedisClient;

    @Resource
    private RedisClient redisClient;
```

## 分布式序列号生成组件

可以按照配置使用多个不同的业务key同时生成顺序号 组件使用段号模式 在本地缓存一定数量的顺序号提供使用，
在消费达到阈值时，会预取新的段号缓存。本地缓存使用完后会使用预取的段号刷新缓存


支持分布式部署： 组件采用lua+redis 由于redis原子执行lua的特性，不同实例间同一个key生成段号时不可能重复
支持趋势递增：lua 采用INCRBY命令 按配置步长递增，入库时不是严格递增 但是能保证趋势递增
全局唯一：本地缓存使用AtomicLong getAndIncrement保证原子性 final保证最大值不可变
支持自定义组装成分布式id:可以指定长度生成顺序号，组装成雪花id或者其他形式
性能：单纯的组件压测 达百万QPS 即1秒内可以生成百万序号

[IdGenerator](src/main/java/com/blink/framework/redis/id/IdGenerator.java)
[ReactiveIdGenerator](src/main/java/com/blink/framework/redis/id/ReactiveIdGenerator.java)



所有在服务应用中 用于生成序列号key(后缀)值，必须要配置到yml中

定义配置属性类[BlinkRedisProperties](src/main/java/com/blink/framework/redis/id/BlinkRedisProperties.java)
配置项例子如下
```yaml
blink:
  redis:
    ## 是否开启本地缓存 使用caffeineCache
    enableLocalCache: false
    ## 分布式id生成器
    idGenerator:
      ##参数
      seq-param:
        ## key前缀 requestid 序列号
        request:
          ## 步长或 本地缓存序列号数量
          step: 1000
          ## 触发刷新的阈值
          fetch-percent: 0.8
          ## tranceid 序列号 key后缀 实际中redis中的key值为 seq:trance
        trance:
          step: 2000
```


## 测试

测试需要在build.gradle 和测试类中手动注释代码 来切换组件的使用环境(servlet或者 reactive)

顺序号测试
[IdGeneratorTest](src/test/java/com/blink/redis/IdGeneratorTest.java)

TODO:添加redission分布式锁封装

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

```