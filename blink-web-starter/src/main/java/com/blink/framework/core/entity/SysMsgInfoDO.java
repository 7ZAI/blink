package com.blink.framework.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * <p>
 * 消息码信息表
 * </p>
 *
 * @author binblink
 * @since 2023-12-29
 */
@Getter
@Setter
@TableName("sys_msg_info")
public class SysMsgInfoDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据字典id
     */
    @TableId(value = "msg_id", type = IdType.AUTO)
    private Integer msgId;

    /**
     * 消息代码
     */
    @TableField("msg_code")
    private String msgCode;

    /**
     * 消息描述
     */
    @TableField("msg_info")
    private String msgInfo;

    /**
     * 消息类型 错误E 警告W 成功S
     */
    @TableField("msg_type")
    private String msgType;

    /**
     * 消息语言
     */
    @TableField("msg_lang")
    private String msgLang;
}
