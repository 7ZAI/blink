package com.blink.base.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 数据过滤ID请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class DataFilterIdReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据过滤ID
     */
    @NotNull(message = "数据过滤ID不能为空")
    private Integer dataFilterId;
}