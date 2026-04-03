package com.blink.framework.mq.constant;

public class MqConstant {

    /**
     * 默认队列名
     */
    public static final String DEFAULT_QUEUE = "blink";

    /**
     * 默认路由key名
     */
    public static final String DEFAULT_ROUTING_KEY = "blink";


    /**
     * 默认交换机名称
     */
    public static final String DEFAULT_EXCHANGE_NAME = "blink_exchange";

    /**
     * mq消息状态 未发送 0  未消费 0
     */
    public static final Integer MQ_STS_NO_HANDLE = 0;

    /**
     * mq消息重试开关 开启 0  关闭 1
     */
    public static final Integer MQ_ENABLE_RETRY = 0;

    /**
     * mq消息重试开关 开启 0  关闭 1
     */
    public static final Integer MQ_DISABLE_RETRY = 1;

    /**
     * mq消息状态 发送成功 1 消费成功 1 success
     */
    public static final Integer MQ_STS_SUCCESS = 1;

    /**
     * mq消息状态 发送失败 2 消费失败
     */
    public static final Integer MQ_STS_FAIL = 2;

    /**
     * mq消息状态 回退 3 return
     */
    public static final Integer MQ_STS_RETURN = 3;

    /**
     * mq 消息消费模式 单次消费
     */
    public static final String MQ_CONSUMER_MODE_SINGLE = "S";

    /**
     * mq 消息消费模式 多次消费
     */
    public static final String MQ_CONSUMER_MODE_MULTIPLE = "M";
}
