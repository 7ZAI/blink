package com.blink.gateway.trafficControl;

import com.blink.framework.common.exception.BlinkException;

/**
 * 自定义流量控制异常
 * @Author binblink
 */
public class RateLimitExceededException extends BlinkException {


    public RateLimitExceededException(String code) {
        super(code);

    }


}
