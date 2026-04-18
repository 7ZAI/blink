# 实例分组功能设计文档

## 一、需求概述

为 gateway-admin 添加实例分组管理功能，实现：
1. 实例分组的 CRUD 管理
2. 实例按分组独立存储路由配置
3. 路由推送时按分组隔离

### 核心业务逻辑

- **分组独立存储**：每个分组有独立的路由存储空间
- **路由推送隔离**：路由推送时，根据 `routesGroup` 只推送到该分组的实例
- **实例配置来源**：实例的 `storageMode` 决定该实例从哪个数据源（Redis/Nacos）读取路由配置
- **自动注册**：实例的 `groupKey` 和 `storageMode` 从配置文件中读取，随实例注册自动写入

## 二、数据库设计

### 1. 新增表 `gateway_instance_group`

```sql
CREATE TABLE IF NOT EXISTS `gateway_instance_group` (
    `group_id` INT NOT NULL AUTO_INCREMENT COMMENT '分组ID',
    `group_key` VARCHAR(64) NOT NULL COMMENT '分组标识（业务唯一键）',
    `group_name` VARCHAR(128) NOT NULL COMMENT '分组名称',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注说明',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`group_id`),
    UNIQUE KEY `uk_group_key` (`group_key`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关实例分组表';
```

### 2. 修改表 `gateway_instance`

新增字段：
```sql
ALTER TABLE `gateway_instance`
ADD COLUMN `group_key` VARCHAR(64) DEFAULT 'default' COMMENT '分组标识' AFTER `instance_id`,
ADD COLUMN `storage_mode` VARCHAR(16) DEFAULT 'redis' COMMENT '存储方式：redis/nacos' AFTER `group_key`,
ADD INDEX `idx_group_key` (`group_key`);
```

### 3. 初始化数据

插入默认分组：
```sql
INSERT INTO `gateway_instance_group` (`group_key`, `group_name`, `status`, `remark`)
VALUES ('default', '默认分组', 1, '系统默认分组');
```

## 三、后端实现

### 1. 实体类

**文件路径**：`blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/entity/GatewayInstanceGroupDO.java`

```java
@Data
@TableName("gateway_instance_group")
public class GatewayInstanceGroupDO implements Serializable {
    @TableId(type = IdType.AUTO)
    private Integer groupId;
    private String groupKey;
    private String groupName;
    private Byte status;
    private String remark;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
```

### 2. DTO 设计

| DTO 类 | 用途 | 关键字段 |
|--------|------|----------|
| `QueryInstanceGroupReq` | 分页查询请求 | 继承 Page，groupKey, groupName, status |
| `AddInstanceGroupReq` | 新增分组请求 | groupKey, groupName, remark |
| `UpdateInstanceGroupReq` | 更新分组请求 | groupId, groupName, status, remark |
| `GetInstanceGroupReq` | 获取详情请求 | groupId |
| `DeleteInstanceGroupReq` | 删除分组请求 | groupId |
| `InstanceGroupVO` | 分组视图对象 | 全部字段 |
| `InstanceGroupListRsp` | 分页列表响应 | 继承 PageDTO<InstanceGroupVO> |

### 3. Mapper

**文件路径**：`blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/mapper/GatewayInstanceGroupMapper.java`

继承 `BaseMapper<GatewayInstanceGroupDO>`

### 4. Service

**接口**：`GatewayInstanceGroupService`
**实现**：`GatewayInstanceGroupServiceImpl`

核心方法：
- `InstanceGroupListRsp queryInstanceGroupList(QueryInstanceGroupReq req)` - 分页查询
- `InstanceGroupVO getInstanceGroupDetail(GetInstanceGroupReq req)` - 获取详情
- `void addInstanceGroup(AddInstanceGroupReq req)` - 新增分组
- `void updateInstanceGroup(UpdateInstanceGroupReq req)` - 更新分组
- `void deleteInstanceGroup(DeleteInstanceGroupReq req)` - 删除分组（需校验关联实例）

### 5. Controller

**文件路径**：`blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/InstanceGroupController.java`

| 接口路径 | 方法 | 说明 |
|----------|------|------|
| `/instanceGroup/queryInstanceGroupList` | POST | 分页查询分组列表 |
| `/instanceGroup/getInstanceGroupDetail` | POST | 获取分组详情 |
| `/instanceGroup/addInstanceGroup` | POST | 新增分组 |
| `/instanceGroup/updateInstanceGroup` | POST | 更新分组 |
| `/instanceGroup/deleteInstanceGroup` | POST | 删除分组 |

### 6. 修改现有服务

#### 6.1 修改 `GatewayInstanceDO`

新增字段：
- `groupKey` - 分组标识
- `storageMode` - 存储方式

#### 6.2 修改 `GatewayInstanceServiceImpl`

实例注册/同步时，从实例元数据或配置读取 `groupKey` 和 `storageMode`：
- 元数据来源：Nacos 注册时携带的 metadata
- 默认值：`groupKey=default`, `storageMode=redis`

#### 6.3 修改 `RoutePushServiceImpl`

推送路由时，根据 `routesGroup` 过滤目标实例：
```java
// 广播模式下，按 routesGroup 过滤实例
List<GatewayInstanceVO> targetInstances = instances.stream()
    .filter(inst -> inst.getGroupKey().equals(routesGroup))
    .filter(inst -> inst.getStatus().equals(INSTANCE_STATUS_ONLINE))
    .toList();
```

### 7. 错误码定义

在 `ErrCodeConstant` 中新增：
```java
String GROUP_NOT_EXIST = "GATE0030";
String GROUP_KEY_EXISTS = "GATE0031";
String GROUP_HAS_INSTANCES = "GATE0032";
```

## 四、前端实现

### 1. 新增页面

**文件路径**：`frontend/packages/gateway-admin/src/views/instanceGroup/index.vue`

功能：
- 分组列表展示（表格）
- 搜索：分组标识、分组名称
- 操作：新增、编辑、删除
- 分页

### 2. 新增 API

**文件路径**：`frontend/packages/gateway-admin/src/api/instanceGroup.ts`

```typescript
export interface InstanceGroup {
  groupId: number
  groupKey: string
  groupName: string
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export const queryInstanceGroupList = (params) => ...
export const getInstanceGroupDetail = (params) => ...
export const addInstanceGroup = (params) => ...
export const updateInstanceGroup = (params) => ...
export const deleteInstanceGroup = (params) => ...
```

### 3. 修改实例管理页面

**文件路径**：`frontend/packages/gateway-admin/src/views/instance/index.vue`

修改点：
1. 搜索区增加分组筛选下拉框
2. 表格增加「分组」「存储方式」列
3. 删除/修改实例 API 的 DTO 中增加 groupKey 和 storageMode 字段

### 4. 修改实例 API

**文件路径**：`frontend/packages/gateway-admin/src/api/instance.ts`

修改 `InstanceInfo` 接口，新增字段：
```typescript
export interface InstanceInfo {
  // ... 现有字段
  groupKey?: string
  storageMode?: string
}
```

新增分组下拉列表接口：
```typescript
export const getInstanceGroupOptions = (): Promise<InstanceGroup[]> => ...
```

### 5. 国际化

**中文** (`zh-cn.ts`)：
```typescript
instanceGroup: {
  title: '实例分组',
  groupKey: '分组标识',
  groupName: '分组名称',
  // ...
}
```

**英文** (`en-us.ts`)：
```typescript
instanceGroup: {
  title: 'Instance Group',
  groupKey: 'Group Key',
  groupName: 'Group Name',
  // ...
}
```

## 五、菜单配置

在「监控中心」下新增菜单，位置在「实例管理」之前：

```
监控中心
├── 实例分组  (新增) - /monitor/instanceGroup
├── 实例管理  - /monitor/instance
├── 熔断监控  - /monitor/circuitBreaker
└── ...
```

菜单数据插入脚本：
```sql
INSERT INTO sys_menu (parent_id, menu_name, menu_type, route_path, component_path, perms, order_num, status)
VALUES (监控中心ID, '实例分组', 'MENU', 'instanceGroup', 'instanceGroup/index', 'monitor:instanceGroup:list', 1, 1);
```

## 六、实现顺序

1. **数据库迁移脚本** - 创建表、新增字段、初始化数据
2. **后端实体类** - GatewayInstanceGroupDO、修改 GatewayInstanceDO
3. **后端 DTO** - 请求/响应类
4. **后端 Mapper** - 数据访问层
5. **后端 Service** - 业务逻辑层
6. **后端 Controller** - 接口层
7. **修改现有服务** - 实例注册、路由推送逻辑
8. **前端 API** - 接口定义
9. **前端页面** - 分组管理页面
10. **修改实例管理** - 增加分组筛选和显示
11. **国际化** - 中英文文案
12. **菜单数据** - 数据库菜单配置
