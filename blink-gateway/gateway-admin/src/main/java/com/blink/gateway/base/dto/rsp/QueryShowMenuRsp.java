package com.blink.gateway.base.dto.rsp;

import com.blink.gateway.base.dto.vo.SysMenuVO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class QueryShowMenuRsp implements Serializable {

    /**
     * 菜单展示
     */
    private List<SysMenuVO> menus;

    /**
     * 功能展示
     */
    private List<SysMenuVO> functionMenu;
}
