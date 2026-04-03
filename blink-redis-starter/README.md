# blink-redis-starter

封装 Redis 的依赖，提供生产级的 Redis 操作组件，包括分布式序列号生成、分布式锁、Redis Stream 等功能。

## 功能特性

- **Redis 客户端封装** - 提供同步 `RedisClient` 和响应式 `ReactiveRedisClient`
- **分布式序列号生成** - 基于 Lua 脚本的高性能序列号生成器，支持百万 QPS
- **分布式锁** - 基于 Redisson 的分布式锁，支持注解式和编程式两种使用方式
- **Redis Stream** - 封装 Stream 生产者和消息体
- **本地缓存** - 可选的 Caffeine 本地缓存支持

## 快速开始

### 引入依赖

```gradle
implementation 'com.blink:blink-redis-spring-boot-starter:1.0.0-SNAPSHOT'
```

### 配置 Redis 连接

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: 123456
      database: 0
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          max-wait: -1ms
          min-idle: 0
```

### 使用 Redis 客户端

```java
@Resource
private RedisClient redisClient;

@Resource
private ReactiveRedisClient reactiveRedisClient;
```

## 自动配置

模块根据 Web 应用类型（Servlet 或 Reactive）自动配置对应组件：

| 组件 | Servlet 环境 | Reactive 环境 |
|------|-------------|---------------|
| RedisTemplate | ✅ | ✅ |
| RedisClient | ✅ | ❌ |
| ReactiveRedisClient | ❌ | ✅ |
| SeqGenerator | ✅ | ❌ |
| ReactiveSeqGenerator | ❌ | ✅ |
| CacheComponent | ✅ | ❌ |

配置类：[BlinkRedisAutoConfiguration](src/main/java/com/blink/framework/redis/config/BlinkRedisAutoConfiguration.java)

---

## 分布式锁

基于 Redisson 实现的分布式锁，支持注解式和编程式两种使用方式。

### 依赖要求

需要引入 Redisson 依赖：

```gradle
implementation 'org.redisson:redisson-spring-boot-starter:3.24.3'
```

### 配置项

```yaml
blink:
  redis:
    distributed-lock:
      enabled: true                    # 是否启用分布式锁，默认 false
      default-wait-time: 3s            # 默认等待时间，默认 3 秒
      default-lease-time: 30s          # 默认租约时间，默认 30 秒
      watchdog-enabled: true           # 是否启用看门狗，默认 true
      watchdog-timeout: 30s            # 看门狗超时时间，默认 30 秒
      retry-interval: 100ms            # 重试间隔，默认 100 毫秒
      retry-count: 3                   # 重试次数，默认 3 次
      key-prefix: "lock:"              # 锁 key 前缀，默认 "lock:"
```

### 方式一：注解式（推荐）

在方法上添加 `@DistributedLock` 注解，AOP 自动处理锁的获取和释放。

#### 基本用法

```java
@Service
public class OrderService {

    @DistributedLock("create-order")
    public void createOrder(OrderDTO orderDTO) {
        // 此方法执行时会自动获取分布式锁
        // 锁 key: lock:create-order
    }
}
```

#### SpEL 表达式支持

支持 SpEL 表达式动态生成锁 key：

```java
@Service
public class UserService {

    @DistributedLock(key = "'user:' + #userId")
    public void updateUser(Long userId, UserDTO userDTO) {
        // 锁 key: lock:user:123
    }

    @DistributedLock(key = "'order:' + #order.id")
    public Order processOrder(Order order) {
        // 支持嵌套属性访问
        return order;
    }
}
```

#### 自定义等待和租约时间

```java
@DistributedLock(
    key = "'payment:' + #paymentId",
    waitTime = 5,              // 等待 5 秒获取锁
    leaseTime = 60,            // 锁租约 60 秒
    timeUnit = TimeUnit.SECONDS
)
public PaymentResult processPayment(String paymentId) {
    // 业务逻辑
}
```

#### 公平锁

```java
@DistributedLock(
    key = "'inventory:' + #productId",
    fairLock = true            // 使用公平锁，按请求顺序获取
)
public void deductInventory(Long productId, int quantity) {
    // 公平锁保证按顺序获取
}
```

#### 失败策略

```java
public enum LockFailureStrategy {
    THROW_EXCEPTION,        // 抛出异常（默认）
    RETURN_NULL,            // 返回 null
    EXECUTE_WITHOUT_LOCK,   // 无锁执行（慎用）
    RETRY                   // 重试
}
```

使用示例：

```java
@DistributedLock(
    key = "'seckill:' + #productId",
    failureStrategy = LockFailureStrategy.RETURN_NULL,
    errorMessage = "秒杀太火爆，请稍后重试"
)
public SeckillResult seckill(Long productId, Long userId) {
    // 获取不到锁时返回 null，不抛异常
    return doSeckill(productId, userId);
}
```

### 方式二：编程式

注入 `DistributedLockClient` 进行编程式锁操作。

```java
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final DistributedLockClient lockClient;

    public boolean deductStock(Long productId, int quantity) {
        String lockKey = "product:" + productId;
        
        if (lockClient.tryLock(lockKey)) {
            try {
                return doDeductStock(productId, quantity);
            } finally {
                lockClient.unlock(lockKey);
            }
        }
        return false;
    }
}
```

#### executeWithLock - 自动管理锁

推荐使用 `executeWithLock`，自动处理锁的获取和释放：

```java
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final DistributedLockClient lockClient;

    public PaymentResult pay(String orderId) {
        return lockClient.executeWithLock("order:" + orderId, () -> {
            Payment payment = validatePayment(orderId);
            deductBalance(payment);
            return confirmPayment(payment);
        });
    }
}
```

#### 自定义等待和租约时间

```java
public void process(String resourceId) {
    lockClient.executeWithLock(
        resourceId,
        Duration.ofSeconds(5),    // 等待 5 秒
        Duration.ofMinutes(2),    // 租约 2 分钟
        () -> {
            // 业务逻辑
        }
    );
}
```

#### 读写锁

```java
public class CacheService {

    private final DistributedLockClient lockClient;

    public Object read(String key) {
        RLock readLock = lockClient.getReadLock("cache:" + key);
        readLock.lock();
        try {
            return doRead(key);
        } finally {
            readLock.unlock();
        }
    }

    public void write(String key, Object value) {
        RLock writeLock = lockClient.getWriteLock("cache:" + key);
        writeLock.lock();
        try {
            doWrite(key, value);
        } finally {
            writeLock.unlock();
        }
    }
}
```

#### 锁状态查询

```java
public void checkLock(String resourceId) {
    String lockKey = "resource:" + resourceId;
    
    if (lockClient.isLocked(lockKey)) {
        log.info("资源 {} 已被锁定", resourceId);
    }
    
    if (lockClient.isHeldByCurrentThread(lockKey)) {
        log.info("当前线程持有锁");
    }
}
```

### 最佳实践

1. **锁 key 设计** - 使用业务前缀 + 唯一标识，如 `order:123`, `user:456`
2. **合理设置租约时间** - 业务执行时间 + 缓冲时间，避免锁提前释放
3. **启用看门狗** - 长时间任务建议启用看门狗自动续期
4. **异常处理** - 使用 `executeWithLock` 确保锁一定被释放
5. **避免死锁** - 不要在锁内调用其他需要锁的方法

---

## 分布式序列号生成

基于 Lua 脚本的高性能序列号生成器，采用段号模式，支持百万 QPS。

### 特性

- **分布式部署** - 多实例间序列号不重复
- **趋势递增** - 保证趋势递增，非严格递增
- **全局唯一** - 本地缓存使用 AtomicLong 保证原子性
- **高性能** - 单机百万 QPS

### 配置

```yaml
blink:
  redis:
    enableLocalCache: false
    idGenerator:
      seq-param:
        request:          # key 后缀
          step: 1000      # 本地缓存序列号数量
          fetch-percent: 0.8  # 触发刷新阈值
        trance:
          step: 2000
```

### 使用

```java
@Resource
private IdGenerator idGenerator;

public Long generateRequestId() {
    return idGenerator.nextId("request");
}

@Resource
private ReactiveIdGenerator reactiveIdGenerator;

public Mono<Long> generateRequestIdReactive() {
    return reactiveIdGenerator.nextId("request");
}
```

源码：[IdGenerator](src/main/java/com/blink/framework/redis/id/IdGenerator.java) | [ReactiveIdGenerator](src/main/java/com/blink/framework/redis/id/ReactiveIdGenerator.java)

---

## Redis Stream

支持 Redis Stream 消息的生产和消费。

### 发送消息

```java
@Resource
private RedisClient redisClient;

public String sendMessage(String streamKey, Map<String, Object> message) {
    return redisClient.xAdd(streamKey, message);
}
```

### 消费消息

```java
public void consumeMessages(String streamKey, String groupName) {
    redisClient.xGroupCreate(streamKey, groupName, "0-0");
    
    Consumer consumer = Consumer.from(groupName, "consumer-1");
    List<Map<String, Object>> messages = redisClient.xReadGroup(
        consumer, streamKey, groupName, 10, 5000
    );
    
    for (Map<String, Object> msg : messages) {
        try {
            processMessage(msg);
            redisClient.xAck(streamKey, groupName, (String) msg.get("id"));
        } catch (Exception e) {
            log.error("处理消息失败", e);
        }
    }
}
```

---

## RedisClient 常用操作

```java
@Resource
private RedisClient redisClient;

// String 操作
redisClient.set("key", "value");
redisClient.setEx("key", "value", Duration.ofMinutes(30));
Object value = redisClient.get("key");

// Hash 操作
redisClient.hPutField("user:1", "name", "张三");
Map<String, Object> user = redisClient.hGetStringMap("user:1");

// List 操作
redisClient.lPush("queue", "item1", "item2");
Object item = redisClient.rPop("queue");

// Set 操作
redisClient.sAdd("tags", "java", "redis");
Set<Object> tags = redisClient.sMembers("tags");

// ZSet 操作
redisClient.zAdd("leaderboard", "player1", 100.0);
Set<Object> top10 = redisClient.zRange("leaderboard", 0, 9);

// 批量操作
Map<String, Object> batch = Map.of("k1", "v1", "k2", "v2");
redisClient.batchSet(batch);
redisClient.batchSetWithExpire(batch, 60, TimeUnit.SECONDS);

// 批量删除
redisClient.deleteByPrefixScan("temp:*");
```

---

## 测试

测试类位于 `src/test/java/com/blink/redis/`：

- [DistributedLockTest](src/test/java/com/blink/redis/DistributedLockTest.java) - 分布式锁测试
- [IdGeneratorTest](src/test/java/com/blink/redis/IdGeneratorTest.java) - 序列号生成测试
