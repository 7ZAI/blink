package com.blink.gateway.base.dto.rsp;

import com.blink.framework.common.data.PageDTO;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程实例分页响应DTO
 *
 * @author binblink
 */
public class ProcessInstanceRsp extends PageDTO<ProcessInstanceVO> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}