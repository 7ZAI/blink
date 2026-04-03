package com.blink.base.dto.req;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 获取实体字段请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class GetEntityFieldsReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 实体类全限定名
     */
    @NotBlank(message = "实体类不能为空")
    private String entityClass;
}