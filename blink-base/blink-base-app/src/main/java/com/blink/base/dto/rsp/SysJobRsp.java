package com.blink.base.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务列表响应
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysJobRsp extends PageDTO<SysJobVO> {
}
