package com.blink.base.dto.req;

import com.blink.base.dto.constant.BaseAppConstant;

import com.blink.framework.common.data.PageDTO;
import com.blink.framework.validate.annotation.DataDict;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * QuerySysUserReqDTO  查询系统用户请求参数对象
 */
@Data
public class QuerySysUserReqDTO extends PageDTO implements Serializable   {

    private static final long serialVersionUID = -951055160080394698L;

    /**
     * 登录名
     */
    @DataDict(name="loginName",message = BaseAppConstant.PARAMETER_OUT_RANGE )
    private String loginName;

    /**
     * 昵称
     */
    @DataDict(name="loginName",message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private String username;

    /**
     * 性别 1男 2女 3不确定
     */
    @DataDict(name="flag1",message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private Integer sex;

    /**
     * 起始日期
     */
    @DataDict(name="date",message = BaseAppConstant.PARAMETER_OUT_RANGE )
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @DataDict(name="date",message = BaseAppConstant.PARAMETER_OUT_RANGE )
    private LocalDate endDate;

    /**
     * 组Id
     */
    @DataDict(name="systemId",message = BaseAppConstant.PARAMETER_OUT_RANGE )
    private List<Integer> groupId;

}
