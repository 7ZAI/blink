package com.blink.base.dto.req;

import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.framework.validate.annotation.FieldConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * <p>
 * UpdateSysGroupReqDTO 更新组请求参数对象
 * </p>
 *
 * @author binblink
 * @since 2024-01-04
 */
@Data
public class UpdateSysGroupReq implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;


    /**
     * 分组id
     */
    @NotNull
    @FieldConstraint(name="systemId",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer groupId;


  /**
   * 分组编号
   */
  private String groupNo;



    /**
     * 组名称
     */
    @NotNull
    @FieldConstraint(name="systemName",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String groupName;


    /**
     * 组英文名称
     */
    @FieldConstraint(name="systemEnName",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String groupEnName;


    /**
     * 父组id
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    @FieldConstraint(name="systemId",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer groupParentId;


    /**
     * 层级
     */
    @FieldConstraint(name="number",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer groupLevel;


    /**
     * 是否叶子节点 0否 1是
     */
    @NotNull
    @FieldConstraint(name="flag1",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer isLeaf;


    /**
     * 组领导
     */
    @FieldConstraint(name="systemName",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String groupLeader;


    /**
     * 组地址
     */
    @FieldConstraint(name="address",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String groupAddress;


    /**
     * 组电话
     */
    @FieldConstraint(name="phone",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String phone;




}
