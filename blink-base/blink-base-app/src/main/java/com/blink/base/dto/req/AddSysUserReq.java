package com.blink.base.dto.req;

import com.blink.base.constants.BaseErrCodeConstant;

import com.blink.framework.validate.annotation.FieldConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;


/**
 * AddSysUserReqDTO 新增系统用户请求参数对象
 *
 * @author binblink
 */
@Data
public class AddSysUserReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 登录名
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    @FieldConstraint(name="loginName",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE )
    private String loginName;

    /**
     * 昵称
     */
    private String username;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 头像样式(DiceBear样式)
     */
    private String avatarStyle;

    /**
     * 性别 1男 2女 3不确定
     */
    @NotNull(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    private Integer sex;

    /**
     * 电话
     */
    @NotBlank(message = BaseErrCodeConstant.PARAMETER_NOT_NULL)
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String phone;

    /**
     * 邮箱
     */
    @Email(message = BaseErrCodeConstant.EMAIL_FORMAT_ERR)
    private String email;

    /**
     * 角色id
     */
    private List<Integer> roles;

    /**
     * 组织id
     */
    private Integer groupId;
}
