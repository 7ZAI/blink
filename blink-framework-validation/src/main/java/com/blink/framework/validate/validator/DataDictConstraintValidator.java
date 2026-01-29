package com.blink.framework.validate.validator;


import com.blink.framework.common.data.DictCacheDO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.ApplicationContextUtil;
import com.blink.framework.redis.component.CacheComponent;
import com.blink.framework.validate.DictValidHandler;
import com.blink.framework.validate.annotation.DataDict;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * 针对数据字典校验注解的校验规则类
 * @author binblink
 */
@Slf4j
public class DataDictConstraintValidator implements ConstraintValidator<DataDict, Object> {


    public static final String DICT_KEY_PREFIX = "system:dict:";
    private String dictName;

    @Override
    public void initialize(DataDict constraintAnnotation) {

        ConstraintValidator.super.initialize(constraintAnnotation);

        dictName = constraintAnnotation.name();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {

            if (Objects.isNull(value)) {
                return true;
            }

            CacheComponent cacheUtil = ApplicationContextUtil.getBean(CacheComponent.class);
            //从缓存获取获取
            DictCacheDO dictObject = (DictCacheDO) cacheUtil.getFromAllCache(DICT_KEY_PREFIX + dictName);

            //缓存获取失败
            if (Objects.isNull(dictObject)) {
                log.error("{} don't exist record ", DICT_KEY_PREFIX + dictName);
                throw new BlinkException("DataDict validate error!");
            }
            return DictValidHandler.check(dictObject, value);

        } catch (Exception e) {
            return false;
        }
    }
}

