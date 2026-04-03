package com.blink.gateway.admin.dto.rsp;

import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 网关实例列表响应DTO
 *
 * @author binblink
 */
@Data
public class GatewayInstanceListRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例总数
     */
    private Integer total;

    /**
     * 实例列表
     */
    private List<GatewayInstanceVO> instances;
}