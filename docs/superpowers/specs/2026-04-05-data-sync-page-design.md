# 数据同步页面设计文档

## 概述

为 gateway-admin 添加数据同步功能页面，支持渠道、路由、配置三种数据的同步操作和一致性检查。

## 功能需求

1. **同步操作** - 支持全量同步渠道、路由、配置数据到网关
2. **一致性检查** - 检查各网关实例缓存与数据库数据是否一致
3. **单项同步** - 支持对差异项进行单条级别的同步
4. **同步日志** - 记录同步操作历史

## 架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端页面 (Vue3)                           │
│  ┌─────────────┐  ┌─────────────────────────────────────────┐   │
│  │ 同步操作区   │  │ 一致性检查矩阵                           │   │
│  │ [渠道同步]   │  │  实例\类型 │ 渠道 │ 路由 │ 配置 │        │   │
│  │ [路由同步]   │  │  gw-1     │ ✅   │ ✅   │ ⚠️   │        │   │
│  │ [配置同步]   │  │  gw-2     │ ✅   │ ❌   │ ✅   │        │   │
│  └─────────────┘  └─────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ 同步日志列表                                                 │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   gateway-admin 后端                            │
│  CacheStatusController                                          │
│  - GET  /cacheStatus/instances     获取网关实例列表               │
│  - POST /cacheStatus/check        执行一致性检查                 │
│  - POST /cacheStatus/sync         同步指定项到网关               │
│  - GET  /cacheStatus/logs         获取同步日志                   │
└─────────────────────────────────────────────────────────────────┘
                              │
            ┌─────────────────┼─────────────────┐
            ▼                 ▼                 ▼
   ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
   │ gateway-reactive│ │ gateway-reactive│ │    MySQL        │
   │ /actuator/      │ │ /actuator/      │ │  sync_log 表    │
   │ cache-status    │ │ cache-status    │ │  (同步日志)      │
   └─────────────────┘ └─────────────────┘ └─────────────────┘
```

## 详细设计

### 1. 网关侧 Actuator 端点

**端点路径：** `/actuator/cache-status?type={channel|route|config}`

**响应结构：**

```json
{
  "instanceId": "192.168.1.1:8002",
  "type": "channel",
  "timestamp": "2026-04-05T14:30:00",
  "items": [
    { "key": "channel_001", "checksum": "a1b2c3d4", "updateTime": "2026-04-05T14:20:00" },
    { "key": "channel_002", "checksum": "e5f6g7h8", "updateTime": "2026-04-05T14:15:00" }
  ]
}
```

**checksum 计算：** 对数据 JSON 序列化后计算 MD5

### 2. Admin 后端接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/cacheStatus/instances` | GET | 从 Nacos 获取网关实例列表 |
| `/cacheStatus/check` | POST | 执行一致性检查，返回差异矩阵 |
| `/cacheStatus/sync` | POST | 同步指定项到网关实例 |
| `/cacheStatus/logs` | GET | 分页查询同步日志 |

**CacheCheckReq：**

```java
public class CacheCheckReq {
    private String type;        // channel / route / config
    private List<String> keys;  // 可选，指定检查哪些key
}
```

**CacheCheckRsp：**

```java
public class CacheCheckRsp {
    private String type;
    private List<CacheCheckItem> dbItems;         // 数据库数据
    private List<InstanceCacheStatus> instances;  // 各实例状态
}
```

**CacheSyncReq：**

```java
public class CacheSyncReq {
    private String type;           // channel / route / config
    private List<String> keys;     // 指定同步的key
    private Boolean syncAll;       // true=全量同步
}
```

### 3. 前端页面

**路径：** `/dataSync`

**布局：**

1. **同步操作区** - 三个同步按钮（渠道/路由/配置）
2. **一致性检查区** - 类型选择 + 检查按钮 + 结果表格
3. **同步日志区** - 最近同步记录列表

**状态图标：**
- ✅ 一致 (MATCH) - 绿色
- ⚠️ 差异 (MISMATCH) - 橙色
- ❌ 缺失 (MISSING) - 红色

### 4. 菜单配置

**菜单SQL：**

```sql
-- 数据同步菜单 (在网关管理目录 menu_id=49 下)
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(50, '数据同步', 'DataSync', 2, 'Refresh', '/dataSync', 5, 0, 49, 2, 'views/dataSync/index.vue', NULL, 0, 'admin', NOW(), 0);

-- 数据同步按钮权限
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(51, '执行同步', 'SyncData', 3, NULL, NULL, 1, 0, 50, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(52, '一致性检查', 'CheckConsistency', 3, NULL, NULL, 2, 0, 50, 3, NULL, NULL, 0, 'admin', NOW(), 0);
```

### 5. 同步日志表

```sql
CREATE TABLE sync_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sync_type VARCHAR(32) NOT NULL COMMENT '同步类型: channel/route/config',
    sync_mode TINYINT DEFAULT 0 COMMENT '同步模式: 0-全量, 1-增量/单项',
    sync_keys TEXT COMMENT '同步的key列表(JSON数组)',
    operator VARCHAR(64) COMMENT '操作人',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-成功, 1-部分失败, 2-失败',
    instance_count INT COMMENT '同步实例数量',
    success_count INT COMMENT '成功实例数量',
    detail TEXT COMMENT '详细结果(JSON)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sync_type (sync_type),
    INDEX idx_create_time (create_time)
) COMMENT '数据同步日志表';
```

## 文件结构

### 网关侧 (blink-gateway-reactive)

```
src/main/java/com/blink/gateway/
├── endpoint/
│   └── CacheStatusEndpoint.java
└── dto/
    └── CacheStatusResponse.java
```

### Admin侧 (gateway-admin)

```
src/main/java/com/blink/gateway/admin/
├── controller/
│   └── CacheStatusController.java
├── service/
│   ├── CacheStatusService.java
│   └── impl/CacheStatusServiceImpl.java
├── mapper/
│   └── SyncLogMapper.java
├── entity/
│   └── SyncLogDO.java
└── dto/
    ├── req/
    │   ├── CacheCheckReq.java
    │   └── CacheSyncReq.java
    └── rsp/
        ├── CacheCheckRsp.java
        ├── InstanceCacheStatus.java
        ├── CacheItemStatus.java
        └── SyncLogRsp.java

src/main/resources/db/migration/
└── V20260405__add_sync_log_table.sql
```

### 前端 (gateway-admin-web)

```
src/
├── api/
│   └── dataSync.ts
├── views/
│   └── dataSync/
│       └── index.vue
├── router/
│   └── index.ts (修改)
└── locales/
    ├── zh-cn.ts (添加 dataSync)
    └── en-us.ts (添加 dataSync)
```

## 国际化文本

```typescript
// zh-cn.ts
dataSync: {
  title: '数据同步',
  syncOperation: '同步操作',
  channelSync: '渠道同步',
  routeSync: '路由同步',
  configSync: '配置同步',
  consistencyCheck: '一致性检查',
  startCheck: '开始检查',
  checkType: '检查类型',
  syncSelected: '同步选中项',
  syncAll: '全量同步',
  syncLog: '同步日志',
  syncTime: '同步时间',
  syncType: '同步类型',
  operator: '操作人',
  status: '状态',
  detail: '详情',
  match: '一致',
  mismatch: '差异',
  missing: '缺失',
  channel: '渠道',
  route: '路由',
  config: '配置',
  checkSuccess: '检查完成',
  syncSuccess: '同步成功',
  confirmSync: '确认同步选中的 {count} 项？',
  confirmSyncAll: '确认执行全量同步？',
}

// en-us.ts
dataSync: {
  title: 'Data Sync',
  syncOperation: 'Sync Operation',
  channelSync: 'Channel Sync',
  routeSync: 'Route Sync',
  configSync: 'Config Sync',
  consistencyCheck: 'Consistency Check',
  startCheck: 'Start Check',
  checkType: 'Check Type',
  syncSelected: 'Sync Selected',
  syncAll: 'Sync All',
  syncLog: 'Sync Log',
  syncTime: 'Sync Time',
  syncType: 'Sync Type',
  operator: 'Operator',
  status: 'Status',
  detail: 'Detail',
  match: 'Match',
  mismatch: 'Mismatch',
  missing: 'Missing',
  channel: 'Channel',
  route: 'Route',
  config: 'Config',
  checkSuccess: 'Check completed',
  syncSuccess: 'Sync successful',
  confirmSync: 'Confirm to sync {count} selected items?',
  confirmSyncAll: 'Confirm to perform full sync?',
}
```

## 交互流程

1. 用户进入数据同步页面
2. 点击"开始检查"执行一致性检查
3. 查看检查结果矩阵，发现差异项
4. 勾选差异项或点击单项"同步"按钮
5. 确认后执行同步
6. 同步完成后自动刷新检查结果
7. 查看同步日志记录