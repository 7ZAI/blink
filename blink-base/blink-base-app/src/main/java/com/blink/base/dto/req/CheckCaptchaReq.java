package com.blink.base.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * CheckCaptchaReq 校验验证码请求参数
 *
 * @author binblink
 */
@Data
public class CheckCaptchaReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 验证码id
     */
    @NotBlank(message = "验证码ID不能为空")
    private String captchaId;

    /**
     * 验证码类型:(clickWord-点选文字, blockPuzzle-滑块拼图)
     */
    @NotBlank(message = "验证码类型不能为空")
    private String captchaType;

    /**
     * 点坐标(base64加密传输)
     */
    @NotBlank(message = "验证数据不能为空")
    private String pointJson;

    /**
     * 客户端UI组件id
     */
    private String clientUid;

    /**
     * 客户端的请求时间戳
     */
    private Long ts;
}
