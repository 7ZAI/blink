package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 路由分组视图对象
 *
 * @author binblink
 * @since 2026-04-18
 */
@Data
public class RouteGroupVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组ID
     */
    private Integer groupId;

    /**
     * 分组标识
     */
    private String groupKey;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 绑定的实例数量
     * 用于判断分组状态：>0 表示已启用（有实例绑定），=0 表示未启用
     */
    private Integer instanceCount;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
