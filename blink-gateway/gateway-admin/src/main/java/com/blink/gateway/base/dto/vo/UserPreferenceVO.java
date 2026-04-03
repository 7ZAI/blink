package com.blink.gateway.base.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * UserPreferenceVO 用户偏好设置视图对象
 *
 * @author binblink
 */
@Data
public class UserPreferenceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 偏好ID
     */
    private Integer preferenceId;

    /**
     * 用户ID
     */
    private Integer userId;

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
    private Integer fontSize;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
