package com.blink.datasource;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blink.datasource.annotation.DataScopeEntity;
import com.blink.datasource.annotation.DataScopeRelation;
import com.blink.datasource.annotation.RelationEndpoint;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测试用实体类
 * 用于集成测试
 *
 * @author binblink
 * @since 2026-04-12
 */
public class TestEntities {

    /**
     * 用户实体
     */
    @Data
    @DataScopeEntity(name = "用户", enName = "User")
    @TableName("test_user")
    public static class TestUser {
        private Long userId;
        private String userName;
        private Long deptId;
        private String createBy;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

    /**
     * 部门实体
     */
    @Data
    @DataScopeEntity(name = "部门", enName = "Dept")
    @TableName("test_dept")
    public static class TestDept {
        private Long deptId;
        private String deptName;
        private Long parentId;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

    /**
     * 用户部门关联表
     */
    @Data
    @DataScopeRelation(
            endpointA = @RelationEndpoint(
                    name = "用户",
                    enName = "User",
                    table = "test_user",
                    field = "user_id"
            ),
            endpointB = @RelationEndpoint(
                    name = "部门",
                    enName = "Dept",
                    table = "test_dept",
                    field = "dept_id"
            )
    )
    @TableName("test_user_dept")
    public static class TestUserDept {
        private Long id;
        private Long userId;
        private Long deptId;
    }

    /**
     * 无注解实体
     */
    @Data
    @TableName("test_other")
    public static class TestOther {
        private Long id;
        private String name;
    }
}
