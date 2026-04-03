package com.blink.gateway.admin.dto.rsp;

import com.blink.gateway.admin.dto.vo.ConfigHistoryVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 配置历史响应DTO
 *
 * @author binblink
 */
@Data
public class ConfigHistoryRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 总数
     */
    private Integer total;

    /**
     * 配置历史列表
     */
    private List<ConfigHistoryVO> history;
}