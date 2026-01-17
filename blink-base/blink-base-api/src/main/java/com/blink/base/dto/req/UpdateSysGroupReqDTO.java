package com.blink.base.dto.req;

import com.blink.base.constans.BaseErrCodeConstant;
import com.blink.framework.validate.annotation.DataDict;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

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
public class UpdateSysGroupReqDTO implements Serializable {

  private static final long serialVersionUID = 1L;


    /**
     * 分组id
     */
    @NotNull
    @DataDict(name="systemId",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer groupId;



    /**
     * 组名称
     */
    @NotNull
    @DataDict(name="systemName",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String groupName;


    /**
     * 组英文名称
     */
    @DataDict(name="systemEnName",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String groupEnName;


    /**
     * 父组id
     */
    @NotNull
    @DataDict(name="systemId",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer groupParentId;


    /**
     * 层级
     */
    @NotNull
    @DataDict(name="number",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer groupLevel;


    /**
     * 是否叶子节点 0否 1是
     */
    @NotNull
    @DataDict(name="flag1",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer isLeaf;


    /**
     * 组领导
     */
    @DataDict(name="systemName",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String groupLeader;


    /**
     * 组地址
     */
    @DataDict(name="address",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String groupAddress;


    /**
     * 组电话
     */
    @DataDict(name="phone",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String phone;




}
