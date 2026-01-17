package com.blink.framework.mq.config;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.blink.framework.mq.constant.ExchangeType;
import jakarta.annotation.Resource;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 队列配置
 */
@Configuration
public class RabbitQueueAppConfig {

    @Resource
    private BlinkRabbitAppProperties properties;


    /**
     * 集合声明方式 可以多个queue exchange binding一起声明
     * 在配置文件中支持 只配置 bindings 和 deadQueues 就可以声明完全 即 queues 、exchanges 可以缺省
     *
     * @return
     */
    @Bean
    public Declarables ds() {

        HashMap<String, Queue> queues = new HashMap<>();
        HashMap<String, Exchange> exchanges = new HashMap<>();
        List<Binding> bindings = new ArrayList<>();

        // 优先遍历绑定值 此项一定要配置
        properties.getBindings().forEach(bind -> {

            String queueName = bind.getQueueName();
            String exchange = bind.getExchangeName();

            bindings.add(new Binding(queueName, Binding.DestinationType.QUEUE,
                    exchange, bind.getRoutingKey(), null));

            queues.put(queueName, QueueBuilder.durable(queueName).build());
            exchanges.put(exchange, ExchangeBuilder.directExchange(exchange).build());

        });

        // 遍历死信队列 死信队列就是一个正常的队列 声明即可 而和死信绑定的队列需要配置死信参数
        List<BlinkRabbitAppProperties.DeadQueue> deadQueues = properties.getDeadQueues();

        for (int i = 0; i < deadQueues.size(); i++) {

            String deadQueueName = "dead_queue_" + i;
            String deadExchange = "dead_exchange_" + i;
            String deadRoutingKey = "dead_key_" + i;

            queues.put(deadQueueName, QueueBuilder.durable(deadQueueName).build());
            exchanges.put(deadExchange, ExchangeBuilder.topicExchange(deadExchange).build());

            bindings.add(new Binding(deadQueueName, Binding.DestinationType.QUEUE,
                    deadExchange, deadRoutingKey, null));

            deadQueues.get(i).getBindQueues().forEach(s -> {

                Queue queue = QueueBuilder.durable(s).maxLength(1024).
                        ttl(10000).
                        deadLetterExchange(deadExchange).
                        deadLetterRoutingKey(deadRoutingKey)
                        .build();
                queues.put(s, queue);
            });
        }

        // 遍历exchanges
        properties.getExchanges().forEach(ex -> {

            String exName = ex.getName();
            Exchange exchange = exchanges.get(exName);
            // 在之前没有创建过
            if (Objects.isNull(exchange)) {

                exchange = getProperExchange(ex.getType(), exName);
                exchanges.put(exName, exchange);
                return;
            }
            //创建过但是类型不同 以exchanges中配置的类型为准
            if (Objects.nonNull(exchange) && !exchange.getType().equals(ex.getType().getName())) {

                exchange = getProperExchange(ex.getType(), exName);
                exchanges.put(exName, exchange);
            }

        });


        // 遍历queues
        properties.getQueues().forEach(q -> {

            String queueName = q.getName();
            String maxLength = q.getMaxLength();
            String ttl = q.getTtl();

            Queue queue = queues.get(queueName);
            // 在之前没有创建过
            if (Objects.isNull(queue)) {

                queue = QueueBuilder.durable(queueName).build();

                if (!StringUtils.isEmpty(maxLength)) {
                    queue.addArgument("x-max-length", maxLength);
                }
                if (!StringUtils.isEmpty(ttl)) {
                    queue.addArgument("x-message-ttl", ttl);
                }

            } else {

                if (!StringUtils.isEmpty(maxLength)) {
                    queue.addArgument("x-max-length", maxLength);
                }

                if (!StringUtils.isEmpty(ttl)) {
                    queue.addArgument("x-message-ttl", ttl);
                }
            }

            queues.put(queueName, queue);

        });


        Declarables ds = new Declarables();

        Collection<Declarable> collection = ds.getDeclarables();

        collection.addAll(queues.values().stream().collect(Collectors.toList()));
        collection.addAll(exchanges.values().stream().collect(Collectors.toList()));
        collection.addAll(bindings);


        return ds;
    }


    /**
     * 获得对应类型的交换机
     *
     * @param type
     * @param exName
     * @return
     */
    private Exchange getProperExchange(ExchangeType type, String exName) {

        Exchange exchange = null;
        if (ExchangeType.DIRECT.equals(type)) {
            exchange = ExchangeBuilder.directExchange(exName).build();
        }

        if (ExchangeType.TOPIC.equals(type)) {
            exchange = ExchangeBuilder.topicExchange(exName).build();
        }

        if (ExchangeType.FANOUT.equals(type)) {
            exchange = ExchangeBuilder.fanoutExchange(exName).build();
        }

        if (ExchangeType.HEADERS.equals(type)) {
            exchange = ExchangeBuilder.headersExchange(exName).build();
        }

        return exchange;
    }

}
