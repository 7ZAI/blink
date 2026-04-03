package com.blink.gateway.admin.dto.rsp;

import com.blink.gateway.admin.dto.vo.GatewayHealthStatusVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 网关健康状态响应DTO
 *
 * @author binblink
 */
@Data
public class GatewayHealthStatusRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 健康状态列表
     */
    private List<GatewayHealthStatusVO> healthStatusList;
}