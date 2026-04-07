package com.blink.base.dto.rsp;

import com.blink.base.dto.vo.OperationLogVO;
import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志列表响应DTO
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLogRsp extends PageDTO<OperationLogVO> {

}
