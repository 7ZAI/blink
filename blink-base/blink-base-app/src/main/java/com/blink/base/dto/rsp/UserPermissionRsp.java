package com.blink.base.dto.rsp;

import com.blink.base.dto.vo.SysMenuVO;
import com.blink.base.dto.vo.SysPermissionVO;
import com.blink.base.dto.vo.SysRoleVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户权限信息响应DTO
 *
 * @author binblink
 */
@Data
public class UserPermissionRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户拥有的角色列表
     */
    private List<SysRoleVO> roles;

    /**
     * 用户拥有的菜单列表（树形结构）
     */
    private List<SysMenuVO> menus;

    /**
     * 用户拥有的权限列表
     */
    private List<SysPermissionVO> permissions;
}