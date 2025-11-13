package com.blink.base.dto.req;

import com.blink.base.dto.constant.BaseAppConstant;
import com.blink.framework.validate.annotation.DataDict;
import lombok.Data;
import java.io.Serializable;
import com.blink.framework.common.data.PageDTO;

/**
 * <p>
 * QuerySysMenuReqDTO 查询列表系统菜单请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2024-01-05
 */
@Data
public class QuerySysMenuReqDTO extends PageDTO implements Serializable {

  private static final long serialVersionUID = 1L;


  /**
   * 菜单名称
   */
  @DataDict(name = "systemName", message = BaseAppConstant.PARAMETER_OUT_RANGE)
  private String menuName;


  /**
   * 菜单英文名称
   */
  @DataDict(name = "systemEnName", message = BaseAppConstant.PARAMETER_OUT_RANGE)
  private String menuEnName;


  /**
   * 菜单类型
   */
  @DataDict(name = "flag1", message = BaseAppConstant.PARAMETER_OUT_RANGE)
  private Integer type;




}
