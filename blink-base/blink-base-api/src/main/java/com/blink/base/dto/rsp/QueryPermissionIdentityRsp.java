package com.blink.base.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author binblink
 * @Date 2025/8/24
 */
@Data
public class QueryPermissionIdentityRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 4121061350500172869L;
    /**
     * 权限标识
     */
    private String acIdentity;
}
