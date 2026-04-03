package com.blink.base.dto.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 批量获取字典数据请求参数对象
 *
 * @author binblink
 * @since 2026-03-21
 */
@Data
public class GetDictDataByTypesReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典类型编码列表
     */
    @NotEmpty(message = "字典类型编码列表不能为空")
    private List<String> dictTypes;
}