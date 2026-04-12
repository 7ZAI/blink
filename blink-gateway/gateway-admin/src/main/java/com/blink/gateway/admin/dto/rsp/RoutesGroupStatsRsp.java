package com.blink.gateway.admin.dto.rsp;

import com.blink.gateway.admin.dto.vo.RoutesGroupStatsVO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 路由分组统计响应DTO
 *
 * @author binblink
 * @since 2026-04-12
 */
@Getter
@Setter
public class RoutesGroupStatsRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组统计列表
     */
    private List<RoutesGroupStatsVO> groups;
}