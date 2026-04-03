package com.blink.gateway.base.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * GetCaptchaReq 获取验证码请求参数
 *
 * @author binblink
 */
@Data
public class GetCaptchaReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 验证码类型:(clickWord-点选文字, blockPuzzle-滑块拼图)
     */
    @NotBlank(message = "验证码类型不能为空")
    private String captchaType;

    /**
     * 客户端UI组件id,组件初始化时设置一次，UUID
     */
    private String clientUid;

    /**
     * 客户端的请求时间戳
     */
    private Long ts;
}
