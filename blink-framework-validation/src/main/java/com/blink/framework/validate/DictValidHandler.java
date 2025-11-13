package com.blink.framework.validate;


import com.blink.framework.validate.checker.DecimalValidChecker;
import com.blink.framework.validate.checker.DictValidChecker;
import com.blink.framework.validate.checker.GeneralValidChecker;
import com.blink.framework.validate.constant.DictType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author binblink
 */
public class DictValidHandler {

    /**
     *  本地缓存
     */
    private static final Map<String, DictValidChecker> checkerMap = new HashMap<>(16);


    static{
        GeneralValidChecker generalValidChecker = new GeneralValidChecker();
        checkerMap.put(DictType.DECIMAL.getType(),new DecimalValidChecker());
        checkerMap.put(DictType.STRING.getType(),generalValidChecker);
        checkerMap.put(DictType.NUMBER.getType(),generalValidChecker);
    }

    public static boolean check(DictCacheDO dict, Object value){

        if(Objects.isNull(dict) || Objects.isNull(value)){
            return true;
        }

        DictValidChecker checker = checkerMap.get(dict.getDataType());

        if(Objects.isNull(checker)){
            return false;
        }

        return checker.check(dict,value);
    }
}
