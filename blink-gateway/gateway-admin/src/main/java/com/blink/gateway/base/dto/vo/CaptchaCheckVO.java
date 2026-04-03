package com.blink.gateway.base.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * CaptchaCheckVO 验证码校验结果VO
 *
 * @author binblink
 */
@Data
public class CaptchaCheckVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 校验结果 true-成功 false-失败
     */
    private Boolean result;

    /**
     * 校验结果提示信息
     */
    private String msg;

    /**
     * 验证码ID
     */
    private String captchaId;

    /**
     * 后台二次校验参数
     */
    private String captchaVerification;

    /**
     * 是否启用二次校验
     */
    private Boolean verification;

    public CaptchaCheckVO() {
    }

    public CaptchaCheckVO(Boolean result, String msg) {
        this.result = result;
        this.msg = msg;
    }

    public static CaptchaCheckVO success(String captchaId, String captchaVerification) {
        CaptchaCheckVO vo = new CaptchaCheckVO();
        vo.setResult(true);
        vo.setMsg("校验成功");
        vo.setCaptchaId(captchaId);
        vo.setCaptchaVerification(captchaVerification);
        return vo;
    }

    public static CaptchaCheckVO fail(String msg) {
        CaptchaCheckVO vo = new CaptchaCheckVO();
        vo.setResult(false);
        vo.setMsg(msg);
        return vo;
    }
}
