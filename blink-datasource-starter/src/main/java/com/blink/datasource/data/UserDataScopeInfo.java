package com.blink.datasource.data;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * 用户数据权限信息
 * 封装当前用户的数据权限上下文信息
 *
 * @author binblink
 * @since 2026-03-20
 */
@Data
public class UserDataScopeInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 963285923626533184L;
    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 登录名
     */
    private String loginName;


    /**
     * 登入凭证
     */
    private String token;

    /**
     * 用户 -->角色-->权限-->过滤规则
     * 用户过滤规则
     */
    private List<RuleConfig> ruleConfigs;

    /**
     * 超级管理员标志（0否 1是）
     */
    private Integer superFlag;

    /**
     * 角色ID列表
     */
    private List<Integer> roleIds;

    /**
     * 部门和其子部门
     */
    private List<Integer> deptIds;

    /**
     * 用户当前部门id
     */
    private Integer deptId;
}
