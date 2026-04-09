package com.blink.base.dto.req;

import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.framework.validate.annotation.FieldConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * <p>
 * QuerySysMenuReqDTO 查询列表系统菜单请求参数对象
 * </p>
 * 菜单为树形结构展示，不需要分页
 *
 * @author binblink
 * @since 2024-01-05
 */
@Getter
@Setter
@ToString
public class QuerySysMenuReq implements Serializable {

  private static final long serialVersionUID = 1L;


  /**
   * 菜单名称
   */
  @FieldConstraint(name = "systemName", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
  private String menuName;


  /**
   * 菜单英文名称
   */
  @FieldConstraint(name = "systemEnName", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
  private String menuEnName;


  /**
   * 菜单类型
   */
  @FieldConstraint(name = "flag1", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
  private Integer type;

}
