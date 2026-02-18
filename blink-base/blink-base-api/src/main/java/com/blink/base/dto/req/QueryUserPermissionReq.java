package com.blink.base.dto.req;

import com.blink.base.constans.BaseErrCodeConstant;
import com.blink.framework.validate.annotation.DataDict;
import com.blink.framework.validate.annotation.MutuallyExclusive;
import lombok.Data;

/**
 * 根据userid获取用户权限 请求参数
 *
 * @Author binblink
 * @Date 2026/2/14
 */
@Data
@MutuallyExclusive(field1 = "userId",field2 = "url")
public class QueryUserPermissionReq {

    @DataDict(name="systemId",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer userId;

    private String url;
}
