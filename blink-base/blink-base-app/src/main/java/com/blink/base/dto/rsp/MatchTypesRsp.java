package com.blink.base.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 匹配类型选项响应
 *
 * @author binblink
 */
@Data
public class MatchTypesRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 匹配类型选项列表
     */
    private List<MatchTypeOption> options;

    /**
     * 匹配类型选项
     */
    @Data
    public static class MatchTypeOption implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 匹配类型值（如 CURRENT_USER）
         */
        private String value;

        /**
         * 匹配类型描述（如"当前用户"）
         */
        private String label;

        /**
         * 是否为动态类型（不需要选择具体值）
         */
        private Boolean dynamic;

        public MatchTypeOption() {
        }

        public MatchTypeOption(String value, String label, Boolean dynamic) {
            this.value = value;
            this.label = label;
            this.dynamic = dynamic;
        }
    }
}