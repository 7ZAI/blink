package com.blink.framework.mq.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 消息发送记录表
 * </p>
 *
 * @author binblink
 * @since 2023-12-06
 */
@Getter
@Setter
@TableName("mq_msg_send")
public class MqMsgSendDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息id
     */
    @TableId("msg_id")
    private String msgId;

    /**
     * 业务id
     */
    @TableField("buss_id")
    private String bussId;

    /**
     * 请求id
     */
    @TableField("req_id")
    private String reqId;

    /**
     * 消息发送状态 ‘0’未发送 1 发送成功 2 发送失败
     */
    @TableField("send_sts")
    private Integer sendSts;

    /**
     * 消息类型 N 普通 B 业务
     */
    @TableField("mq_type")
    private String mqType;

    /**
     * 工作模式 S 单消费  M 多消费
     */
    @TableField("mq_mode")
    private String mqMode;

    /**
     * 消息内容
     */
    @TableField("mq_context")
    private String mqContext;

    /**
     * 消息类
     */
    @TableField("mq_context_class")
    private String mqContextClass;

    /**
     * 消息交换机
     */
    @TableField("mq_exchange")
    private String mqExchange;

    /**
     * 消息路由key
     */
    @TableField("mq_routing_key")
    private String mqRoutingKey;

    /**
     * 发送者
     */
    @TableField("send_sys")
    private String sendSys;

    /**
     * 初始发送时间
     */
    @TableField("send_time")
    private LocalDateTime sendTime;

    /**
     * 最新发送时间
     */
    @TableField("last_send_time")
    private LocalDateTime lastSendTime;

    /**
     * 是否允许重发 0 开启 1关闭
     */
    @TableField("enable_retry")
    private Integer enableRetry;

    /**
     * 发送次数
     */
    @TableField("retry_times")
    private Integer retryTimes;

    /**
     * 失败次数
     */
    @TableField("fail_times")
    private Integer failTimes;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value= "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


}
