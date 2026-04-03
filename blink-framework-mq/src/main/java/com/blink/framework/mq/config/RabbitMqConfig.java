package com.blink.framework.mq.config;

import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.mq.MyFatalExceptionStrategy;
import com.blink.framework.mq.componet.Fastjson2MessageConverter;
import com.blink.framework.mq.componet.MessageReturnCallBack;
import jakarta.annotation.Resource;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.boot.autoconfigure.amqp.RabbitRetryTemplateCustomizer;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.ErrorHandler;


@Configuration
@EnableRabbit
public class RabbitMqConfig {

    /**
     * 连接配置 根据配置文件自动注入
     */
    @Resource
    private CachingConnectionFactory connectionFactory;

    @Resource
    private MessageReturnCallBack messageReturnCallBack;

    @Resource
    private BlinkRabbitAppProperties properties;


    /**
     * 配置收发信息操作 模板
     * @return
     */
    @Bean
    public RabbitTemplate rabbitTemplate() {

        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        template.setMessageConverter(fastJsonMessageConverter());
        // 当mandatory标志位设置为true时，如果exchange无法找到queue，那么broker会调用basic.return方法将消息返还给生产者;
        // 当mandatory设置为false时，出现上述情况broker会直接将消息丢弃;通俗的讲，
        // mandatory标志告诉broker代理服务器至少将消息route到一个队列中，否则就将消息return给发送者;
        template.setMandatory(true);

        //手动确认消息回调 交换机成功接收到消息 ack true
        // 以为每个Message 配置回调CorrelationData的方式替换掉全局配置 这样更加灵活
//        template.setConfirmCallback(messageConfirmReturn);

        //交换机把消息投递到队列 队列未收到消息时回调
        // 一般发生情景：路由key配置错误;
        template.setReturnsCallback(messageReturnCallBack);

//        // 消息接收时  后置处理器 消息未被转换前比如解压消息
//        template.setAfterReceivePostProcessors();
//        //消息发送前 后置处理器 比如压缩消息
//        template.setBeforePublishPostProcessors();
//        //消息发送前 可以同时针对 Message 和 CorrelationData 对线进行内容设置修改
//        template.setCorrelationDataPostProcessor();

        return template;
    }


    /**
     * 配置消息转换方式 以json格式转换
     *
     * @return
     */
    @Bean
    public Fastjson2MessageConverter fastJsonMessageConverter(){
        return new Fastjson2MessageConverter();
    }



    /**
     *  配置消费端消息接收监听器容器 通过@RabbitListener 采用push的方式进行消费
     *  configurer 已在自动配置类中配置 详情 RabbitAnnotationDrivenConfiguration
     * @return
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(SimpleRabbitListenerContainerFactoryConfigurer configurer){

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();


        factory.setConnectionFactory(connectionFactory);

        factory.setMessageConverter(fastJsonMessageConverter());
        // 配置同时存在消费端个数 相当于消费端多线程
        factory.setConcurrentConsumers(properties.getConcurrency());
        factory.setMaxConcurrentConsumers(properties.getMaxConcurrency());
        // 每个消费端一次从队列中获取消息数量
        factory.setPrefetchCount(properties.getPrefetch());
        //设置消费者确认模式为 手动确定
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);


        configurer.configure(factory, connectionFactory);

        factory.setErrorHandler(errorHandler());


        return factory;
    }

    /**
     * 自定义 消费端 RetryTemplate
     * @return
     */
    @Bean
    public RabbitRetryTemplateCustomizer retryTemplateCustomizers(){

        return (target,retryTemplate) ->  {
            retryTemplate = retryTemplate(retryTemplate);
        };
    }
    @Bean
    public ErrorHandler errorHandler() {

        return new ConditionalRejectingErrorHandler(new MyFatalExceptionStrategy());
    }



    /**
     * 自定义 RetryTemplate
     * @param retryTemplate
     * @return
     */
    private RetryTemplate retryTemplate(RetryTemplate retryTemplate){

        retryTemplate.registerListener(new RetryListener() {

            // 开启重试之前
            @Override
            public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
                //自动应答时设置为true 一般情景下  true 放过继续执行retry机制
                // false 则拒绝执行本次retry机制 如果在auto模式下 没有对应答信息进行recover 即没有处理 unnack信息 一切默认的话（重新入队） 会死循环 不断重启retry
                return true;
            }

            // 最后一次重试之后 调用
            @Override
            public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            }
            // 每次失败会调用
            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                System.out.println("第" + context.getRetryCount() + "次重试  失败！");
                if(context!=null){
                    Throwable t =  context.getLastThrowable();
                    if(t != null){
                        // 业务上的异常 取消重试
                        if( t.getCause() instanceof BlinkException){
                            // 取消重试
                            context.setExhaustedOnly();
                        }
                    }
                }
            }
        });

        // 重试 休眠机制 重试的时候 先休眠一段时间 在开启重试
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();

        //初始休眠时间，默认100毫秒
        backOffPolicy.setInitialInterval(1000);
        //指定乘数，当前休眠时间*multiplier即为下一次的休眠时间；
        backOffPolicy.setMultiplier(2.0);
        //最大休眠时间 指定最大休眠时间，默认30秒，避免multiplier过大引起无限期等待
        backOffPolicy.setMaxInterval(10000);
        //休眠重试 策略 默认为不休眠
        retryTemplate.setBackOffPolicy(backOffPolicy);

        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3));

        return retryTemplate;
    }

}
