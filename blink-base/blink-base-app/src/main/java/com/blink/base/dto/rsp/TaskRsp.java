package com.blink.base.dto.rsp;

import com.blink.framework.common.data.PageDTO;

import java.io.Serial;
import java.io.Serializable;

/**
 * 待办任务分页响应DTO
 *
 * @author binblink
 */
public class TaskRsp extends PageDTO<TaskVO> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}