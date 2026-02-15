package com.blink.base.dto.rsp;

import com.blink.base.entity.SysPermissionDO;
import lombok.Data;

import java.util.List;

/**
 * @Author binblink
 * @Date 2026/2/15
 */
@Data
public class GetAllApiPermissionsRsp {

    private List<SysPermissionDO> permissionList;
}
