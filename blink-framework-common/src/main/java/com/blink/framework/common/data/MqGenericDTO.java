package com.blink.framework.common.data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MQ 消息传输封装
 */
public class MqGenericDTO<T> implements Serializable {

    /**
     * 消息id
     */
    private String msgId;
    /**
     * 请求id
     */
    private String reqId;

    /**
     * 业务id
     */
    private String bussId;

    /**
     * 消息类型 N 普通  B 业务
     */
    private String mqType;

    /**
     * 消息内容
     */
    private T body;

    /**
     * 工作模式 S 单消费  M 多消费
     */
    private String mqMode;

    /**
     * 消息发送方
     */
    private String sender;

    /**
     * 消息接收方
     */
    private String receiver;

    /**
     * 消息发起时间
     */
    private LocalDateTime sendTime;

    /**
     * 是否允许重发 0 开启 1关闭
     */
    private Integer enableRetry;

    /**
     * 消息发起时登入的系统用户id
     */
    private String userId;

    /**
     * 消息发起ip
     */
    private String clientIp;

    /**
     * 消息生产者bean
     */
    private String producerBean;

    /**
     * 消息消费者者bean
     */
    private String consumerBean;

    /**
     * 消息exchange
     */
    private String mqExchange;

    /**
     * 消息路由key
     */
    private String mqRoutingKey;

    /**
     * 消息存在的队列
     */
    private String queue;

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }

    public String getMqType() {
        return mqType;
    }

    public void setMqType(String mqType) {
        this.mqType = mqType;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public LocalDateTime getSendTime() {
        return sendTime;
    }

    public void setSendTime(LocalDateTime sendTime) {
        this.sendTime = sendTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getProducerBean() {
        return producerBean;
    }

    public void setProducerBean(String producerBean) {
        this.producerBean = producerBean;
    }

    public String getConsumerBean() {
        return consumerBean;
    }

    public void setConsumerBean(String consumerBean) {
        this.consumerBean = consumerBean;
    }


    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getBussId() {
        return bussId;
    }

    public void setBussId(String bussId) {
        this.bussId = bussId;
    }

    public String getMqMode() {
        return mqMode;
    }

    public void setMqMode(String mqMode) {
        this.mqMode = mqMode;
    }

    public Integer getEnableRetry() {
        return enableRetry;
    }

    public void setEnableRetry(Integer enableRetry) {
        this.enableRetry = enableRetry;
    }

    public String getMqExchange() {
        return mqExchange;
    }

    public void setMqExchange(String mqExchange) {
        this.mqExchange = mqExchange;
    }

    public String getMqRoutingKey() {
        return mqRoutingKey;
    }

    public void setMqRoutingKey(String mqRoutingKey) {
        this.mqRoutingKey = mqRoutingKey;
    }

    @Override
    public String toString() {
        return "MqGenericDTO{" +
                "msgId='" + msgId + '\'' +
                ", reqId='" + reqId + '\'' +
                ", bussId='" + bussId + '\'' +
                ", mqType='" + mqType + '\'' +
                ", body=" + body +
                ", mqMode='" + mqMode + '\'' +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", sendTime=" + sendTime +
                ", enableRetry=" + enableRetry +
                ", userId='" + userId + '\'' +
                ", clientIp='" + clientIp + '\'' +
                ", producerBean='" + producerBean + '\'' +
                ", consumerBean='" + consumerBean + '\'' +
                ", mqExchange='" + mqExchange + '\'' +
                ", mqRoutingKey='" + mqRoutingKey + '\'' +
                ", queue='" + queue + '\'' +
                '}';
    }
}
