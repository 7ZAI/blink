package com.blink.datasource.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器
 * 自动填充 createTime、updateTime 等常用字段
 *
 * @author binblink
 */
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 创建时间字段名
     */
    private static final String CREATE_TIME = "createTime";

    /**
     * 更新时间字段名
     */
    private static final String UPDATE_TIME = "updateTime";

    /**
     * 创建人字段名
     */
    private static final String CREATE_BY = "createBy";

    /**
     * 更新人字段名
     */
    private static final String UPDATE_BY = "updateBy";

    /**
     * 插入时自动填充
     *
     * @param metaObject 元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始插入填充...");

        LocalDateTime now = LocalDateTime.now();

        // 填充创建时间
        this.strictInsertFill(metaObject, CREATE_TIME, LocalDateTime.class, now);

        // 填充更新时间
        this.strictInsertFill(metaObject, UPDATE_TIME, LocalDateTime.class, now);



        // 填充创建人（如果字段存在且有值则不覆盖）
        // 注意：需要配合 @TableField(fill = FieldFill.INSERT) 注解使用
        // 实际使用时可以从安全上下文获取当前用户
         String currentUser = getCurrentUser();
        this.strictInsertFill(metaObject, CREATE_BY, String.class, currentUser);
    }

    /**
     * 更新时自动填充
     *
     * @param metaObject 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始更新填充...");

        LocalDateTime now = LocalDateTime.now();

        // 填充更新时间
        this.strictUpdateFill(metaObject, UPDATE_TIME, LocalDateTime.class, now);

        // 填充更新人（如果字段存在）
         String currentUser = getCurrentUser();
        this.strictUpdateFill(metaObject, UPDATE_BY, String.class, currentUser);
    }

    /**
     * 获取当前用户（可根据实际项目安全框架实现）
     *
     * @return 当前用户登录名
     */
     private String getCurrentUser() {
         String currentUser = "";
         try {
             // 从 Spring Security 或其他安全上下文获取
             // return SecurityContextHolder.getContext().getAuthentication().getName();
              currentUser = BlinkRequestContextHolder.getLoginName();
         } catch (Exception e) {
             log.error("BlinkRequestContextHolder获取登入名失败！{}",e.getMessage(),e);
             return currentUser;
         }
         return currentUser;

     }
}