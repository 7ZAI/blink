package com.blink.gateway.base.dto.rsp;



import com.blink.gateway.base.dto.vo.SysMenuVO;
import com.blink.gateway.base.dto.vo.SysUserVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * @author binblink
 */
@Data
public class SysLoginRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户信息
     */
    private SysUserVO userInfo;

    /**
     * 用户角色
     */
    private List<String> roles;

    /**
     * 角色ID列表
     */
    private List<Integer> roleIds;

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
    private Set<String> permissions;

    /**
     * 登入凭证
     */
    private String token;

    /**
     * 是否需要重置密码(首次登录)
     */
    private Boolean needResetPassword;


}