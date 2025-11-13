package com.blink.framework.validate.checker;


import com.blink.framework.validate.DictCacheDO;

public interface DictValidChecker {

    boolean check(DictCacheDO dict, Object value);
}
