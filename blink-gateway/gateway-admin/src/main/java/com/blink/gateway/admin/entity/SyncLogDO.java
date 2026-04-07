package com.blink.gateway.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据同步日志实体
 *
 * @author binblink
 */
@Data
@TableName("sync_log")
public class SyncLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 同步类型: channel/route/config
     */
    private String syncType;

    /**
     * 同步模式: 0-全量, 1-增量/单项
     */
    private Byte syncMode;

    /**
     * 同步的key列表(JSON数组)
     */
    private String syncKeys;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 状态: 0-成功, 1-部分失败, 2-失败
     */
    private Byte status;

    /**
     * 同步实例数量
     */
    private Integer instanceCount;

    /**
     * 成功实例数量
     */
    private Integer successCount;

    /**
     * 详细结果(JSON)
     */
    private String detail;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}