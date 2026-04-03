package com.blink.framework.common.data;

import java.io.Serial;

/**
 * 空body对象
 * 用于不需要请求参数的接口
 *
 * @author binblink
 */
public class EmptyBody implements java.io.Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public EmptyBody() {
    }
}
