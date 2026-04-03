package com.blink.base.dto.rsp;

import com.blink.framework.common.data.PageDTO;

import java.io.Serial;
import java.io.Serializable;

/**
 * 已办任务分页响应DTO
 *
 * @author binblink
 */
public class HistoricTaskRsp extends PageDTO<HistoricTaskVO> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}