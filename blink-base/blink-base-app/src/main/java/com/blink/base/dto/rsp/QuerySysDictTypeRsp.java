package com.blink.base.dto.rsp;

import com.blink.base.entity.SysDictTypeDO;
import com.blink.framework.common.data.PageDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 查询字典类型表响应参数对象
 *
 * @author blink
 * @since 2025-03-07
 */
@Getter
@Setter
@ToString
public class QuerySysDictTypeRsp extends PageDTO<SysDictTypeDO> {

}
