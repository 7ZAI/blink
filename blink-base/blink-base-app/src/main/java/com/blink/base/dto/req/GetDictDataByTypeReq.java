package com.blink.base.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 根据字典类型编码获取字典数据请求参数对象
 *
 * @author binblink
 * @since 2026-03-21
 */
@Data
public class GetDictDataByTypeReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典类型编码
     */
    @NotBlank(message = "字典类型编码不能为空")
    private String dictType;
}