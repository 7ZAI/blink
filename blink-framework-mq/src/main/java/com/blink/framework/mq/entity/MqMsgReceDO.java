package com.blink.framework.mq.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 消息消费记录表
 * </p>
 *
 * @author binblink
 * @since 2023-12-09
 */
@Getter
@Setter
@TableName("mq_msg_rece")
public class MqMsgReceDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息id
     */
    @TableId("msg_id")
    private String msgId;

    /**
     * 接收者标识
     */
    @TableField("receive_id")
    private String receiveId;

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
     * 消息接收状态 ‘0’ 未消费 1 消费成功 2 消费失败
     */
    @TableField("receive_sts")
    private Integer receiveSts;

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
     * 发送者
     */
    @TableField("send_sys")
    private String sendSys;

    /**
     * 接收者
     */
    @TableField("receive_sys")
    private String receiveSys;

    /**
     * 接收时间
     */
    @TableField("receive_time")
    private LocalDateTime receiveTime;

    /**
     * 消费次数
     */
    @TableField("consumer_times")
    private Integer consumerTimes;

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
