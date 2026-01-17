
package com.blink.base.dto.rsp;

import com.blink.base.entity.SysConfigDO;
import com.blink.framework.common.data.PageDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * <p>
 * QuerySysConfigRspDTO 新增参数配置表请求参数对象
 * </p>
 *
 * @author blink
 * @since 2025-09-05
 */
@Getter
@Setter
@ToString
public class QuerySysConfigRspDTO extends PageDTO<SysConfigDO> implements Serializable {

  private static final long serialVersionUID = 1L;


  
}
