package com.blink.gateway.base.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 查询操作日志请求DTO
 *
 * @author binblink
 * @since 2024-03-11
 */
@Getter
@Setter
public class QueryOperationLogReq extends Page {

    /**
     * 登录名
     */
    private String loginName;

    /**
     * 日志类型
     * <p>
     * LOGIN-登入日志, SYSTEM-系统日志, OPERATION-操作日志
     */
    private String logType;

    /**
     * 执行状态 0成功 1失败
     */
    private Integer executeStatus;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 关键词（搜索描述、URL）
     */
    private String keyword;

}