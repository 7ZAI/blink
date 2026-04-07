package com.blink.base.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


/**
 * <p>
 * QuerySysRoleReqDTO 查询列表系统角色请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2024-01-03
 */
@Getter
@Setter
public class QuerySysRoleReq extends Page {



    /**
     * 角色名称
     */
    private String roleName;


    /**
     * 角色状态
     */
    private Byte status;


    /**
     * 角色代码
     */
    private String roleCode;

    /**
     * 角色类型
     */
    private Byte roleType;


    /**
     * 创建者
     */
    private String createBy;


    /**
     * 创建时间
     */
    private LocalDateTime createTime;




}
