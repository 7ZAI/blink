package com.blink.gateway.base.dto.rsp;

import com.blink.gateway.base.dto.vo.EntityFieldVO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 实体字段列表响应DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class EntityFieldsRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字段列表
     */
    private List<EntityFieldVO> fields;
}