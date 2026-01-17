package com.blink.base.dto.req;

import com.blink.base.constans.BaseErrCodeConstant;
import com.blink.framework.validate.annotation.DataDict;
import com.blink.framework.validate.annotation.SameValue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * AddSysUserReqDTO 新增系统用户请求参数对象
 *
 * @author binblink
 */
@SameValue(fields = {"password","confirmPassword"},message = BaseErrCodeConstant.PASSWORD_CONFIRM_ERR)
@Data
public class AddSysUserReqDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 登录名
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    @DataDict(name="loginName",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE )
    private String loginName;

    /**
     * 密码
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    @DataDict(name="password",message = BaseErrCodeConstant.PASSWORD_FORMAT_ERR)
    private String password;

    /**
     * 确认密码
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    @DataDict(name="password",message = BaseErrCodeConstant.PASSWORD_FORMAT_ERR)
    private String confirmPassword;

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
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private Integer sex;

    /**
     * 电话
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private String phone;

    /**
     * 邮箱
     */
    @Email(message = BaseErrCodeConstant.EMAIL_FORMAT_ERR)
    private String email;



}
