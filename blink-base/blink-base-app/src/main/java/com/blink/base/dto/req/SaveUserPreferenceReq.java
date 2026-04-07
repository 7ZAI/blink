package com.blink.base.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * SaveUserPreferenceReq 保存用户偏好设置请求参数
 *
 * @author binblink
 */
@Data
public class SaveUserPreferenceReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主题: light/dark/auto
     */
    private String theme;

    /**
     * 语言: zh_cn/en_us
     */
    private String language;

    /**
     * 侧边栏收起: false否 true是
     */
    private Boolean sidebarCollapsed;

    /**
     * 字体大小
     */
    @Min(value = 12, message = "字体大小最小为12")
    @Max(value = 18, message = "字体大小最大为18")
    private Integer fontSize;

}
