package com.blink.gateway.admin.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import com.blink.gateway.admin.dto.vo.RoutePushStatusVO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 查询推送状态响应DTO
 *
 * @author binblink
 * @since 2026-04-12
 */
@Getter
@Setter
public class QueryPushStatusRsp extends PageDTO<RoutePushStatusVO> {

    @Serial
    private static final long serialVersionUID = 1L;
}