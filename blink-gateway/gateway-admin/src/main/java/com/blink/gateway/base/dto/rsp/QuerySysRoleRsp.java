package com.blink.gateway.base.dto.rsp;

import com.blink.gateway.base.dto.vo.SysRoleVO;
import com.blink.framework.common.data.PageDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * QuerySysRoleRspDTO 新增系统角色请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2024-01-03
 */
@Getter
@Setter
@ToString
public class QuerySysRoleRsp extends PageDTO<SysRoleVO> implements Serializable {

  private static final long serialVersionUID = 1L;


    /**
     * 角色id
     */
    private Integer roleId;


    /**
     * 角色名称
     */
    private String roleName;


    /**
     * 角色英文名称
     */
    private String roleEnName;


    /**
     * 角色状态
     */
    private Byte status;


    /**
     * 角色代码
     */
    private String roleCode;


    /**
     * 组id
     */
    private Integer groupId;


    /**
     * 角色类型
     */
    private Byte roleType;


    /**
     * 创建者
     */
    private String createBy;


    /**
     * 更新者
     */
    private String updateBy;


    /**
     * 创建时间
     */
    private LocalDateTime createTime;


    /**
     * 更新时间
     */
    private LocalDateTime updateTime;





}
