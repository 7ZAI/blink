package com.blink.datasource.annotation;

import java.lang.annotation.*;

/**
 * 数据范围关联关系注解
 * 用于标记关联表DO，声明该关联表连接的两个实体表
 * <p>
 * 关联关系是双向对称的，扫描时会为每个端点构建关联关系：
 * - endpointA 的实体可以选择关联到 endpointB
 * - endpointB 的实体可以选择关联到 endpointA
 * <p>
 * 匹配类型会根据目标实体自动推断：
 * - 目标是"用户" → CURRENT_USER, USER_LIST
 * - 目标是"部门" → CURRENT_DEPT, DEPT_LIST
 * - 目标是"角色" → CURRENT_ROLE, ROLE_LIST
 * <p>
 * 示例：sys_user_role_rela 连接 sys_user 和 sys_role
 * - sys_user 实体可以选择"角色关联"来过滤（匹配类型：CURRENT_ROLE, ROLE_LIST）
 * - sys_role 实体可以选择"用户关联"来过滤（匹配类型：CURRENT_USER, USER_LIST）
 *
 * @author binblink
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScopeRelation {

    /**
     * 关联表的一端
     */
    RelationEndpoint endpointA();

    /**
     * 关联表的另一端
     */
    RelationEndpoint endpointB();
}