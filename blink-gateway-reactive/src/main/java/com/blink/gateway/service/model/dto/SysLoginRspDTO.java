package com.blink.gateway.service.model.dto;

import com.blink.gateway.service.model.vo.SysMenuVO;
import com.blink.gateway.service.model.vo.SysRoleVO;
import com.blink.gateway.service.model.vo.SysUserVO;
import lombok.Data;

import java.util.List;

@Data
public class SysLoginRspDTO {

    /**
     * 用户信息
     */
    private SysUserVO userInfo;

    /**
     * 用户角色
     */
    private List<SysRoleVO> roles;

    /**
     * 菜单展示
     */
    private List<SysMenuVO> menus;

    /**
     * 功能展示
     */
    private List<SysMenuVO> functionMenu;

    /**
     * 权限标签
     */
    private List<String> permissions;

    /**
     * 登入凭证
     */
    private String token;


}
