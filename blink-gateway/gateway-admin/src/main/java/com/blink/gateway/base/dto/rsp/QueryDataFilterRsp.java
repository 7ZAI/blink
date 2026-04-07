package com.blink.gateway.base.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import com.blink.gateway.base.dto.vo.DataFilterVO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 数据过滤规则响应DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class QueryDataFilterRsp extends PageDTO<DataFilterVO> {
}
