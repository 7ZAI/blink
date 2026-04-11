package com.blink.gateway.admin.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import com.blink.gateway.admin.dto.vo.InstanceInfoVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分页查询实例列表响应
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryInstanceListRsp extends PageDTO<InstanceInfoVO> {
}