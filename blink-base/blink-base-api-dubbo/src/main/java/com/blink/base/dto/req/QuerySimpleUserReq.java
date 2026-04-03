package com.blink.base.dto.req;

import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 简化用户查询请求（用于弹窗选择）
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuerySimpleUserReq extends PageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 搜索关键字（模糊匹配 loginName/username）
     */
    private String keyword;
}