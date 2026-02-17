package com.blink.base.dto.rsp;

import com.blink.base.dto.vo.SysPermissionVO;
import lombok.Data;

import java.util.List;

/**
 * @Author binblink
 * @Date 2026/2/15
 */
@Data
public class GetAllApiPermissionsRsp {

    private List<SysPermissionVO> permissionList;
}
