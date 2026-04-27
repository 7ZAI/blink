# 路由分组存储方式设计

## 背景

当前路由分组（`gateway_route_group`）表仅记录分组基本信息，未指定存储方式。新增分组后，需要手动在 Nacos 或 Redis 中创建对应的配置文件/缓存。

本设计为路由分组增加存储方式字段，实现新增分组时自动创建配置，删除分组时自动清理配置。

## 需求

1. 路由分组页面新增"存储方式"字段，支持 `nacos` 和 `redis` 两种选项
2. 新增分组时，根据存储方式自动创建初始配置：
   - Nacos: 发布配置文件 `gateway-routes-{groupKey}.json`，内容为空数组 `[]`
   - Redis: 写入 Hash Key `blink:gateway:routes:{groupKey}:default`，值为空
3. 删除分组时，同时删除对应的 Nacos 配置文件或 Redis 缓存
4. 存储方式创建后不可修改
5. 默认分组 `default` 也允许配置存储方式

## 设计方案

### 数据库变更

在 `gateway_route_group` 表添加 `storage_mode` 字段：

```sql
ALTER TABLE gateway_route_group
ADD COLUMN storage_mode VARCHAR(16) DEFAULT 'nacos' COMMENT '存储方式：nacos/redis'
AFTER group_name;
```

**字段说明**：
- `storage_mode`: 存储方式，可选值 `nacos` 或 `redis`
- 默认值: `nacos`

### 后端变更

#### 实体类

`GatewayRouteGroupDO` 新增字段：

```java
/**
 * 存储方式：nacos/redis
 */
@TableField("storage_mode")
private String storageMode;
```

#### DTO 变更

**AddRouteGroupReq** - 新增参数：

```java
/**
 * 存储方式：nacos/redis，默认 nacos
 */
@NotBlank(message = "存储方式不能为空")
private String storageMode = "nacos";
```

**RouteGroupVO** - 新增响应字段：

```java
/**
 * 存储方式：nacos/redis
 */
private String storageMode;
```

**UpdateRouteGroupReq** - 不包含 `storageMode` 字段（不可修改）

#### Service 逻辑

**新增分组 (`addRouteGroup`)**：

1. 校验 `storageMode` 必须为 `nacos` 或 `redis`
2. 开启事务
3. 保存数据库记录
4. 根据存储方式创建初始配置：
   - Nacos: 发布配置文件 `gateway-routes-{groupKey}.json`，内容为 `[]`，group 为 `DEFAULT_GROUP`
   - Redis: 写入 Hash Key `blink:gateway:routes:{groupKey}:default`，值为空 Hash
5. 如果配置创建失败，回滚事务
6. 提交事务

**删除分组 (`deleteRouteGroup`)**：

1. 检查是否有关联实例（现有逻辑）
2. 先删除配置：
   - Nacos: 删除配置文件 `gateway-routes-{groupKey}.json`
   - Redis: 删除 Hash Key `blink:gateway:routes:{groupKey}:default`
3. 如果配置删除失败，返回错误，不删除数据库记录
4. 开启事务
5. 删除数据库记录
6. 提交事务

**更新分组 (`updateRouteGroup`)**：

- `storageMode` 字段不允许修改
- 其他字段（`groupName`, `remark`）可正常修改

#### 常量定义

在 `RouteConstant` 中新增：

```java
/**
 * 存储方式 - Nacos
 */
String STORAGE_MODE_NACOS = "nacos";

/**
 * 存储方式 - Redis
 */
String STORAGE_MODE_REDIS = "redis";

/**
 * Nacos 路由配置文件前缀
 */
String NACOS_ROUTE_CONFIG_PREFIX = "gateway-routes";

/**
 * Nacos 路由配置文件后缀
 */
String NACOS_ROUTE_CONFIG_SUFFIX = ".json";

/**
 * Nacos 路由配置文件 Group
 */
String NACOS_ROUTE_CONFIG_GROUP = "DEFAULT_GROUP";
```

#### 错误码定义

在 `ErrCodeConstant` 中新增：

| 错误码 | 常量名 | 描述 |
|--------|--------|------|
| `GATE0030` | `STORAGE_MODE_INVALID` | 存储方式不合法 |
| `GATE0031` | `CREATE_ROUTE_CONFIG_FAILED` | 创建路由配置失败 |
| `GATE0032` | `DELETE_ROUTE_CONFIG_FAILED` | 删除路由配置失败 |

### 前端变更

#### 新增分组弹窗

在表单中添加"存储方式"单选框：

- 选项：Nacos（默认选中）、Redis
- 选项说明：
  - Nacos: 配置中心管理，支持版本历史，适合配置审计场景
  - Redis: 高性能存储，支持实时同步，适合高并发场景

#### 分组列表页

表格新增"存储方式"列，显示 `Nacos` 或 `Redis` 标签。

#### 国际化

新增翻译键：

```json
{
  "routeGroup": {
    "storageMode": "存储方式",
    "storageModePlaceholder": "请选择存储方式",
    "storageModeRequired": "存储方式不能为空",
    "nacos": "Nacos",
    "redis": "Redis",
    "nacosDesc": "配置中心管理，支持版本历史",
    "redisDesc": "高性能存储，支持实时同步"
  }
}
```

### 配置格式

#### Nacos 配置文件

- **dataId**: `gateway-routes-{groupKey}.json`
- **group**: `DEFAULT_GROUP`
- **初始内容**: `[]`

示例：分组 `group-a` 的配置文件：
- dataId: `gateway-routes-group-a.json`
- 内容: `[]`

#### Redis 缓存

- **Key**: `blink:gateway:routes:{groupKey}:default`
- **类型**: Hash
- **初始值**: 空 Hash（无字段）

示例：分组 `group-a` 的缓存 Key：
- Key: `blink:gateway:routes:group-a:default`

### 异常处理

| 场景 | 处理方式 |
|------|---------|
| storageMode 不是 nacos/redis | 抛出 `GATE0030` |
| Nacos 配置发布失败 | 记录日志，抛出 `GATE0031`，事务回滚 |
| Redis 写入失败 | 记录日志，抛出 `GATE0031`，事务回滚 |
| 删除分组时配置删除失败 | 记录日志，抛出 `GATE0032`，数据库不删除 |

### 默认分组处理

- `default` 分组在系统初始化时创建，`storage_mode` 默认为 `nacos`
- 允许通过 API 更新 `default` 分组的 `storageMode`（仅首次配置）
- 如果 `default` 分组切换存储方式，需要迁移现有配置数据

## 影响范围

- 数据库：`gateway_route_group` 表结构变更
- 后端：
  - `gateway-admin` 模块：实体类、DTO、Service、Controller
- 前端：
  - `gateway-admin` 应用：路由分组页面、API、国际化

## 兼容性

- 现有分组数据 `storage_mode` 默认为 `nacos`，与现有行为一致
- 现有 API 请求不包含 `storageMode` 时，使用默认值 `nacos`
