package com.blink.gateway.base.dto.rsp;

import com.blink.framework.common.data.PageDTO;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程定义分页响应DTO
 *
 * @author binblink
 */
public class ProcessDefinitionRsp extends PageDTO<ProcessDefinitionVO> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}