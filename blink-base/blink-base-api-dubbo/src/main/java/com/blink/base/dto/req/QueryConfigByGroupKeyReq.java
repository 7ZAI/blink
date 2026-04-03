package com.blink.base.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 按分组键查询配置请求
 *
 * @author blink
 * @since 2025-03-06
 */
@Getter
@Setter
public class QueryConfigByGroupKeyReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分组键名
     */
    @NotBlank(message = "分组键名不能为空")
    private String groupKey;
}
