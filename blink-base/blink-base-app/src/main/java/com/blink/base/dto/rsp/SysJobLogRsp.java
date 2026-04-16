package com.blink.base.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务日志列表响应
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysJobLogRsp extends PageDTO<SysJobLogVO> {
}
