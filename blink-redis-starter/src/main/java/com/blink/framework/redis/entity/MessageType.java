package com.blink.framework.redis.entity;

public interface MessageType {

    // 通知类消息
     String NOTIFICATION = "NOTIFICATION";
    // 业务类消息
     String BUSINESS = "BUSINESS";
    // 事件类消息
     String EVENT = "EVENT";
    // 事务类消息
     String TRANSACTION = "TRANSACTION";
    // 日志类消息
     String LOG = "LOG";
    // 普通
    String NORMAL = "NORMAL";
}
