# 渠道关联用户选择与权限查询功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现渠道管理中关联用户选择弹窗，支持查看用户权限详情（角色、接口权限、数据过滤权限）

**Architecture:** 通过 Dubbo 接口暴露简化用户列表查询和用户权限详情查询，gateway-admin 调用后提供给前端

**Tech Stack:** Spring Boot 3.2, Dubbo 3.3, MyBatis-Plus, Vue 3, Element Plus, TypeScript

---

## File Structure

### 新增文件

| 文件路径 | 说明 |
|----------|------|
| `blink-base-api-dubbo/.../dto/req/QuerySimpleUserReq.java` | 简化用户查询请求 |
| `blink-base-api-dubbo/.../dto/req/UserIdReq.java` | 用户ID请求 |
| `blink-base-api-dubbo/.../dto/rsp/QuerySimpleUserRsp.java` | 简化用户列表响应 |
| `blink-base-api-dubbo/.../dto/rsp/UserPermissionDetailRsp.java` | 用户权限详情响应 |
| `blink-base-api-dubbo/.../dto/vo/SimpleUserVO.java` | 简化用户信息 |
| `blink-base-api-dubbo/.../dto/vo/DataFilterVO.java` | 数据过滤规则信息 |
| `gateway-admin/.../controller/ChannelUserController.java` | 渠道用户接口控制器 |
| `gateway-admin-web/src/api/channelUser.ts` | 前端 API 接口 |

### 修改文件

| 文件路径 | 说明 |
|----------|------|
| `blink-base-api-dubbo/.../service/BaseDubboService.java` | 新增接口方法 |
| `blink-base-app/.../dubbo/BaseDubboServiceImpl.java` | 实现新增接口 |
| `blink-base-app/.../mapper/SysUserMapper.java` | 新增查询方法 |
| `blink-base-app/.../mapper/SysDataFilterMapper.java` | 新增查询方法 |
| `blink-base-app/.../resources/mapper/SysUserMapper.xml` | 新增 SQL |
| `gateway-admin-web/src/views/channel/index.vue` | 页面改造 |

---

## Task 1: 定义 Dubbo DTO 类

**Files:**
- Create: `blink-base/blink-base-api-dubbo/src/main/java/com/blink/base/dto/req/QuerySimpleUserReq.java`
- Create: `blink-base/blink-base-api-dubbo/src/main/java/com/blink/base/dto/req/UserIdReq.java`
- Create: `blink-base/blink-base-api-dubbo/src/main/java/com/blink/base/dto/vo/SimpleUserVO.java`
- Create: `blink-base/blink-base-api-dubbo/src/main/java/com/blink/base/dto/rsp/QuerySimpleUserRsp.java`
- Create: `blink-base/blink-base-api-dubbo/src/main/java/com/blink/base/dto/vo/DataFilterVO.java`
- Create: `blink-base/blink-base-api-dubbo/src/main/java/com/blink/base/dto/rsp/UserPermissionDetailRsp.java`

- [ ] **Step 1: 创建 QuerySimpleUserReq.java**

```java
package com.blink.base.dto.req;

import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 简化用户查询请求（用于弹窗选择）
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuerySimpleUserReq extends PageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 搜索关键字（模糊匹配 loginName/username）
     */
    private String keyword;
}
```

- [ ] **Step 2: 创建 UserIdReq.java**

```java
package com.blink.base.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户ID请求DTO
 *
 * @author binblink
 */
@Data
public class UserIdReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Integer userId;
}
```

- [ ] **Step 3: 创建 SimpleUserVO.java**

```java
package com.blink.base.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 简化用户信息（用于弹窗选择）
 *
 * @author binblink
 */
@Data
public class SimpleUserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 登录名
     */
    private String loginName;

    /**
     * 昵称
     */
    private String username;
}
```

- [ ] **Step 4: 创建 QuerySimpleUserRsp.java**

```java
package com.blink.base.dto.rsp;

import com.blink.base.dto.vo.SimpleUserVO;
import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 简化用户列表响应
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuerySimpleUserRsp extends PageDTO<SimpleUserVO> {

    @Serial
    private static final long serialVersionUID = 1L;
}
```

- [ ] **Step 5: 创建 DataFilterVO.java**

```java
package com.blink.base.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据过滤规则VO
 *
 * @author binblink
 */
@Data
public class DataFilterVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据过滤ID
     */
    private Integer dataFilterId;

    /**
     * 过滤规则名称
     */
    private String dataFilterName;

    /**
     * 过滤规则英文名称
     */
    private String dataFilterEnName;

    /**
     * 实体类全限定名
     */
    private String entityClass;

    /**
     * 对应表名
     */
    private String tableName;

    /**
     * 规则类型
     */
    private String ruleType;

    /**
     * 状态 0启用 1禁用
     */
    private Byte status;
}
```

- [ ] **Step 6: 创建 UserPermissionDetailRsp.java**

```java
package com.blink.base.dto.rsp;

import com.blink.base.dto.vo.DataFilterVO;
import com.blink.base.dto.vo.SysPermissionVO;
import com.blink.base.dto.vo.SysRoleVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户权限详情响应
 *
 * @author binblink
 */
@Data
public class UserPermissionDetailRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色列表
     */
    private List<SysRoleVO> roles;

    /**
     * 接口权限列表
     */
    private List<SysPermissionVO> permissions;

    /**
     * 数据过滤权限列表
     */
    private List<DataFilterVO> dataFilters;
}
```

- [ ] **Step 7: Commit DTO 文件**

```bash
git add blink-base/blink-base-api-dubbo/src/main/java/com/blink/base/dto/
git commit -m "$(cat <<'EOF'
feat(base-api-dubbo): add DTO classes for user selector and permission query

- QuerySimpleUserReq: request for simplified user list query
- UserIdReq: request with user ID
- SimpleUserVO: simplified user info for selector
- QuerySimpleUserRsp: response for user list
- DataFilterVO: data filter rule info
- UserPermissionDetailRsp: user permission detail response

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: 扩展 BaseDubboService 接口

**Files:**
- Modify: `blink-base/blink-base-api-dubbo/src/main/java/com/blink/base/dubbo/service/BaseDubboService.java`

- [ ] **Step 1: 在 BaseDubboService.java 添加接口方法**

在现有接口末尾（`getAllApiPermissionsAsync` 方法之后）添加：

```java
    // ==================== 渠道关联用户选择 ====================

    /**
     * 查询简化用户列表（用于弹窗选择）
     *
     * @param reqDto 请求参数
     * @return 用户列表
     */
    ResponseDTO<QuerySimpleUserRsp> getSimpleUserList(RequestDTO<QuerySimpleUserReq> reqDto);

    /**
     * 查询用户权限详情（角色、接口权限、数据过滤权限）
     *
     * @param reqDto 请求参数
     * @return 权限详情
     */
    ResponseDTO<UserPermissionDetailRsp> getUserPermissionDetail(RequestDTO<UserIdReq> reqDto);

    // ==================== 异步方法（渠道关联用户选择）====================

    /**
     * 查询简化用户列表（用于弹窗选择）- 异步
     *
     * @param reqDto 请求参数
     * @return 用户列表
     */
    CompletableFuture<ResponseDTO<QuerySimpleUserRsp>> getSimpleUserListAsync(RequestDTO<QuerySimpleUserReq> reqDto);

    /**
     * 查询用户权限详情（角色、接口权限、数据过滤权限）- 异步
     *
     * @param reqDto 请求参数
     * @return 权限详情
     */
    CompletableFuture<ResponseDTO<UserPermissionDetailRsp>> getUserPermissionDetailAsync(RequestDTO<UserIdReq> reqDto);
```

同时需要在文件头部添加 import：

```java
import com.blink.base.dto.req.UserIdReq;
import com.blink.base.dto.req.QuerySimpleUserReq;
import com.blink.base.dto.rsp.QuerySimpleUserRsp;
import com.blink.base.dto.rsp.UserPermissionDetailRsp;
```

- [ ] **Step 2: Commit 接口变更**

```bash
git add blink-base/blink-base-api-dubbo/src/main/java/com/blink/base/dubbo/service/BaseDubboService.java
git commit -m "$(cat <<'EOF'
feat(base-api-dubbo): add user selector and permission query interfaces

- getSimpleUserList: query simplified user list for selector
- getUserPermissionDetail: query user permission detail

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: 实现 Mapper 层查询

**Files:**
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/mapper/SysUserMapper.java`
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/mapper/SysDataFilterMapper.java`
- Modify: `blink-base/blink-base-app/src/main/resources/mapper/SysUserMapper.xml`
- Modify: `blink-base/blink-base-app/src/main/resources/mapper/SysDataFilterMapper.xml`

- [ ] **Step 1: 在 SysUserMapper.java 添加方法**

```java
import com.blink.base.dto.req.QuerySimpleUserReq;
import com.blink.base.dto.vo.SimpleUserVO;

// 在接口中添加方法：

    /**
     * 查询简化用户列表（用于弹窗选择）
     *
     * @param req 查询条件
     * @return 用户列表
     */
    List<SimpleUserVO> selectSimpleUserList(QuerySimpleUserReq req);
```

- [ ] **Step 2: 在 SysDataFilterMapper.java 添加方法**

```java
import com.blink.base.dto.vo.DataFilterVO;

// 在接口中添加方法：

    /**
     * 根据角色ID列表查询数据过滤权限
     *
     * @param roleIds 角色ID列表
     * @return 数据过滤权限列表
     */
    List<DataFilterVO> selectDataFiltersByRoleIds(@Param("roleIds") List<Integer> roleIds);
```

- [ ] **Step 3: 在 SysUserMapper.xml 添加 SQL**

在 `</mapper>` 标签前添加：

```xml
    <!-- 简化用户列表查询结果映射 -->
    <resultMap id="SimpleUserVOResultMap" type="com.blink.base.dto.vo.SimpleUserVO">
        <id column="user_id" property="userId"/>
        <result column="login_name" property="loginName"/>
        <result column="username" property="username"/>
    </resultMap>

    <!-- 查询简化用户列表 -->
    <select id="selectSimpleUserList" parameterType="com.blink.base.dto.req.QuerySimpleUserReq"
            resultMap="SimpleUserVOResultMap">
        SELECT user_id, login_name, username
        FROM sys_user
        WHERE del_flag = 0
        <if test="keyword != null and keyword != ''">
            AND (login_name LIKE CONCAT('%', #{keyword}, '%')
                 OR username LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY create_time DESC
    </select>
```

- [ ] **Step 4: 在 SysDataFilterMapper.xml 添加 SQL**

在 `</mapper>` 标签前添加：

```xml
    <!-- 数据过滤权限VO结果映射 -->
    <resultMap id="DataFilterVOResultMap" type="com.blink.base.dto.vo.DataFilterVO">
        <id column="data_filter_id" property="dataFilterId"/>
        <result column="data_filter_name" property="dataFilterName"/>
        <result column="data_filter_en_name" property="dataFilterEnName"/>
        <result column="entity_class" property="entityClass"/>
        <result column="table_name" property="tableName"/>
        <result column="rule_type" property="ruleType"/>
        <result column="status" property="status"/>
    </resultMap>

    <!-- 根据角色ID列表查询数据过滤权限 -->
    <select id="selectDataFiltersByRoleIds" resultMap="DataFilterVOResultMap">
        SELECT DISTINCT
            df.data_filter_id, df.data_filter_name, df.data_filter_en_name,
            df.entity_class, df.table_name, df.rule_type, df.status
        FROM sys_data_filter df
        INNER JOIN sys_permission p ON p.data_filter_id = df.data_filter_id
        INNER JOIN sys_role_perm_rela rp ON rp.ac_id = p.ac_id
        WHERE df.status = 0
          AND p.data_filter_id IS NOT NULL
        <if test="roleIds != null and roleIds.size() > 0">
            AND rp.role_id IN
            <foreach collection="roleIds" item="roleId" open="(" separator="," close=")">
                #{roleId}
            </foreach>
        </if>
    </select>
```

- [ ] **Step 5: Commit Mapper 层变更**

```bash
git add blink-base/blink-base-app/src/main/java/com/blink/base/mapper/
git add blink-base/blink-base-app/src/main/resources/mapper/
git commit -m "$(cat <<'EOF'
feat(base-app): add mapper methods for user selector and permission query

- SysUserMapper.selectSimpleUserList: query simplified user list
- SysDataFilterMapper.selectDataFiltersByRoleIds: query data filters by role IDs

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: 实现 Dubbo 服务方法

**Files:**
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/dubbo/BaseDubboServiceImpl.java`

- [ ] **Step 1: 在 BaseDubboServiceImpl 添加依赖注入**

在类中添加 Mapper 依赖注入：

```java
    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysDataFilterMapper sysDataFilterMapper;
```

同时添加必要的 import：

```java
import com.blink.base.dto.req.QuerySimpleUserReq;
import com.blink.base.dto.req.UserIdReq;
import com.blink.base.dto.rsp.QuerySimpleUserRsp;
import com.blink.base.dto.rsp.UserPermissionDetailRsp;
import com.blink.base.dto.vo.DataFilterVO;
import com.blink.base.dto.vo.SimpleUserVO;
import com.blink.base.entity.SysDataFilterDO;
import com.blink.base.entity.SysRoleDO;
import com.blink.base.mapper.SysDataFilterMapper;
import com.blink.base.mapper.SysUserMapper;
import com.blink.datasource.utils.PageUtils;
import java.util.ArrayList;
import java.util.stream.Collectors;
```

- [ ] **Step 2: 实现 getSimpleUserList 方法**

在类的末尾（异步方法之前）添加：

```java
    /**
     * 查询简化用户列表（用于弹窗选择）
     *
     * @param reqDto 请求参数
     * @return 用户列表
     */
    @Override
    public ResponseDTO<QuerySimpleUserRsp> getSimpleUserList(RequestDTO<QuerySimpleUserReq> reqDto) {
        try {
            QuerySimpleUserReq req = reqDto.getBody();
            QuerySimpleUserRsp rsp = new QuerySimpleUserRsp();
            PageUtils.queryPage(req, () -> sysUserMapper.selectSimpleUserList(req), rsp);
            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            log.warn("[BaseDubbo] 查询简化用户列表失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[BaseDubbo] 查询简化用户列表异常", e);
            throw new BlinkException(e.getMessage(), e, "GATE0001");
        }
    }
```

- [ ] **Step 3: 实现 getUserPermissionDetail 方法**

```java
    /**
     * 查询用户权限详情（角色、接口权限、数据过滤权限）
     *
     * @param reqDto 请求参数
     * @return 权限详情
     */
    @Override
    public ResponseDTO<UserPermissionDetailRsp> getUserPermissionDetail(RequestDTO<UserIdReq> reqDto) {
        try {
            UserIdReq req = reqDto.getBody();
            Integer userId = req.getUserId();

            // 查询用户是否存在
            var user = sysUserMapper.selectById(userId);
            if (Objects.isNull(user)) {
                BlinkException.throwBusinessException(USER_NOT_EXIST);
            }

            UserPermissionDetailRsp rsp = new UserPermissionDetailRsp();

            // 超级管理员拥有所有权限
            if (CommonConstans.SUPER_ADMIN_YES.equals(user.getSuperFlag())) {
                // 查询所有角色
                List<SysRoleDO> allRoles = roleMapper.selectList(
                        new LambdaQueryWrapper<SysRoleDO>().eq(SysRoleDO::getStatus, 0));
                rsp.setRoles(BeanUtil.copyToList(allRoles, SysRoleVO.class));

                // 查询所有接口权限
                List<SysPermissionDO> allPermissions = sysPermissionMapper.selectList(
                        new LambdaQueryWrapper<SysPermissionDO>()
                                .eq(SysPermissionDO::getAcType, CommonConstans.PERMISSION_API_TYPE));
                rsp.setPermissions(BeanUtil.copyToList(allPermissions, SysPermissionVO.class));

                // 查询所有数据过滤规则
                List<SysDataFilterDO> allDataFilters = sysDataFilterMapper.selectList(
                        new LambdaQueryWrapper<SysDataFilterDO>().eq(SysDataFilterDO::getStatus, 0));
                rsp.setDataFilters(BeanUtil.copyToList(allDataFilters, DataFilterVO.class));

                return ResponseDTO.newSuccessInstance(rsp);
            }

            // 查询用户关联的角色
            List<SysUserRoleRelaDO> userRoleRelas = sysUserRoleRelaMapper.selectList(
                    new LambdaQueryWrapper<SysUserRoleRelaDO>().eq(SysUserRoleRelaDO::getUserId, userId));

            if (CollUtil.isEmpty(userRoleRelas)) {
                rsp.setRoles(new ArrayList<>());
                rsp.setPermissions(new ArrayList<>());
                rsp.setDataFilters(new ArrayList<>());
                return ResponseDTO.newSuccessInstance(rsp);
            }

            List<Integer> roleIds = userRoleRelas.stream()
                    .map(SysUserRoleRelaDO::getRoleId)
                    .collect(Collectors.toList());

            // 查询角色信息
            List<SysRoleDO> roles = roleMapper.selectList(
                    new LambdaQueryWrapper<SysRoleDO>()
                            .in(SysRoleDO::getRoleId, roleIds)
                            .eq(SysRoleDO::getStatus, 0));
            rsp.setRoles(BeanUtil.copyToList(roles, SysRoleVO.class));

            // 查询角色关联的权限ID
            List<SysRolePermRelaDO> permRelas = sysRolePermRelaMapper.selectList(
                    new LambdaQueryWrapper<SysRolePermRelaDO>().in(SysRolePermRelaDO::getRoleId, roleIds));

            if (CollUtil.isNotEmpty(permRelas)) {
                Set<Integer> permIds = permRelas.stream()
                        .map(SysRolePermRelaDO::getAcId)
                        .collect(Collectors.toSet());

                // 查询接口权限（acType=2）
                List<SysPermissionDO> permissions = sysPermissionMapper.selectList(
                        new LambdaQueryWrapper<SysPermissionDO>()
                                .in(SysPermissionDO::getAcId, permIds)
                                .eq(SysPermissionDO::getAcType, CommonConstans.PERMISSION_API_TYPE));
                rsp.setPermissions(BeanUtil.copyToList(permissions, SysPermissionVO.class));
            } else {
                rsp.setPermissions(new ArrayList<>());
            }

            // 查询数据过滤权限
            List<DataFilterVO> dataFilters = sysDataFilterMapper.selectDataFiltersByRoleIds(roleIds);
            rsp.setDataFilters(CollUtil.isEmpty(dataFilters) ? new ArrayList<>() : dataFilters);

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            log.warn("[BaseDubbo] 查询用户权限详情失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[BaseDubbo] 查询用户权限详情异常", e);
            throw new BlinkException(e.getMessage(), e, "GATE0001");
        }
    }
```

- [ ] **Step 4: 实现异步方法**

```java
    @Override
    public CompletableFuture<ResponseDTO<QuerySimpleUserRsp>> getSimpleUserListAsync(RequestDTO<QuerySimpleUserReq> reqDto) {
        if (ioThreadPool != null) {
            return CompletableFuture.supplyAsync(() -> getSimpleUserList(reqDto), ioThreadPool);
        }
        return CompletableFuture.supplyAsync(() -> getSimpleUserList(reqDto));
    }

    @Override
    public CompletableFuture<ResponseDTO<UserPermissionDetailRsp>> getUserPermissionDetailAsync(RequestDTO<UserIdReq> reqDto) {
        if (ioThreadPool != null) {
            return CompletableFuture.supplyAsync(() -> getUserPermissionDetail(reqDto), ioThreadPool);
        }
        return CompletableFuture.supplyAsync(() -> getUserPermissionDetail(reqDto));
    }
```

- [ ] **Step 5: 添加必要的 import 和依赖**

需要确保以下 import 存在：

```java
import com.blink.base.constants.CommonConstans;
import com.blink.base.entity.SysRoleDO;
import com.blink.base.entity.SysPermissionDO;
import com.blink.base.entity.SysUserRoleRelaDO;
import com.blink.base.entity.SysRolePermRelaDO;
import com.blink.base.mapper.SysRoleMapper;
import com.blink.base.mapper.SysPermissionMapper;
import com.blink.base.mapper.SysUserRoleRelaMapper;
import com.blink.base.mapper.SysRolePermRelaMapper;
import com.blink.base.dto.vo.SysRoleVO;
import com.blink.base.dto.vo.SysPermissionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Set;
```

需要确保依赖注入存在（检查并添加缺失的）：

```java
    @Resource
    private SysRoleMapper roleMapper;

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Resource
    private SysUserRoleRelaMapper sysUserRoleRelaMapper;

    @Resource
    private SysRolePermRelaMapper sysRolePermRelaMapper;
```

- [ ] **Step 6: Commit 服务实现**

```bash
git add blink-base/blink-base-app/src/main/java/com/blink/base/dubbo/BaseDubboServiceImpl.java
git commit -m "$(cat <<'EOF'
feat(base-app): implement Dubbo service methods for user selector

- getSimpleUserList: query simplified user list with keyword search
- getUserPermissionDetail: query user roles, API permissions and data filters

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: 发布 base-api-dubbo 到本地 Maven 仓库

**Files:**
- Build: `blink-base/blink-base-api-dubbo`

- [ ] **Step 1: 发布 base-api-dubbo 模块**

```bash
./gradlew :blink-base:blink-base-api-dubbo:publishToMavenLocal
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 发布 base-app 模块（如需要）**

```bash
./gradlew :blink-base:blink-base-app:publishToMavenLocal
```

Expected: BUILD SUCCESSFUL

---

## Task 6: 实现 Gateway Admin Controller

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/ChannelUserController.java`

- [ ] **Step 1: 创建 ChannelUserController.java**

```java
package com.blink.gateway.admin.controller;

import com.blink.base.dto.req.QuerySimpleUserReq;
import com.blink.base.dto.req.UserIdReq;
import com.blink.base.dto.rsp.QuerySimpleUserRsp;
import com.blink.base.dto.rsp.UserPermissionDetailRsp;
import com.blink.base.dubbo.service.BaseDubboService;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 渠道用户控制器
 * 提供用户选择和权限查询接口
 *
 * @author binblink
 */
@Slf4j
@RestController
@RequestMapping("/channelUser")
public class ChannelUserController {

    @DubboReference(timeout = 10000, check = false)
    private BaseDubboService baseDubboService;

    /**
     * 查询简化用户列表（用于弹窗选择）
     *
     * @param reqDto 请求参数
     * @return 用户列表
     */
    @PostMapping("/getSimpleUserList")
    public ResponseDTO<QuerySimpleUserRsp> getSimpleUserList(
            @RequestBody @Validated RequestDTO<QuerySimpleUserReq> reqDto) {
        log.info("[ChannelUser] 查询简化用户列表 | keyword: {}", 
                reqDto.getBody() != null ? reqDto.getBody().getKeyword() : null);
        return baseDubboService.getSimpleUserList(reqDto);
    }

    /**
     * 查询用户权限详情
     *
     * @param reqDto 请求参数
     * @return 权限详情
     */
    @PostMapping("/getUserPermissionDetail")
    public ResponseDTO<UserPermissionDetailRsp> getUserPermissionDetail(
            @RequestBody @Validated RequestDTO<UserIdReq> reqDto) {
        log.info("[ChannelUser] 查询用户权限详情 | userId: {}", 
                reqDto.getBody() != null ? reqDto.getBody().getUserId() : null);
        return baseDubboService.getUserPermissionDetail(reqDto);
    }
}
```

- [ ] **Step 2: Commit Controller**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/ChannelUserController.java
git commit -m "$(cat <<'EOF'
feat(gateway-admin): add ChannelUserController for user selector API

- getSimpleUserList: API for user selector dialog
- getUserPermissionDetail: API for viewing user permissions

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: 实现前端 API 接口

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/api/channelUser.ts`

- [ ] **Step 1: 创建 channelUser.ts**

```typescript
import request from '@/utils/request'
import type { PageResult } from '@/types'

/**
 * 简化用户查询参数
 */
export interface QuerySimpleUserParams {
  pageNum: number
  pageSize: number
  keyword?: string
}

/**
 * 简化用户信息
 */
export interface SimpleUserInfo {
  userId: number
  loginName: string
  username: string
}

/**
 * 角色信息
 */
export interface RoleInfo {
  roleId: number
  roleName: string
  roleEnName: string
  status: number
}

/**
 * 权限信息
 */
export interface PermissionInfo {
  acId: number
  acName: string
  acEnName: string
  acIdentity: string
  acType: number
  url: string
  status: number
}

/**
 * 数据过滤权限信息
 */
export interface DataFilterInfo {
  dataFilterId: number
  dataFilterName: string
  dataFilterEnName: string
  entityClass: string
  tableName: string
  ruleType: string
  status: number
}

/**
 * 用户权限详情
 */
export interface UserPermissionDetail {
  roles: RoleInfo[]
  permissions: PermissionInfo[]
  dataFilters: DataFilterInfo[]
}

/**
 * 查询简化用户列表
 */
export const getSimpleUserList = (params: QuerySimpleUserParams): Promise<PageResult<SimpleUserInfo>> => {
  return request.post('/channelUser/getSimpleUserList', { body: params }) as Promise<PageResult<SimpleUserInfo>>
}

/**
 * 查询用户权限详情
 */
export const getUserPermissionDetail = (userId: number): Promise<UserPermissionDetail> => {
  return request.post('/channelUser/getUserPermissionDetail', { body: { userId } }) as Promise<UserPermissionDetail>
}
```

- [ ] **Step 2: Commit 前端 API**

```bash
git add blink-gateway/gateway-admin-web/src/api/channelUser.ts
git commit -m "$(cat <<'EOF'
feat(gateway-admin-web): add channelUser API for user selector

- getSimpleUserList: fetch simplified user list for selector dialog
- getUserPermissionDetail: fetch user permission detail

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: 改造渠道管理前端页面

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/views/channel/index.vue`

- [ ] **Step 1: 添加 import 和类型定义**

在 `<script setup lang="ts">` 部分添加：

```typescript
import {
  getSimpleUserList,
  getUserPermissionDetail,
  type SimpleUserInfo,
  type UserPermissionDetail,
  type PermissionInfo,
  type DataFilterInfo
} from '@/api/channelUser'
```

- [ ] **Step 2: 添加用户选择弹窗相关状态**

在现有响应式变量后添加：

```typescript
// 用户选择弹窗
const userSelectDialogVisible = ref(false)
const userSelectLoading = ref(false)
const userSelectKeyword = ref('')
const userSelectData = ref<SimpleUserInfo[]>([])
const userSelectTotal = ref(0)
const userSelectPageNum = ref(1)
const userSelectPageSize = ref(10)
const selectedUser = ref<SimpleUserInfo | null>(null)

// 权限详情弹窗
const permissionDialogVisible = ref(false)
const permissionLoading = ref(false)
const permissionDetail = ref<UserPermissionDetail>({
  roles: [],
  permissions: [],
  dataFilters: []
})
const permissionActiveTab = ref('roles')
```

- [ ] **Step 3: 添加用户查询方法**

```typescript
/**
 * 加载用户列表
 */
const loadUserList = async () => {
  userSelectLoading.value = true
  try {
    const res = await getSimpleUserList({
      pageNum: userSelectPageNum.value,
      pageSize: userSelectPageSize.value,
      keyword: userSelectKeyword.value
    })
    userSelectData.value = res.rows || []
    userSelectTotal.value = res.total || 0
  } catch (error) {
    console.error('[ChannelManagement] Failed to load user list:', error)
    userSelectData.value = []
    userSelectTotal.value = 0
  } finally {
    userSelectLoading.value = false
  }
}

/**
 * 打开用户选择弹窗
 */
const openUserSelectDialog = () => {
  userSelectKeyword.value = ''
  userSelectPageNum.value = 1
  selectedUser.value = null
  userSelectDialogVisible.value = true
  loadUserList()
}

/**
 * 用户搜索
 */
const handleUserSearch = () => {
  userSelectPageNum.value = 1
  loadUserList()
}

/**
 * 用户分页变更
 */
const handleUserPageChange = (page: number) => {
  userSelectPageNum.value = page
  loadUserList()
}

/**
 * 选择用户
 */
const selectUser = (user: SimpleUserInfo) => {
  selectedUser.value = user
  formData.relaUserId = String(user.userId)
  userSelectDialogVisible.value = false
}

/**
 * 查看用户权限
 */
const viewUserPermission = async (user: SimpleUserInfo) => {
  permissionLoading.value = true
  permissionDialogVisible.value = true
  permissionActiveTab.value = 'roles'
  try {
    const res = await getUserPermissionDetail(user.userId)
    permissionDetail.value = res
  } catch (error) {
    console.error('[ChannelManagement] Failed to load user permission:', error)
    permissionDetail.value = { roles: [], permissions: [], dataFilters: [] }
  } finally {
    permissionLoading.value = false
  }
}
```

- [ ] **Step 4: 在表单中改造关联用户输入项**

将现有的关联用户表单项（如果存在）或添加新的：

```vue
        <el-form-item :label="t('channel.relaUserId')">
          <el-input
            v-model="formData.relaUserId"
            readonly
            :placeholder="t('common.pleaseSelect') + t('channel.relaUserId')"
            @click="openUserSelectDialog"
          >
            <template #suffix>
              <el-icon class="cursor-pointer" @click="openUserSelectDialog">
                <Search />
              </el-icon>
            </template>
          </el-input>
          <div v-if="selectedUser" class="selected-user-info">
            {{ selectedUser.loginName }} ({{ selectedUser.username }})
          </div>
        </el-form-item>
```

- [ ] **Step 5: 添加用户选择弹窗组件**

在 Form Dialog 之后添加：

```vue
    <!-- User Select Dialog -->
    <el-dialog
      v-model="userSelectDialogVisible"
      :title="t('channel.selectUser')"
      width="700px"
      :close-on-click-modal="false"
      :lock-scroll="false"
    >
      <div class="user-select-dialog">
        <div class="search-bar">
          <el-input
            v-model="userSelectKeyword"
            :placeholder="t('channel.searchUserPlaceholder')"
            clearable
            style="width: 250px"
            @keyup.enter="handleUserSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="handleUserSearch">
            {{ t('common.search') }}
          </el-button>
        </div>
        <el-table
          v-loading="userSelectLoading"
          :data="userSelectData"
          height="350"
          stripe
          highlight-current-row
          @current-change="(row: SimpleUserInfo) => selectedUser = row"
        >
          <el-table-column prop="loginName" :label="t('user.loginName')" min-width="120" />
          <el-table-column prop="username" :label="t('user.username')" min-width="120" />
          <el-table-column :label="t('common.operation')" width="100" align="center">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="viewUserPermission(row)">
                {{ t('channel.viewPermission') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="userSelectPageNum"
            v-model:page-size="userSelectPageSize"
            :total="userSelectTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, prev, pager, next"
            @current-change="handleUserPageChange"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="userSelectDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :disabled="!selectedUser" @click="selectUser(selectedUser!)">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Permission Detail Dialog -->
    <el-dialog
      v-model="permissionDialogVisible"
      :title="t('channel.userPermission')"
      width="700px"
      :close-on-click-modal="false"
      :lock-scroll="false"
    >
      <el-tabs v-model="permissionActiveTab">
        <el-tab-pane :label="t('channel.roles')" name="roles">
          <el-table v-loading="permissionLoading" :data="permissionDetail.roles" max-height="400" stripe>
            <el-table-column prop="roleName" :label="t('role.roleName')" min-width="120" />
            <el-table-column prop="roleEnName" :label="t('role.roleEnName')" min-width="120" />
            <el-table-column :label="t('common.status')" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.status === 0" type="success">{{ t('common.enabled') }}</el-tag>
                <el-tag v-else type="danger">{{ t('common.disabled') }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="t('channel.permissions')" name="permissions">
          <el-table v-loading="permissionLoading" :data="permissionDetail.permissions" max-height="400" stripe>
            <el-table-column prop="acName" :label="t('permission.acName')" min-width="120" />
            <el-table-column prop="acIdentity" :label="t('permission.acIdentity')" min-width="150" />
            <el-table-column prop="url" :label="t('permission.url')" min-width="200" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="t('channel.dataFilters')" name="dataFilters">
          <el-table v-loading="permissionLoading" :data="permissionDetail.dataFilters" max-height="400" stripe>
            <el-table-column prop="dataFilterName" :label="t('dataFilter.dataFilterName')" min-width="120" />
            <el-table-column prop="tableName" :label="t('dataFilter.tableName')" min-width="120" />
            <el-table-column prop="ruleType" :label="t('dataFilter.ruleType')" min-width="100" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>
```

- [ ] **Step 6: 添加样式**

在 `<style scoped lang="scss">` 部分添加：

```scss
.user-select-dialog {
  .search-bar {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
  }

  .pagination-wrapper {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}

.selected-user-info {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
```

- [ ] **Step 7: 添加国际化文本**

需要在 `src/locales/zh-cn.ts` 和 `src/locales/en-us.ts` 中添加相应的翻译键。检查并添加缺失的翻译。

在 `zh-cn.ts` 的 `channel` 对象中添加：

```typescript
  channel: {
    // ... existing keys
    relaUserId: '关联用户',
    selectUser: '选择用户',
    searchUserPlaceholder: '请输入登录名或用户名搜索',
    viewPermission: '查看权限',
    userPermission: '用户权限',
    roles: '角色列表',
    permissions: '接口权限',
    dataFilters: '数据过滤权限'
  }
```

在 `en-us.ts` 的 `channel` 对象中添加：

```typescript
  channel: {
    // ... existing keys
    relaUserId: 'Related User',
    selectUser: 'Select User',
    searchUserPlaceholder: 'Search by login name or username',
    viewPermission: 'View Permission',
    userPermission: 'User Permission',
    roles: 'Roles',
    permissions: 'API Permissions',
    dataFilters: 'Data Filters'
  }
```

- [ ] **Step 8: Commit 前端页面改造**

```bash
git add blink-gateway/gateway-admin-web/src/views/channel/index.vue
git add blink-gateway/gateway-admin-web/src/locales/
git commit -m "$(cat <<'EOF'
feat(gateway-admin-web): implement user selector dialog for channel

- Add user selector dialog with search and pagination
- Add permission detail dialog with tabs for roles/permissions/dataFilters
- Add channelUser API integration

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Task 9: 验证构建

**Files:**
- Build: `blink-base` 和 `gateway-admin` 模块

- [ ] **Step 1: 构建 base-api-dubbo 模块**

```bash
./gradlew :blink-base:blink-base-api-dubbo:build
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 构建 base-app 模块**

```bash
./gradlew :blink-base:blink-base-app:build
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 构建 gateway-admin 模块**

```bash
./gradlew :blink-gateway:gateway-admin:build
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 构建前端项目**

```bash
cd blink-gateway/gateway-admin-web && npm run build
```

Expected: 构建成功，无错误

---

## Task 10: 最终提交

- [ ] **Step 1: 检查所有变更**

```bash
git status
git diff --stat
```

- [ ] **Step 2: 创建最终提交（如有未提交的变更）**

```bash
git add -A
git commit -m "$(cat <<'EOF'
feat: implement channel user selector and permission query

- Add Dubbo interfaces for user list and permission detail query
- Implement backend services with role/API permission/data filter support
- Add frontend user selector dialog with permission view
- Support keyword search and pagination for user selection

Closes: channel user selector feature

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Summary

| Task | Description | Files Changed |
|------|-------------|---------------|
| 1 | 定义 Dubbo DTO 类 | 6 new files |
| 2 | 扩展 BaseDubboService 接口 | 1 modified |
| 3 | 实现 Mapper 层查询 | 4 modified |
| 4 | 实现 Dubbo 服务方法 | 1 modified |
| 5 | 发布到本地 Maven | build only |
| 6 | 实现 Gateway Admin Controller | 1 new file |
| 7 | 实现前端 API 接口 | 1 new file |
| 8 | 改造渠道管理前端页面 | 2-3 modified |
| 9 | 验证构建 | build verification |
| 10 | 最终提交 | git commit |