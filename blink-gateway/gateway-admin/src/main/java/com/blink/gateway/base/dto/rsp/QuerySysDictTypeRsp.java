package com.blink.gateway.base.dto.rsp;

import com.blink.gateway.base.entity.SysDictTypeDO;
import com.blink.framework.common.data.PageDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询字典类型表响应参数对象
 *
 * @author blink
 * @since 2025-03-07
 */
@Getter
@Setter
@ToString
public class QuerySysDictTypeRsp extends PageDTO<SysDictTypeDO> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
