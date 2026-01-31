package com.blink.base.dto.req;

import com.blink.base.constans.BaseErrCodeConstant;
import com.blink.base.entity.RouteDefinitionDO;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * @Author binblink
 */
@Data
public class AddRoutesReqDTO {

    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String routesGroup;

    private List<RouteDefinitionDO> routes;
}
