# 数据范围权限实施计划

## 概述

基于 `docs/superpowers/specs/2026-03-17-data-scope-permission-design.md` 设计文档，实施动态数据范围权限系统。

## 实施状态分析

### 已完成（无需修改）
- ✅ `SysDataFilterDO` 实体类结构已符合新设计
- ✅ `SysRolePermRelaMapper.selectDataFilterIdsByRoleId` 方法已存在
- ✅ `UserInfoRedisDO` 已包含 `superFlag` 和 `roleIds` 字段

### 需要实施的任务

---

## 一、基础设施层

### 1.1 常量与错误码
**文件：** `blink-base/blink-base-app/src/main/java/com/blink/base/constans/`

| 文件 | 修改内容 |
|------|---------|
| `BaseErrCodeConstant.java` | 新增数据范围错误码 `BUSS0050-BUSS0053` |
| `RedisKeyConstans.java` | 新增 `DATA_SCOPE_ROLE = "blink:data_scope:role:"` |

### 1.2 Mapper 方法扩展

| Mapper文件 | 新增方法 | XML配置 |
|-----------|---------|---------|
| `SysUserGroupRelaMapper.java` | `selectDeptIdByUserId(Integer userId)` | 新增XML |
| `SysUserRoleRelaMapper.java` | `selectUserIdsByRoleIds(List<Integer> roleIds)` | 新增XML |
| `SysGroupMapper.java` | `selectDeptAndChildrenById(Integer deptId)` | 新增XML |
| `SysDataFilterMapper.java` | 新建Mapper接口 | 新建XML |

---

## 二、数据范围权限核心模块

**包路径：** `com.blink.base.datascope`

### 2.1 注解与枚举

| 类名 | 路径 | 说明 |
|-----|------|------|
| `DataScope.java` | `annotation/` | 标记注解，控制是否启用过滤 |
| `DataScopeRuleType.java` | `constant/` | 规则类型枚举 |

### 2.2 DTO 类

**路径：** `com.blink.base.datascope.dto`

| 类名 | 类型 | 说明 |
|-----|------|------|
| `RuleConfig.java` | 通用 | 规则配置JSON映射类 |
| `AddDataFilterReq.java` | req | 新增请求DTO |
| `UpdateDataFilterReq.java` | req | 更新请求DTO |
| `QueryDataFilterReq.java` | req | 查询请求DTO（继承PageDTO） |
| `DataFilterIdReq.java` | req | ID请求DTO |
| `GetEntityFieldsReq.java` | req | 获取实体字段请求DTO |
| `QueryDataFilterRsp.java` | rsp | 查询响应DTO（继承PageDTO） |
| `DataFilterVO.java` | vo | 数据过滤规则VO |
| `EntityFieldVO.java` | vo | 实体字段VO |
| `EntityFieldsRsp.java` | rsp | 实体字段列表响应DTO |

### 2.3 核心组件

| 类名 | 路径 | 职责 |
|-----|------|------|
| `DataScopeContext.java` | `context/` | 封装用户上下文、Mapper依赖 |
| `DataScopeInterceptor.java` | `interceptor/` | MyBatis拦截器，拦截SQL应用过滤 |
| `DataScopeEntityScanner.java` | `scanner/` | 启动扫描实体类，建立表名映射 |
| `DataScopeCache.java` | `cache/` | Redis缓存服务，获取/合并规则 |
| `RuleMergeStrategy.java` | `merge/` | 多角色规则合并策略 |
| `DataScopeSqlUtil.java` | `util/` | SQL解析、字段过滤、条件追加 |
| `CustomSqlValidator.java` | `util/` | SQL片段安全验证 |

### 2.4 规则处理器

**路径：** `com.blink.base.datascope.handler`

| 类名 | 规则类型 | 说明 |
|-----|---------|------|
| `RuleHandler.java` | 接口 | 规则处理器接口 |
| `FieldFilterHandler.java` | FIELD_FILTER | 字段过滤处理 |
| `CreatorFilterHandler.java` | CREATOR_FILTER | 创建人过滤处理 |
| `DeptFilterHandler.java` | DEPT_FILTER | 部门过滤处理 |
| `DateRangeFilterHandler.java` | DATE_RANGE_FILTER | 时间范围过滤处理 |
| `StatusFilterHandler.java` | STATUS_FILTER | 状态过滤处理 |
| `CustomSqlHandler.java` | CUSTOM_SQL | 自定义SQL处理 |

---

## 三、业务服务层

### 3.1 Service

| 文件 | 路径 | 方法 |
|-----|------|------|
| `SysDataFilterService.java` | `service/` | 接口定义 |
| `SysDataFilterServiceImpl.java` | `service/impl/` | 服务实现 |

**核心方法：**
- `getDataFilterList(QueryDataFilterReq req)` - 分页查询
- `addDataFilter(AddDataFilterReq req)` - 新增规则
- `updateDataFilter(UpdateDataFilterReq req)` - 更新规则
- `deleteDataFilter(Integer dataFilterId)` - 删除规则
- `getEntityFields(String entityClass)` - 获取实体字段列表

### 3.2 Controller

| 文件 | 路径 | 接口 |
|-----|------|------|
| `SysDataFilterController.java` | `controller/` | REST接口 |

**接口列表：**
- `POST /sysDataFilter/getDataFilterList` - 查询列表
- `POST /sysDataFilter/addDataFilter` - 新增
- `POST /sysDataFilter/updateDataFilter` - 更新
- `POST /sysDataFilter/deleteDataFilter` - 删除
- `POST /sysDataFilter/getEntityFields` - 获取实体字段

---

## 四、自动配置

### 4.1 配置类

| 文件 | 模块 | 说明 |
|-----|------|------|
| `DataScopeAutoConfiguration.java` | `blink-datasource-starter` | 注册MyBatis拦截器 |

---

## 五、前端实现

**模块：** `blink-base-web`

### 5.1 API 层

| 文件 | 说明 |
|-----|------|
| `src/api/dataScope.ts` | 数据权限API接口定义 |

### 5.2 视图组件

| 文件 | 说明 |
|-----|------|
| `src/views/system/dataScope/index.vue` | 列表页面 |
| `src/views/system/dataScope/components/DataFilterFormDialog.vue` | 新增/编辑弹窗 |

### 5.3 国际化

| 文件 | 新增内容 |
|-----|---------|
| `src/locales/zh-cn.ts` | dataScope 中文文案 |
| `src/locales/en-us.ts` | dataScope 英文文案 |

---

## 六、实施顺序

### Phase 1: 基础设施（无依赖）
1. 常量与错误码
2. 注解与枚举
3. DTO类

### Phase 2: Mapper扩展
4. 新增Mapper方法及XML配置
5. 新建SysDataFilterMapper

### Phase 3: 核心组件
6. DataScopeSqlUtil（工具类）
7. CustomSqlValidator（验证器）
8. DataScopeContext（上下文）
9. DataScopeEntityScanner（实体扫描）
10. RuleMergeStrategy（合并策略）
11. DataScopeCache（缓存服务）

### Phase 4: 规则处理器
12. RuleHandler接口
13. FieldFilterHandler
14. CreatorFilterHandler
15. DeptFilterHandler
16. DateRangeFilterHandler
17. StatusFilterHandler
18. CustomSqlHandler

### Phase 5: 拦截器与配置
19. DataScopeInterceptor
20. DataScopeAutoConfiguration

### Phase 6: 业务层
21. SysDataFilterService接口与实现
22. SysDataFilterController

### Phase 7: 前端
23. API接口定义
24. 列表页面
25. 表单弹窗
26. 国际化配置

---

## 七、依赖关系图

```
DataScopeInterceptor
    ├── DataScopeCache
    │   ├── SysRolePermRelaMapper
    │   ├── SysDataFilterMapper
    │   └── RuleMergeStrategy
    ├── List<RuleHandler>
    │   └── 各Handler实现
    ├── DataScopeEntityScanner
    ├── DataScopeSqlUtil
    └── DataScopeContext
        ├── UserInfoRedisDO
        ├── SysUserGroupRelaMapper
        ├── SysUserRoleRelaMapper
        └── SysGroupMapper
```

---

## 八、文件清单

### 新建文件（共32个）

**后端（26个）：**
```
blink-base/blink-base-app/src/main/java/com/blink/base/
├── datascope/
│   ├── annotation/DataScope.java
│   ├── constant/DataScopeRuleType.java
│   ├── dto/
│   │   ├── RuleConfig.java
│   │   ├── req/
│   │   │   ├── AddDataFilterReq.java
│   │   │   ├── UpdateDataFilterReq.java
│   │   │   ├── QueryDataFilterReq.java
│   │   │   ├── DataFilterIdReq.java
│   │   │   └── GetEntityFieldsReq.java
│   │   └── rsp/
│   │       ├── QueryDataFilterRsp.java
│   │       ├── DataFilterVO.java
│   │       ├── EntityFieldVO.java
│   │       └── EntityFieldsRsp.java
│   ├── context/DataScopeContext.java
│   ├── interceptor/DataScopeInterceptor.java
│   ├── scanner/DataScopeEntityScanner.java
│   ├── cache/DataScopeCache.java
│   ├── merge/RuleMergeStrategy.java
│   ├── util/
│   │   ├── DataScopeSqlUtil.java
│   │   └── CustomSqlValidator.java
│   ├── handler/
│   │   ├── RuleHandler.java
│   │   ├── FieldFilterHandler.java
│   │   ├── CreatorFilterHandler.java
│   │   ├── DeptFilterHandler.java
│   │   ├── DateRangeFilterHandler.java
│   │   ├── StatusFilterHandler.java
│   │   └── CustomSqlHandler.java
│   ├── mapper/SysDataFilterMapper.java
│   ├── service/
│   │   ├── SysDataFilterService.java
│   │   └── impl/SysDataFilterServiceImpl.java
│   └── controller/SysDataFilterController.java
└── resources/mapper/
    ├── SysDataFilterMapper.xml
    ├── SysUserGroupRelaMapper.xml (追加方法)
    ├── SysUserRoleRelaMapper.xml (追加方法)
    └── SysGroupMapper.xml (追加方法)

blink-datasource-starter/src/main/java/com/blink/datasource/
└── config/DataScopeAutoConfiguration.java
```

**前端（4个）：**
```
blink-base-web/src/
├── api/dataScope.ts
├── views/system/dataScope/
│   ├── index.vue
│   └── components/DataFilterFormDialog.vue
└── locales/
    ├── zh-cn.ts (修改)
    └── en-us.ts (修改)
```

### 修改文件（共7个）
```
blink-base/blink-base-app/src/main/java/com/blink/base/
├── constans/BaseErrCodeConstant.java
├── constans/RedisKeyConstans.java
├── mapper/SysUserGroupRelaMapper.java
├── mapper/SysUserRoleRelaMapper.java
├── mapper/SysGroupMapper.java
└── resources/mapper/*.xml (对应XML追加方法)

blink-base-web/src/
├── locales/zh-cn.ts
└── locales/en-us.ts
```