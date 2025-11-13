package com.blink.base.dto.req;

import com.blink.base.dto.constant.BaseAppConstant;
import com.blink.framework.validate.annotation.DataDict;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.ScriptAssert;

import java.io.Serializable;


/**
 * AddSysUserReqDTO 新增系统用户请求参数对象
 */
@ScriptAssert(script = "_this.password == _this.confirmPassword",lang="javascript",message = BaseAppConstant.PASSWORD_CONFIRM_ERR )
@Data
public class AddSysUserReqDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 登录名
     */
    @NotNull(message = BaseAppConstant.PARAMETER_NOT_NULL)
    @DataDict(name="loginName",message = BaseAppConstant.PARAMETER_OUT_RANGE )
    private String loginName;

    /**
     * 密码
     */
    @NotBlank(message = BaseAppConstant.PARAMETER_NOT_NULL)
    @DataDict(name="password",message = BaseAppConstant.PASSWORD_FORMAT_ERR)
    private String password;

    /**
     * 确认密码
     */
    @NotBlank(message = BaseAppConstant.PARAMETER_NOT_NULL)
    @DataDict(name="password",message = BaseAppConstant.PASSWORD_FORMAT_ERR)
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
    @NotNull(message = BaseAppConstant.PARAMETER_NOT_NULL)
    private Integer sex;

    /**
     * 电话
     */
    @NotBlank(message = BaseAppConstant.PARAMETER_NOT_NULL)
    private String phone;

    /**
     * 邮箱
     */
    @Email(message = BaseAppConstant.EMAIL_FORMAT_ERR)
    private String email;


    public static boolean confirmPassword(String password,String confirmPassword){


        return password.equals(confirmPassword);
    }

}
