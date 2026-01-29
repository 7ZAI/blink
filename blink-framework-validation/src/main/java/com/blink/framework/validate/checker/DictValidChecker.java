package com.blink.framework.validate.checker;


import com.blink.framework.common.data.DictCacheDO;

public interface DictValidChecker {

    boolean check(DictCacheDO dict, Object value);
}
