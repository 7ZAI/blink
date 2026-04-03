# 渠道关联用户选择与权限查询功能设计

## 概述

在渠道管理新增/编辑渠道时，为"关联用户"字段提供弹窗选择功能，并支持查看用户权限详情（角色、接口权限、数据过滤权限）。

## 需求说明

### 功能点

1. **用户选择弹窗**
   - 点击"关联用户"输入框弹出用户列表弹窗
   - 支持关键字搜索（模糊匹配 loginName/username）
   - 单选用户
   - 每行末尾有"查看权限"按钮

2. **权限详情弹窗**
   - 展示用户角色列表
   - 展示接口权限列表
   - 展示数据过滤权限列表

### 交互流程

```
渠道表单 → 点击关联用户输入框 → 用户选择弹窗
                                    ↓
                            搜索/浏览用户列表
                                    ↓
                            点击"查看权限" → 权限详情弹窗
                                    ↓
                            选择用户 → 回填表单
```

## 技术设计

### 模块变更

| 模块 | 变更内容 |
|------|----------|
| blink-base-api-dubbo | 新增 Dubbo 接口定义和 DTO |
| blink-base-app | 实现 Dubbo 服务方法 |
| gateway-admin | 通过 Dubbo 调用并提供 REST 接口 |
| gateway-admin-web | 前端页面改造 |

### 1. Dubbo 接口定义 (base-api-dubbo)

#### 1.1 新增 DTO 类

**QuerySimpleUserReq.java** - 简化用户查询请求
```java
package com.blink.base.dto.req;

public class QuerySimpleUserReq extends PageDTO implements Serializable {
    /**
     * 搜索关键字（模糊匹配 loginName/username）
     */
    private String keyword;
}
```

**SimpleUserVO.java** - 简化用户信息
```java
package com.blink.base.dto.vo;

public class SimpleUserVO implements Serializable {
    private Integer userId;
    private String loginName;
    private String username;
}
```

**QuerySimpleUserRsp.java** - 简化用户列表响应
```java
package com.blink.base.dto.rsp;

public class QuerySimpleUserRsp extends PageDTO<SimpleUserVO> implements Serializable {
}
```

**DataFilterVO.java** - 数据过滤规则信息
```java
package com.blink.base.dto.vo;

public class DataFilterVO implements Serializable {
    private Integer dataFilterId;
    private String dataFilterName;
    private String dataFilterEnName;
    private String entityClass;
    private String tableName;
    private String ruleType;
    private Byte status;
}
```

**UserPermissionDetailRsp.java** - 用户权限详情响应
```java
package com.blink.base.dto.rsp;

public class UserPermissionDetailRsp implements Serializable {
    private List<SysRoleVO> roles;              // 角色列表
    private List<SysPermissionVO> permissions;  // 接口权限列表
    private List<DataFilterVO> dataFilters;     // 数据过滤权限列表
}
```

**UserIdReq.java** - 用户ID请求（如已存在则复用）
```java
package com.blink.base.dto.req;

public class UserIdReq implements Serializable {
    private Integer userId;
}
```

#### 1.2 扩展 BaseDubboService 接口

```java
// ==================== 渠道关联用户选择 ====================

/**
 * 查询简化用户列表（用于弹窗选择）
 * @param reqDto 请求参数
 * @return 用户列表
 */
ResponseDTO<QuerySimpleUserRsp> getSimpleUserList(RequestDTO<QuerySimpleUserReq> reqDto);

/**
 * 查询用户权限详情（角色、接口权限、数据过滤权限）
 * @param reqDto 请求参数
 * @return 权限详情
 */
ResponseDTO<UserPermissionDetailRsp> getUserPermissionDetail(RequestDTO<UserIdReq> reqDto);


```

### 2. 后端实现 (base-app)

#### 2.1 BaseDubboServiceImpl 新增实现

**getSimpleUserList 实现逻辑：**
1. 解析请求参数
2. 调用 SysUserMapper 查询简化用户列表
3. 支持关键字模糊匹配 loginName 和 username
4. 返回分页结果

**getUserPermissionDetail 实现逻辑：**
1. 根据 userId 查询用户信息
2. 超级管理员返回所有权限
3. 普通用户：
   - 查询用户关联的角色列表
   - 查询角色关联的接口权限（acType=2）
   - 查询角色关联的数据过滤权限（acType=1，通过 permission 关联 dataFilterId）

#### 2.2 SysUserMapper 新增方法

```java
/**
 * 查询简化用户列表
 * @param req 查询条件
 * @return 用户列表
 */
List<SimpleUserVO> selectSimpleUserList(@Param("req") QuerySimpleUserReq req);
```

#### 2.3 SysPermissionMapper 新增方法

```java
/**
 * 根据角色ID列表查询数据过滤权限
 * @param roleIds 角色ID列表
 * @return 数据过滤权限列表
 */
List<DataFilterVO> selectDataFiltersByRoleIds(@Param("roleIds") List<Integer> roleIds);
```

### 3. Gateway Admin 接口 (gateway-admin)

#### 3.1 新增 ChannelUserController

```java
@RestController
@RequestMapping("/channelUser")
public class ChannelUserController {

    @DubboReference
    private BaseDubboService baseDubboService;

    /**
     * 查询简化用户列表
     */
    @PostMapping("/getSimpleUserList")
    public ResponseDTO<QuerySimpleUserRsp> getSimpleUserList(
        @RequestBody @Validated RequestDTO<QuerySimpleUserReq> reqDto);

    /**
     * 查询用户权限详情
     */
    @PostMapping("/getUserPermissionDetail")
    public ResponseDTO<UserPermissionDetailRsp> getUserPermissionDetail(
        @RequestBody @Validated RequestDTO<UserIdReq> reqDto);
}
```

### 4. 前端实现 (gateway-admin-web)

#### 4.1 新增 API 接口

**channelUser.ts**
```typescript
export interface QuerySimpleUserParams {
  pageNum: number
  pageSize: number
  keyword?: string
}

export interface SimpleUserInfo {
  userId: number
  loginName: string
  username: string
}

export interface UserPermissionDetail {
  roles: RoleInfo[]
  permissions: PermissionInfo[]
  dataFilters: DataFilterInfo[]
}

export const getSimpleUserList = (params: QuerySimpleUserParams): Promise<PageResult<SimpleUserInfo>>
export const getUserPermissionDetail = (userId: number): Promise<UserPermissionDetail>
```

#### 4.2 渠道管理页面改造

**channel/index.vue 改造点：**

1. **关联用户表单项改造**
   - 改为只读输入框 + 点击触发弹窗
   - 显示选中用户的 loginName 或 username

2. **新增用户选择弹窗**
   - 搜索框 + 用户表格
   - 操作列包含"查看权限"按钮
   - 单选行点击确定选择

3. **新增权限详情弹窗**
   - 角色列表 Tab
   - 接口权限列表 Tab
   - 数据过滤权限列表 Tab

### 5. 数据库查询

#### 5.1 查询简化用户列表 SQL

```sql
SELECT user_id, login_name, username
FROM sys_user
WHERE del_flag = 0
  AND (login_name LIKE CONCAT('%', #{req.keyword}, '%')
       OR username LIKE CONCAT('%', #{req.keyword}, '%'))
ORDER BY create_time DESC
```

#### 5.2 查询数据过滤权限 SQL

```sql
SELECT DISTINCT df.*
FROM sys_data_filter df
INNER JOIN sys_permission p ON p.data_filter_id = df.data_filter_id
INNER JOIN sys_role_perm_rela rp ON rp.ac_id = p.ac_id
WHERE rp.role_id IN (角色ID列表)
  AND p.ac_type = 1  -- 数据权限类型
```

## 文件清单

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
| `blink-base-app/.../mapper/SysPermissionMapper.java` | 新增查询方法 |
| `blink-base-app/.../mapper/SysUserMapper.xml` | 新增 SQL |
| `blink-base-app/.../mapper/SysPermissionMapper.xml` | 新增 SQL |
| `gateway-admin-web/src/views/channel/index.vue` | 页面改造 |

## 错误码

| 错误码 | 说明 |
|--------|------|
| `GATE0020` | 用户不存在 |

## 注意事项

1. **权限校验**：查询用户权限详情时，不需要特殊权限控制，任何登录用户都可查看
2. **数据过滤**：简化用户列表查询不应用数据权限过滤
3. **超级管理员**：超级管理员的数据过滤权限应返回所有规则
4. **前端交互**：用户选择弹窗和权限详情弹窗使用 Element Plus 的 Dialog 组件
5. **DTO 位置**：所有新增的 DTO 类都放在 `base-api-dubbo` 模块中，保持 Dubbo 接口的独立性
6. **UserIdReq 说明**：base-app 中已有 UserIdReq（userId 为 String 类型），base-api-dubbo 需新建同名类保持一致