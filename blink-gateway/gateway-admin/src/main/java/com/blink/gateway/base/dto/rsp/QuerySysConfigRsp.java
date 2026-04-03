
package com.blink.gateway.base.dto.rsp;

import com.blink.gateway.base.entity.SysConfigDO;

import java.io.Serializable;
import com.blink.framework.common.data.PageDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * QuerySysConfigRspDTO 新增参数配置表请求参数对象
 * </p>
 *
 * @author blink
 */
@Getter
@Setter
@ToString
public class QuerySysConfigRsp extends PageDTO<SysConfigDO> implements Serializable {

  private static final long serialVersionUID = 1L;


  
}
