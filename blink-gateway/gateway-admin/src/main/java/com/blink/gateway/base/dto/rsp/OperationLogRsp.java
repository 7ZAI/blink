package com.blink.gateway.base.dto.rsp;

import com.blink.gateway.base.dto.vo.OperationLogVO;
import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 操作日志列表响应DTO
 *
 * @author binblink
 * @since 2024-03-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLogRsp extends PageDTO<OperationLogVO> implements Serializable {

    private static final long serialVersionUID = 1L;

}
