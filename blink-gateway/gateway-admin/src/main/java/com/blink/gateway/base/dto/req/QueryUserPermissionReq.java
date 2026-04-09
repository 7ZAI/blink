package com.blink.gateway.base.dto.req;

import com.blink.base.constans.DubboErrCodeConstant;
import com.blink.framework.validate.annotation.FieldConstraint;
import com.blink.framework.validate.annotation.MutuallyExclusive;
import lombok.Data;

import java.io.Serializable;

/**
 * 根据userid获取用户权限 请求参数
 *
 * @Author binblink
 * @Date 2026/2/14
 */
@Data
@MutuallyExclusive(field1 = "userId",field2 = "url")
public class QueryUserPermissionReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @FieldConstraint(name="systemId",message = DubboErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer userId;

    private String url;
}
