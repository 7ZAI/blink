package com.blink.base.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Dashboard 统计数据响应
 *
 * @author binblink
 */
@Data
public class DashboardRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 总用户数
     */
    private Integer totalUsers;

    /**
     * 在线用户数
     */
    private Integer onlineUsers;

    /**
     * 总角色数
     */
    private Integer totalRoles;

    /**
     * 总菜单数
     */
    private Integer totalMenus;
}