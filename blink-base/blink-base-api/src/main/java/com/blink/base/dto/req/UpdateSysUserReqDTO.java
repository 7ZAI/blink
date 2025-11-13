package com.blink.base.dto.req;

import com.blink.base.dto.constant.BaseAppConstant;
import com.blink.framework.validate.annotation.DataDict;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * UpdateSysUserReqDTO 修改系统用户信息请求参数
 */
@Data
public class UpdateSysUserReqDTO implements Serializable {

    private static final long serialVersionUID = -1646188349760643295L;

    /**
     * 修改的用户Id
     */
    @NotNull(message = BaseAppConstant.PARAMETER_NOT_NULL)
    @DataDict(name="systemId",message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private Integer userId;


    /**
     * 昵称
     */
    private String username;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 性别 1男 2女 3不确定
     */
    @NotNull
    private Integer sex;

    /**
     * 电话
     */
    @NotBlank
    @DataDict(name="phone",message = BaseAppConstant.PARAMETER_OUT_RANGE)
    private String phone;

    /**
     * 邮箱
     */
    @Email
    private String email;


    /**
     * 组 Id
     */
    private List<Integer> groupIdList;


    /**
     * 角色 Id
     */
    private List<Integer> roleIdList;
}
