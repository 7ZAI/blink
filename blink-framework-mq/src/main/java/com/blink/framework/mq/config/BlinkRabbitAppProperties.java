package com.blink.framework.mq.config;


import com.blink.framework.mq.constant.ExchangeType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


/**
 * rabbitmq应用配置
 */
@Component
@ConfigurationProperties(prefix = "blink.rabbitmq")
public class BlinkRabbitAppProperties {

    /**
     * 仅针对 simple listener
     */
    private Integer prefetch = 30;

    private Integer concurrency = 1;

    private Integer maxConcurrency = 1;

    private List<Queue> queues  = new ArrayList<>();

    private List<Exchange> exchanges  = new ArrayList<>();

    private List<Binding> bindings;

    private List<DeadQueue> deadQueues = new ArrayList<>();

    public Integer getPrefetch() {
        return prefetch;
    }

    public void setPrefetch(Integer prefetch) {
        this.prefetch = prefetch;
    }

    public Integer getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(Integer concurrency) {
        this.concurrency = concurrency;
    }

    public Integer getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(Integer maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    public List<DeadQueue> getDeadQueues() {
        return deadQueues;
    }

    public void setDeadQueues(List<DeadQueue> deadQueues) {
        this.deadQueues = deadQueues;
    }

    public List<Queue> getQueues() {
        return queues;
    }

    public void setQueues(List<Queue> queues) {
        this.queues = queues;
    }

    public List<Exchange> getExchanges() {
        return exchanges;
    }

    public void setExchanges(List<Exchange> exchanges) {
        this.exchanges = exchanges;
    }

    public List<Binding> getBindings() {
        return bindings;
    }

    public void setBindings(List<Binding> bindings) {
        this.bindings = bindings;
    }

    public static class Queue {

        private String name;
        private String ttl;

        private String maxLength;

        public String getTtl() {
            return ttl;
        }

        public void setTtl(String ttl) {
            this.ttl = ttl;
        }

        public String getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(String maxLength) {
            this.maxLength = maxLength;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    public static class Exchange {

        private String name;

        private ExchangeType type;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ExchangeType getType() {
            return type;
        }

        public void setType(ExchangeType type) {
            this.type = type;
        }
    }

    public static class Binding {

        private String queueName;

        private String exchangeName;

        private String routingKey;

        public String getQueueName() {
            return queueName;
        }

        public void setQueueName(String queueName) {
            this.queueName = queueName;
        }

        public String getExchangeName() {
            return exchangeName;
        }

        public void setExchangeName(String exchangeName) {
            this.exchangeName = exchangeName;
        }

        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }
    }

    public static class DeadQueue{
        // 绑定的队列名
        private List<String> bindQueues;

        public List<String> getBindQueues() {
            return bindQueues;
        }

        public void setBindQueues(List<String> bindQueues) {
            this.bindQueues = bindQueues;
        }
    }

}
