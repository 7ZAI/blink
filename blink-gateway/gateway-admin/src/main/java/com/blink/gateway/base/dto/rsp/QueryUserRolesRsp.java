package com.blink.gateway.base.dto.rsp;

import com.blink.gateway.base.dto.vo.SysRoleVO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class QueryUserRolesRsp implements Serializable {

    private List<SysRoleVO> roles;
}
