package com.blink.gateway.admin.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import com.blink.gateway.admin.entity.GaChannelDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询渠道列表响应
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
public class QueryChannelRsp extends PageDTO<GaChannelDO> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
