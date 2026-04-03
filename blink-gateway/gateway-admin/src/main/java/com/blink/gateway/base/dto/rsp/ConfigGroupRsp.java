package com.blink.gateway.base.dto.rsp;

import com.blink.gateway.base.dto.vo.SysConfigVO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分组配置响应
 *
 * @author blink
 * @since 2025-03-06
 */
@Getter
@Setter
public class ConfigGroupRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组ID
     */
    private Integer groupId;

    /**
     * 分组键名
     */
    private String groupKey;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 配置项列表
     */
    private List<SysConfigVO> configs;
}
