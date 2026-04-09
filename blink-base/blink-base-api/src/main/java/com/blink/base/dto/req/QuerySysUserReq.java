package com.blink.base.dto.req;

import com.blink.base.constans.BaseErrCodeConstant;
import com.blink.framework.common.data.PageDTO;
import com.blink.framework.validate.annotation.FieldConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * QuerySysUserReqDTO  查询系统用户请求参数对象
 * @author binblink
 */
@Getter
@Setter
@ToString
public class QuerySysUserReq extends PageDTO implements Serializable   {

    @Serial
    private static final long serialVersionUID = -951055160080394698L;

    /**
     * 登录名
     */
    @FieldConstraint(name="loginName",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE )
    private String loginName;

    /**
     * 昵称
     */
    @FieldConstraint(name="loginName",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String username;

    /**
     * 性别 1男 2女 3不确定
     */
    @FieldConstraint(name="flag1",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer sex;

    /**
     * 起始日期
     */
    @FieldConstraint(name="date",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE )
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @FieldConstraint(name="date",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE )
    private LocalDate endDate;

    /**
     * 组Id
     */
    @FieldConstraint(name="systemId",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE )
    private List<Integer> groupId;

}
