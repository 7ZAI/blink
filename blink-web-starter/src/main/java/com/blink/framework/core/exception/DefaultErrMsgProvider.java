package com.blink.framework.core.exception;

import cn.hutool.core.util.StrUtil;

/**
 * 默认错误信息提供者
 * 不依赖外部资源，根据错误码类型返回对应的默认消息
 *
 * @author binblink
 */
public class DefaultErrMsgProvider implements ErrMsgProvider {

    /**
     * 业务错误码前缀
     */
    private static final String BUSINESS_CODE_PREFIX = "BUSS";

    /**
     * 参数校验错误码前缀
     */
    private static final String INVALID_CODE_PREFIX = "INVALID";

    /**
     * 认证授权错误码前缀
     */
    private static final String AUTH_CODE_PREFIX = "AUTH";

    /**
     * 工作流错误码前缀
     */
    private static final String FLOW_CODE_PREFIX = "FLOW";

    /**
     * 中文默认业务错误消息
     */
    private static final String DEFAULT_BUSINESS_MSG_CN = "操作失败";

    /**
     * 英文默认业务错误消息
     */
    private static final String DEFAULT_BUSINESS_MSG_EN = "Operation failed";

    /**
     * 中文默认系统错误消息
     */
    private static final String DEFAULT_SYSTEM_MSG_CN = "系统错误，请稍后重试";

    /**
     * 英文默认系统错误消息
     */
    private static final String DEFAULT_SYSTEM_MSG_EN = "System error, please try again later";

    @Override
    public String getErrMsg(String msgCode, String lang) {
        boolean isChinese = "zh_cn".equalsIgnoreCase(lang);

        if (isBusinessErrorCode(msgCode)) {
            // 业务错误：显示错误码便于用户反馈
            if (isChinese) {
                return DEFAULT_BUSINESS_MSG_CN + "（错误码：" + msgCode + "）";
            } else {
                return DEFAULT_BUSINESS_MSG_EN + " (Error Code: " + msgCode + ")";
            }
        } else {
            // 系统错误：统一提示系统错误
            if (isChinese) {
                return DEFAULT_SYSTEM_MSG_CN;
            }
            return DEFAULT_SYSTEM_MSG_EN;
        }
    }

    /**
     * 判断是否为业务错误码
     * 业务错误码包括：BUSS、INVALID、AUTH、FLOW 开头的错误码
     *
     * @param msgCode 错误码
     * @return 是否为业务错误码
     */
    private boolean isBusinessErrorCode(String msgCode) {
        if (StrUtil.isBlank(msgCode)) {
            return false;
        }
        String upperCode = msgCode.toUpperCase();
        return upperCode.startsWith(BUSINESS_CODE_PREFIX)
                || upperCode.startsWith(INVALID_CODE_PREFIX)
                || upperCode.startsWith(AUTH_CODE_PREFIX)
                || upperCode.startsWith(FLOW_CODE_PREFIX);
    }
}