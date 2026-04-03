package com.blink.base.dto.rsp;

import com.blink.base.entity.SysDictDataDO;
import com.blink.framework.common.data.PageDTO;

import java.io.Serial;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * QuerySysDictDataRsp 查询字典数据列表响应参数对象
 * </p>
 *
 * @author blink
 * @since 2026-03-07
 */
@Getter
@Setter
@ToString
public class QuerySysDictDataRsp extends PageDTO<SysDictDataDO> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
