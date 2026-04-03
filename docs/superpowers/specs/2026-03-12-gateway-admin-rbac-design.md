# Gateway-Admin RBAC 权限系统设计文档

> 设计日期: 2026-03-12
> 设计者: Claude Code

## 1. 概述

### 1.1 背景

gateway-admin 是网关管理后台，目前缺乏用户认证和权限控制功能。本设计旨在为 gateway-admin 实现一套完整的 RBAC（基于角色的访问控制）权限系统，使用 SA-Token 框架实现用户登录认证和鉴权。

### 1.2 设计目标

- 实现独立的用户体系，与 blink-base 完全隔离
- 支持三级角色：超级管理员 > 普通管理员 > 普通运维
- 支持菜单级权限控制
- 支持登录失败锁定、操作审计日志、密码策略控制
- 支持单实例/多实例部署模式可配置切换

### 1.3 技术选型

| 技术 | 版本 | 用途 |
|------|------|------|
| SA-Token | 1.37.0 | 认证授权框架 |
| Spring Security Crypto | - | BCrypt 密码加密 |
| Redis | 7.0+ | Token 存储、权限缓存 |
| MySQL | 8.0+ | 业务数据存储 |

---

## 2. 系统架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Vue3)                               │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Nginx / 负载均衡器                            │
└─────────────────────────────────────────────────────────────────┘
                                │
              ┌─────────────────┼─────────────────┐
              ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                   gateway-admin 实例集群                          │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    Controller Layer                       │    │
│  │   AuthController │ UserController │ RoleController │ ... │    │
│  └─────────────────────────────────────────────────────────┘    │
│                              │                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    Service Layer                          │    │
│  │   LoginService │ UserService │ RoleService │ MenuService │    │
│  └─────────────────────────────────────────────────────────┘    │
│                              │                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                   Security Layer                          │    │
│  │      SA-Token │ StpInterface │ PermissionService          │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                                │
              ┌─────────────────┼─────────────────┐
              ▼                 ▼                 ▼
        ┌──────────┐     ┌──────────┐     ┌──────────┐
        │  Redis   │     │  MySQL   │     │  Nacos   │
        │ Token/   │     │  业务    │     │ 配置中心  │
        │ 缓存     │     │  数据    │     │          │
        └──────────┘     └──────────┘     └──────────┘
```

### 2.2 认证流程

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   用户登录   │────▶│  密码校验   │────▶│  创建会话   │
└─────────────┘     └─────────────┘     └─────────────┘
                                              │
                                              ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  请求拦截   │◀────│  权限校验   │◀────│  返回Token  │
└─────────────┘     └─────────────┘     └─────────────┘
       │
       ▼
┌─────────────┐     ┌─────────────┐
│  StpUtil    │────▶│  获取权限   │
└─────────────┘     └─────────────┘
```

---

## 3. 数据库设计

### 3.1 表清单

| 序号 | 表名 | 说明 |
|------|------|------|
| 1 | ga_user | 运维用户表 |
| 2 | ga_role | 角色表 |
| 3 | ga_menu | 菜单表 |
| 4 | ga_user_role_rela | 用户角色关联表 |
| 5 | ga_role_menu_rela | 角色菜单关联表 |
| 6 | ga_operation_log | 操作日志表 |

### 3.2 表结构详情

#### 3.2.1 ga_user（运维用户表）

| 字段 | 类型 | 空 | 键 | 默认值 | 说明 |
|------|------|----|----|--------|------|
| user_id | INT | NO | PRI | AUTO_INCREMENT | 用户ID |
| login_name | VARCHAR(30) | YES | | | 登录名 |
| password | VARCHAR(100) | YES | | | 密码（BCrypt加密） |
| username | VARCHAR(50) | YES | | | 用户昵称 |
| phone | VARCHAR(20) | YES | | | 手机号 |
| email | VARCHAR(64) | YES | | | 邮箱 |
| status | TINYINT | YES | | 0 | 状态：0正常 1禁用 |
| locked | TINYINT | YES | | 0 | 锁定：0未锁定 1管理员锁定 2密码错误锁定 |
| psw_retry | INT | YES | | 0 | 密码错误次数 |
| lock_time | DATETIME | YES | | | 锁定时间 |
| pwd_update_time | DATETIME | YES | | | 密码更新时间 |
| first_login | TINYINT | YES | | 1 | 是否首次登录：0否 1是 |
| last_login_time | DATETIME | YES | | | 最后登录时间 |
| salt | VARCHAR(64) | YES | | | 盐值 |
| create_by | VARCHAR(30) | YES | | | 创建人 |
| update_by | VARCHAR(30) | YES | | | 更新人 |
| create_time | DATETIME | YES | | | 创建时间 |
| update_time | DATETIME | YES | | | 更新时间 |
| del_flag | TINYINT | NO | | 0 | 删除标志：0正常 1删除 |

#### 3.2.2 ga_role（角色表）

| 字段 | 类型 | 空 | 键 | 默认值 | 说明 |
|------|------|----|----|--------|------|
| role_id | INT | NO | PRI | AUTO_INCREMENT | 角色ID |
| role_name | VARCHAR(50) | YES | | | 角色名称 |
| role_code | VARCHAR(50) | YES | | | 角色编码 |
| role_type | TINYINT | YES | | 3 | 角色类型：1超管 2管理员 3运维 |
| status | TINYINT | YES | | 0 | 状态：0启用 1禁用 |
| remark | VARCHAR(500) | YES | | | 备注 |
| create_by | VARCHAR(30) | YES | | | 创建人 |
| update_by | VARCHAR(30) | YES | | | 更新人 |
| create_time | DATETIME | YES | | | 创建时间 |
| update_time | DATETIME | YES | | | 更新时间 |
| del_flag | TINYINT | NO | | 0 | 删除标志 |

#### 3.2.3 ga_menu（菜单表）

| 字段 | 类型 | 空 | 键 | 默认值 | 说明 |
|------|------|----|----|--------|------|
| menu_id | INT | NO | PRI | AUTO_INCREMENT | 菜单ID |
| menu_name | VARCHAR(50) | YES | | | 菜单名称 |
| menu_en_name | VARCHAR(50) | YES | | | 菜单英文名 |
| parent_id | INT | YES | | 0 | 父菜单ID |
| menu_level | INT | YES | | 1 | 菜单层级 |
| menu_type | TINYINT | YES | | 1 | 类型：1目录 2菜单 3按钮 |
| path | VARCHAR(200) | YES | | | 路由地址 |
| component | VARCHAR(200) | YES | | | 组件路径 |
| perms | VARCHAR(100) | YES | | | 权限标识 |
| icon | VARCHAR(100) | YES | | | 图标 |
| order_num | INT | YES | | 0 | 排序号 |
| visible | TINYINT | YES | | 0 | 是否可见：0是 1否 |
| status | TINYINT | YES | | 0 | 状态：0正常 1禁用 |
| create_by | VARCHAR(30) | YES | | | 创建人 |
| update_by | VARCHAR(30) | YES | | | 更新人 |
| create_time | DATETIME | YES | | | 创建时间 |
| update_time | DATETIME | YES | | | 更新时间 |
| del_flag | TINYINT | NO | | 0 | 删除标志 |

#### 3.2.4 ga_user_role_rela（用户角色关联表）

| 字段 | 类型 | 空 | 键 | 默认值 | 说明 |
|------|------|----|----|--------|------|
| user_id | INT | NO | PRI | | 用户ID |
| role_id | INT | NO | PRI | | 角色ID |

#### 3.2.5 ga_role_menu_rela（角色菜单关联表）

| 字段 | 类型 | 空 | 键 | 默认值 | 说明 |
|------|------|----|----|--------|------|
| role_id | INT | NO | PRI | | 角色ID |
| menu_id | INT | NO | PRI | | 菜单ID |

#### 3.2.6 ga_operation_log（操作日志表）

| 字段 | 类型 | 空 | 键 | 默认值 | 说明 |
|------|------|----|----|--------|------|
| log_id | BIGINT | NO | PRI | AUTO_INCREMENT | 日志ID |
| user_id | INT | YES | | | 用户ID |
| login_name | VARCHAR(30) | YES | | | 登录名 |
| operation_type | VARCHAR(50) | YES | | | 操作类型 |
| module | VARCHAR(50) | YES | | | 模块名称 |
| content | VARCHAR(500) | YES | | | 操作内容 |
| request_method | VARCHAR(10) | YES | | | 请求方法 |
| request_url | VARCHAR(200) | YES | | | 请求URL |
| request_params | TEXT | YES | | | 请求参数 |
| response_result | TEXT | YES | | | 响应结果 |
| ip | VARCHAR(50) | YES | | | IP地址 |
| status | TINYINT | YES | | 0 | 状态：0成功 1失败 |
| error_msg | TEXT | YES | | | 错误信息 |
| cost_time | INT | YES | | | 耗时(ms) |
| create_time | DATETIME | YES | | | 创建时间 |

### 3.3 初始数据

#### 初始角色

| role_id | role_name | role_code | role_type |
|---------|-----------|-----------|-----------|
| 1 | 超级管理员 | SUPER_ADMIN | 1 |
| 2 | 管理员 | ADMIN | 2 |
| 3 | 运维人员 | OPERATOR | 3 |

#### 初始用户

| login_name | password | username | role |
|------------|----------|----------|------|
| admin | 123456 | 超级管理员 | SUPER_ADMIN |

---

## 4. SA-Token 配置

### 4.1 依赖配置

```groovy
// build.gradle
// SA-Token 权限认证
implementation 'cn.dev33:sa-token-spring-boot3-starter:1.37.0'
implementation 'cn.dev33:sa-token-redis-jackson:1.37.0'

// 密码加密
implementation 'org.springframework.security:spring-security-crypto'
```

### 4.2 应用配置

```yaml
# 自定义部署配置
gateway-admin:
  deploy:
    # 部署模式: single(单实例) / cluster(多实例集群)
    mode: single
    # 权限缓存开关（多实例必须开启）
    cache-enabled: false

# SA-Token 配置
sa-token:
  token-name: Authorization
  timeout: 604800           # Token 有效期 7 天
  active-timeout: 7200      # 活动超时 2 小时
  is-concurrent: true       # 允许多端登录
  is-share: false           # 每次登录生成新 Token
  token-style: uuid
  is-read-header: true
  is-read-cookie: false
  is-read-body: false
```

### 4.3 多实例配置

```yaml
# 生产环境配置（多实例）
spring:
  config:
    activate:
      on-profile: prod
  data:
    redis:
      cluster:
        nodes:
          - redis-node1:6379
          - redis-node2:6379
          - redis-node3:6379

gateway-admin:
  deploy:
    mode: cluster
    cache-enabled: true
```

---

## 5. 接口设计

### 5.1 认证接口

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/auth/login` | POST | 用户登录 |
| `/auth/logout` | POST | 用户登出 |
| `/auth/refresh` | POST | 刷新 Token |
| `/auth/getUserInfo` | POST | 获取当前用户信息 |
| `/auth/getUserMenus` | POST | 获取当前用户菜单 |
| `/auth/modifyPassword` | POST | 修改密码 |

### 5.2 用户管理接口

| 接口路径 | 说明 |
|---------|------|
| `/user/getUserList` | 分页查询用户列表 |
| `/user/getUser` | 获取用户详情 |
| `/user/saveUser` | 新增用户 |
| `/user/modifyUser` | 修改用户 |
| `/user/deleteUser` | 删除用户 |
| `/user/resetPassword` | 重置密码 |
| `/user/unlockUser` | 解锁用户 |
| `/user/assignRole` | 分配角色 |

### 5.3 角色管理接口

| 接口路径 | 说明 |
|---------|------|
| `/role/getRoleList` | 查询角色列表 |
| `/role/saveRole` | 新增角色 |
| `/role/modifyRole` | 修改角色 |
| `/role/deleteRole` | 删除角色 |
| `/role/assignMenu` | 分配菜单权限 |

### 5.4 菜单管理接口

| 接口路径 | 说明 |
|---------|------|
| `/menu/getMenuTree` | 获取菜单树 |
| `/menu/saveMenu` | 新增菜单 |
| `/menu/modifyMenu` | 修改菜单 |
| `/menu/deleteMenu` | 删除菜单 |

---

## 6. 安全功能

### 6.1 登录失败锁定

- 密码错误累计 5 次后自动锁定账号
- 锁定状态：locked = 2（密码错误锁定）
- 需要管理员解锁或等待自动解锁时间

### 6.2 密码策略

| 策略项 | 配置 |
|-------|------|
| 最小长度 | 8 位 |
| 复杂度 | 必须包含大小写字母、数字、特殊字符中至少3种 |
| 有效期 | 90 天 |
| 历史密码 | 不能与最近 5 次密码相同 |
| 首次登录 | 强制修改密码 |

### 6.3 操作审计日志

- 使用 AOP 切面自动记录操作日志
- 记录内容：操作人、操作类型、请求参数、响应结果、IP、耗时
- 异步写入数据库，不影响请求性能

---

## 7. 代码结构

```
com.blink.gateway.admin
├── config/                          # 配置类
│   ├── SaTokenConfig.java
│   ├── DeployProperties.java
│   └── WebMvcConfig.java
├── security/                        # 安全模块
│   ├── StpInterfaceImpl.java
│   ├── StpExceptionHandler.java
│   └── PasswordEncoder.java
├── auth/                            # 认证模块
│   ├── controller/
│   ├── service/
│   └── dto/
├── user/                            # 用户管理模块
│   ├── controller/
│   ├── service/
│   ├── mapper/
│   ├── entity/
│   └── dto/
├── role/                            # 角色管理模块
├── menu/                            # 菜单管理模块
├── permission/                      # 权限服务
│   ├── UserPermissionService.java
│   └── impl/
├── log/                             # 操作日志模块
│   ├── annotation/
│   ├── aspect/
│   └── service/
└── constans/                        # 常量
    ├── ErrCodeConstant.java
    ├── RedisKeyConstant.java
    └── UserStatusConstant.java
```

---

## 8. 错误码定义

```java
public interface ErrCodeConstant {
    // 用户模块 GATE0100-GATE0109
    String USER_NOT_EXIST = "GATE0100";
    String USER_PASSWORD_ERROR = "GATE0101";
    String USER_LOCKED = "GATE0102";
    String USER_DISABLED = "GATE0103";
    String LOGIN_NAME_EXISTS = "GATE0104";
    String OLD_PASSWORD_ERROR = "GATE0105";
    String PWD_TOO_SHORT = "GATE0106";
    String PWD_COMPLEXITY_FAIL = "GATE0107";

    // 角色模块 GATE0110-GATE0119
    String ROLE_NOT_EXIST = "GATE0110";
    String ROLE_NAME_EXISTS = "GATE0111";
    String ROLE_HAS_USERS = "GATE0112";

    // 菜单模块 GATE0120-GATE0129
    String MENU_NOT_EXIST = "GATE0120";
    String MENU_HAS_CHILDREN = "GATE0121";
    String MENU_NAME_EXISTS = "GATE0122";

    // 认证模块 GATE0130-GATE0139
    String TOKEN_INVALID = "GATE0130";
    String TOKEN_EXPIRED = "GATE0131";
    String PERMISSION_DENIED = "GATE0132";
}
```

---

## 9. 部署模式说明

| 特性 | 单实例模式 | 多实例模式 |
|------|-----------|-----------|
| Redis 用途 | 仅 Token 存储 | Token + 权限缓存 |
| 权限查询 | 直接查数据库 | Redis 缓存优先 |
| 性能 | 一般 | 更好（缓存命中时） |
| 配置复杂度 | 简单 | 需要配置 Redis 集群 |
| 适用场景 | 开发/测试环境 | 生产环境 |

---

## 10. 实现优先级

1. **P0 - 核心功能**
   - 数据库表创建
   - SA-Token 集成配置
   - 登录/登出功能
   - 用户管理 CRUD

2. **P1 - 权限控制**
   - 角色管理
   - 菜单管理
   - 权限校验

3. **P2 - 安全增强**
   - 登录失败锁定
   - 密码策略校验
   - 操作审计日志

4. **P3 - 优化完善**
   - 多实例缓存优化
   - 性能调优