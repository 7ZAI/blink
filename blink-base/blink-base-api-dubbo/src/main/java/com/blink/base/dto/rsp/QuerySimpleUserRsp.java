package com.blink.base.dto.rsp;

import com.blink.base.dto.vo.SimpleUserVO;
import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 简化用户列表响应
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuerySimpleUserRsp extends PageDTO<SimpleUserVO> {

    @Serial
    private static final long serialVersionUID = 1L;
}