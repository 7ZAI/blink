package com.blink.base.dto.req;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询我发起的流程请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
public class QueryMyProcessReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 状态（running-运行中, completed-已完成, all-全部）
     */
    private String status = "all";
}