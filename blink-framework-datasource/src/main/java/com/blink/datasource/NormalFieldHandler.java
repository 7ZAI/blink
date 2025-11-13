package com.blink.datasource;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 处理公共字段 mybatis plus 提供的方式
 * DO类字段上添加 @TableField(value = "create_time", fill = FieldFill.INSERT)
 */
//@Component
public class NormalFieldHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime dateTime = LocalDateTime.now();
        this.setFieldValByName("updateTime",dateTime ,metaObject);
        this.setFieldValByName("createTime",dateTime,metaObject);
        this.setFieldValByName("createBy","",metaObject);
        this.setFieldValByName("updateBy","",metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("updateTime", LocalDateTime.now(),metaObject);
        this.setFieldValByName("updateBy","",metaObject);
    }
}
