package com.blink.gateway.base.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 基础系统配置响应
 *
 * @author blink
 */
@Data
public class LoginConfigRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
 * 是否开启登录验证码
     */
    private Boolean captchaEnabled;

    /**
     * 验证码类型: clickWord(点选文字) / blockPuzzle(滑块拼图)
     */
    private String captchaType;

    /**
     * 系统标题
     */
    private String systemTitle;

    /**
     * 系统Logo (SVG或HTML代码)
     */
    private String systemLogo;

    /**
     * 页脚信息
     */
    private String systemFooter;

    /**
     * 用户默认头像
     */
    private String defaultAvatar;
}
