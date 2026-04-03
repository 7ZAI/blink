# blink-framework-mq

RabbitMQ 消息队列封装模块，提供统一的消息收发 API。

## 功能特性

- ✅ 统一消息格式（JSON）
- ✅ 注解方式收发消息
- ✅ 支持外部配置覆盖默认配置
- ✅ 可靠性投递
- ✅ 消费确认机制
- 🚧 消息模式支持（强业务类型、简单类型）

## 快速开始

### 引入依赖

```gradle
implementation 'com.blink:blink-framework-mq:1.0.0-SNAPSHOT'
```

### 配置 RabbitMQ

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    listener:
      simple:
        acknowledge-mode: manual  # 手动确认
        concurrency: 5
        max-concurrency: 10
```

## 使用方式

### 发送消息

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final MqMessageProducer producer;

    public void createOrder(OrderDTO order) {
        // 发送消息
        producer.send("order.exchange", "order.create", order);
    }
}
```

### 接收消息

```java
@Component
@Slf4j
public class OrderConsumer {

    @RabbitListener(
        bindings = @QueueBinding(
            value = @Queue("order.create.queue"),
            exchange = @Exchange("order.exchange"),
            key = "order.create"
        )
    )
    public void onOrderCreate(OrderDTO order, Channel channel, Message message) {
        try {
            // 处理订单
            processOrder(order);
            // 确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // 拒绝消息，重新入队
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}
```

## 消息实体

```java
public class MqGenericDTO<T> implements Serializable {
    private String messageId;      // 消息唯一ID
    private String traceId;        // 追踪ID
    private String source;         // 消息来源
    private Long timestamp;        // 时间戳
    private T body;               // 消息体
    // ...
}
```

## 配置说明

### 默认配置

```yaml
blink:
  mq:
    # 是否启用消息队列
    enabled: true
    # 消息格式
    message-format: json
    # 默认交换机
    default-exchange: blink.default.exchange
    # 重试次数
    retry-count: 3
    # 重试间隔（毫秒）
    retry-interval: 5000
```

## 消息模式

| 模式 | 说明 | 使用场景 |
|------|------|---------|
| 强业务类型 | 可靠性投递、消费确认、死信队列 | 重要业务消息 |
| 简单类型 | 普通消息投递 | 非关键消息 |

## 可靠性投递

1. **生产者确认**：消息发送到 Exchange 后确认
2. **消息持久化**：消息持久化到磁盘
3. **消费者确认**：消费者处理完成后手动确认
4. **死信队列**：处理失败的消息进入死信队列

## 注意事项

- 当前模块处于 DEMO 状态，功能待完善
- 未来可能使用 Spring Cloud Stream 进行更高层抽象
- 未来提供 Kafka、RocketMQ 等模块

## 相关模块

- `blink-redis-starter`：Redis Stream 替代方案
