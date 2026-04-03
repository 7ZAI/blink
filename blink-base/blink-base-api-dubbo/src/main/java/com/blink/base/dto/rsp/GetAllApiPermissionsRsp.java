package com.blink.base.dto.rsp;

import com.blink.base.dto.vo.SysPermissionVO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author binblink
 * @Date 2026/2/15
 */
@Data
public class GetAllApiPermissionsRsp implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<SysPermissionVO> permissionList;
}
