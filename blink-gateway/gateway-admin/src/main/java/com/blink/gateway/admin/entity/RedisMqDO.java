package com.blink.gateway.admin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * redis stream消息发送记录表
 *
 * @author binblink
 * @since 2025-11-05
 */
@Getter
@Setter
@TableName("redis_mq")
public class RedisMqDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息id
     */
    @TableId("msg_id")
    private String msgId;

    /**
     * 未读 0 已读 1 发送失败2 已确认消费 3
     */
    @TableField("msg_status")
    private String msgStatus;

    /**
     * stream_id
     */
    @TableField("stream_id")
    private String streamId;

    /**
     * StreamKey
     */
    @TableField("topic")
    private String topic;

    /**
     * 消息类型 NORMAL
     */
    @TableField("msg_type")
    private String msgType;

    /**
     * 消息内容
     */
    @TableField("payload")
    private String payload;

    /**
     * payload类的全限定名
     */
    @TableField("payload_class")
    private String payloadClass;

    /**
     * 发送者
     */
    @TableField("sender")
    private String sender;

    /**
     * 接收者
     */
    @TableField("receiver")
    private String receiver;

    /**
     * 版本号
     */
    @TableField("version")
    private String version;

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
    @TableField("extra")
    private String extra;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Override
    public String toString() {
        return "RedisMqDO{" +
                "msgId='" + msgId + '\'' +
                ", msgStatus='" + msgStatus + '\'' +
                ", streamId='" + streamId + '\'' +
                ", topic='" + topic + '\'' +
                ", msgType='" + msgType + '\'' +
                ", payload='" + payload + '\'' +
                ", payloadClass='" + payloadClass + '\'' +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", version='" + version + '\'' +
                ", retryTimes=" + retryTimes +
                ", failTimes=" + failTimes +
                ", extra='" + extra + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}