package com.blink.gateway.admin.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import com.blink.gateway.admin.dto.vo.RouteGroupVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 路由分组列表响应
 *
 * @author binblink
 * @since 2026-04-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RouteGroupListRsp extends PageDTO<RouteGroupVO> {
}
