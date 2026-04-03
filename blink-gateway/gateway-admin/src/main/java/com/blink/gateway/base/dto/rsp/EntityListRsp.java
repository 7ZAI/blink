package com.blink.gateway.base.dto.rsp;

import com.blink.datasource.data.RegisteredEntityVO;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 已注册实体列表响应
 *
 * @author binblink
 */
@Data
public class EntityListRsp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 已注册实体列表
     */
    private List<RegisteredEntityVO> entities = new ArrayList<>();
}