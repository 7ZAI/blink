package com.blink.base.dto.req;

import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.framework.common.data.PageDTO;
import com.blink.framework.validate.annotation.DataDict;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

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
    @DataDict(name="loginName",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE )
    private String loginName;

    /**
     * 昵称
     */
    @DataDict(name="loginName",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String username;

    /**
     * 性别 1男 2女 3不确定
     */
    @DataDict(name="flag1",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer sex;

    /**
     * 起始日期
     */
    @DataDict(name="date",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE )
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @DataDict(name="date",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE )
    private LocalDate endDate;

    /**
     * 组Id（点击组织架构时传入，后端会自动查询该组织及所有子孙组织下的用户）
     */
    @DataDict(name="systemId",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE )
    private Integer groupId;

    /**
     * 组ID列表（用于查询多个组织下的用户，包含子孙组织）
     */
    private java.util.List<Integer> groupIdList;

    /**
     * 是否排除超级管理员（非超级管理员查询时排除超级管理员用户）
     */
    private Boolean excludeSuperAdmin;

}
