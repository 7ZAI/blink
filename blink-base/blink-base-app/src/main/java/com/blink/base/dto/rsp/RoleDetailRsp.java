package com.blink.base.dto.rsp;

import com.blink.base.dto.vo.SysMenuVO;
import com.blink.base.dto.vo.SysPermissionVO;
import com.blink.base.dto.vo.SysRoleVO;
import com.blink.base.dto.vo.SysUserVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 角色详情响应DTO
 *
 * @author binblink
 */
@Data
public class RoleDetailRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色基本信息
     */
    private SysRoleVO roleInfo;

    /**
     * 已授权的权限列表
     */
    private List<SysPermissionVO> permissions;

    /**
     * 已分配的菜单列表
     */
    private List<SysMenuVO> menus;

    /**
     * 拥有该角色的用户列表
     */
    private List<SysUserVO> users;
}
