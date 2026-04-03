package com.blink.gateway.base.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author binblink
 * @Date 2026/2/15
 */
@Data
public class QueryPermissionIdentityReq {

    @NotBlank
    private String url;
}
