# Gateway Admin 路由管理增强设计文档

## 概述

增强 gateway-admin 的路由管理功能，支持：
- 运行时切换路由存储方式（Nacos / Redis）
- 多实例路由推送（广播模式或指定实例）
- Nacos 路由的完整增删改查

---

## 1. 功能需求

### 1.1 存储方式切换

| 功能 | 描述 |
|------|------|
| 运行时切换 | 页面下拉选择存储方式，无需重启服务 |
| 路由组管理 | Redis 模式支持路由分组（routeSuffix） |
| Nacos 配置管理 | Nacos 模式支持 dataId/group 配置 |

### 1.2 多实例推送

| 模式 | 描述 |
|------|------|
| 广播推送 | 推送到所有在线 gateway-reactive 实例 |
| 指定实例推送 | 手动勾选目标实例，仅同步到选中实例 |

### 1.3 Nacos 路由管理

| 功能 | 描述 |
|------|------|
| 查询路由 | 从 Nacos Config 读取路由 JSON 配置 |
| 新增路由 | 通过 Nacos Config API 发布更新 |
| 编辑路由 | 修改并重新发布配置 |
| 删除路由 | 从配置中移除路由并发布 |

---

## 2. 前端设计

### 2.1 页面布局

```
┌─────────────────────────────────────────────────────────────┐
│ 路由管理                                                     │
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ [存储方式: Redis ▼] [路由组: default ▼]                  │ │
│ │ [查询] [重置]                                            │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ [新增路由] [刷新路由] [同步到实例]                        │ │
│ │                                                         │ │
│ │ ┌─────────────────────────────────────────────────────┐ │ │
│ │ │ 路由ID │ URI │ 断言 │ 过滤器 │ 顺序 │ 操作          │ │ │
│ │ ├─────────────────────────────────────────────────────┤ │ │
│ │ │ route-001 │ lb://base-app │ Path=/api/** │ ... │ 编辑│删除│ │ │
│ │ └─────────────────────────────────────────────────────┘ │ │
│ │                                                         │ │
│ │ 分页: 共 10 条                                           │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 存储方式切换组件

**位置**：搜索区域，与路由组下拉并列

**选项值**：
- `redis` - Redis Hash 存储
- `nacos` - Nacos Config 存储

**交互逻辑**：
1. 切换存储方式时，自动刷新路由列表
2. Redis 模式显示路由组下拉，Nacos 模式隐藏路由组（或显示 dataId/group）
3. 存储方式保存到 localStorage，下次打开自动恢复

### 2.3 同步到实例弹窗

**触发时机**：
- 新增/编辑路由保存成功后
- 点击"同步到实例"按钮

**弹窗内容**：
```
┌─────────────────────────────────────────────────────────────┐
│ 同步到网关实例                                         [×] │
├─────────────────────────────────────────────────────────────┤
│ 选择推送方式                                                │
│ ┌─────────────────────────┐ ┌─────────────────────────────┐│
│ │ ◉ 广播推送              │ │ ○ 指定实例                  ││
│ │   推送到所有在线实例     │ │   手动选择目标实例          ││
│ └─────────────────────────┘ └─────────────────────────────┘│
│                                                             │
│ 目标实例（仅指定实例模式显示）                              │
│ ┌─────────────────────────────────────────────────────────┐│
│ │ ☑ gateway-reactive-01 (192.168.1.10:8080) - 在线       ││
│ │ ☑ gateway-reactive-02 (192.168.1.11:8080) - 在线       ││
│ │ ☐ gateway-reactive-03 (192.168.1.12:8080) - 离线       ││
│ └─────────────────────────────────────────────────────────┘│
│                                                             │
│                                    [取消] [确认推送]        │
└─────────────────────────────────────────────────────────────┘
```

**交互逻辑**：
- 默认选中"广播推送"，自动勾选所有在线实例
- 切换到"指定实例"时，清空勾选，用户手动选择
- 离线实例禁用勾选，灰色显示
- 广播模式直接提交，指定实例模式需至少勾选一个实例

---

## 3. 后端设计

### 3.1 新增接口

#### 3.1.1 存储方式相关

```java
// 查询支持的存储方式列表
@PostMapping("/route/getStorageModes")
ResponseDTO<List<StorageModeVO>> getStorageModes();

// 查询 Nacos 路由配置列表
@PostMapping("/route/getNacosRouteConfigs")
ResponseDTO<List<NacosConfigVO>> getNacosRouteConfigs();
```

#### 3.1.2 实例同步相关

```java
// 查询在线网关实例列表
@PostMapping("/route/getOnlineGatewayInstances")
ResponseDTO<List<GatewayInstanceVO>> getOnlineGatewayInstances();

// 同步路由到指定实例
@PostMapping("/route/syncRoutesToInstances")
ResponseDTO<EmptyBody> syncRoutesToInstances(@RequestBody RequestDTO<SyncRoutesReq> reqDto);
```

#### 3.1.3 Nacos 路由管理

```java
// 查询 Nacos 路由列表
@PostMapping("/route/getNacosRouteList")
ResponseDTO<QueryGateWayRoutesRsp> getNacosRouteList(@RequestBody RequestDTO<QueryNacosRouteReq> reqDto);

// 保存 Nacos 路由
@PostMapping("/route/saveNacosRoute")
ResponseDTO<EmptyBody> saveNacosRoute(@RequestBody RequestDTO<SaveNacosRouteReq> reqDto);

// 删除 Nacos 路由
@PostMapping("/route/deleteNacosRoute")
ResponseDTO<EmptyBody> deleteNacosRoute(@RequestBody RequestDTO<DeleteNacosRouteReq> reqDto);
```

### 3.2 新增 DTO

#### 3.2.1 SyncRoutesReq

```java
@Getter
@Setter
public class SyncRoutesReq implements Serializable {
    
    /** 存储方式: redis / nacos */
    private String storageMode;
    
    /** 路由组（Redis模式必填） */
    private String routesGroup;
    
    /** Nacos dataId（Nacos模式必填） */
    private String dataId;
    
    /** Nacos group（Nacos模式必填） */
    private String group;
    
    /** 推送模式: broadcast / specified */
    private String pushMode;
    
    /** 目标实例ID列表（指定实例模式必填） */
    private List<String> targetInstanceIds;
    
    /** 待同步的路由ID列表（可选，为空则同步全部） */
    private List<String> routeIds;
}
```

#### 3.2.2 SaveNacosRouteReq

```java
@Getter
@Setter
public class SaveNacosRouteReq implements Serializable {
    
    /** Nacos dataId */
    private String dataId;
    
    /** Nacos group */
    private String group;
    
    /** 路由定义列表 */
    private List<RouteDefinitionReq> routes;
}
```

### 3.3 服务层改造

#### 3.3.1 RouteService 接口扩展

```java
public interface RouteService {
    
    // 现有 Redis 路由方法保持不变
    ResponseDTO<QueryGateWayRoutesRsp> getRouteList(QueryRouteReq req);
    ResponseDTO<EmptyBody> saveRoute(SaveRouteReq req);
    ResponseDTO<EmptyBody> deleteRoute(DeleteRouteReq req);
    ResponseDTO<EmptyBody> refreshRoutes();
    
    // 新增 Nacos 路由方法
    ResponseDTO<QueryGateWayRoutesRsp> getNacosRouteList(QueryNacosRouteReq req);
    ResponseDTO<EmptyBody> saveNacosRoute(SaveNacosRouteReq req);
    ResponseDTO<EmptyBody> deleteNacosRoute(DeleteNacosRouteReq req);
    
    // 新增实例同步方法
    ResponseDTO<List<StorageModeVO>> getStorageModes();
    ResponseDTO<List<GatewayInstanceVO>> getOnlineGatewayInstances();
    ResponseDTO<EmptyBody> syncRoutesToInstances(SyncRoutesReq req);
}
```

#### 3.3.2 NacosRouteServiceImpl

新增专门处理 Nacos 路由的服务实现：

```java
@Service
@Slf4j
public class NacosRouteServiceImpl implements NacosRouteService {
    
    @Resource
    private NacosConfigManager nacosConfigManager;
    
    @Resource
    private GateWayStreamMessageProducer messageProducer;
    
    @Override
    public ResponseDTO<QueryGateWayRoutesRsp> getNacosRouteList(QueryNacosRouteReq req) {
        // 1. 从 Nacos 获取配置内容
        // 2. 解析 JSON 为 RouteDefinition 列表
        // 3. 返回分页结果
    }
    
    @Override
    public ResponseDTO<EmptyBody> saveNacosRoute(SaveNacosRouteReq req) {
        // 1. 从 Nacos 获取当前配置
        // 2. 解析并更新路由列表
        // 3. 发布新配置到 Nacos
        // 4. 发送 Stream 通知同步
    }
    
    @Override
    public ResponseDTO<EmptyBody> deleteNacosRoute(DeleteNacosRouteReq req) {
        // 1. 从 Nacos 获取当前配置
        // 2. 移除指定路由
        // 3. 发布新配置到 Nacos
        // 4. 发送 Stream 通知同步
    }
}
```

### 3.4 Redis Stream 消息扩展

#### 3.4.1 RouteSyncMsg 扩展

```java
@Getter
@Setter
public class RouteSyncMsg implements Serializable {
    
    /** 动态路由 Redis Key */
    private String dynamicRouteKey;
    
    /** 存储方式: redis / nacos */
    private String storageMode;
    
    /** Nacos dataId（Nacos模式） */
    private String dataId;
    
    /** Nacos group（Nacos模式） */
    private String group;
    
    /** 推送模式: broadcast / specified */
    private String pushMode;
    
    /** 目标实例ID列表（指定实例模式） */
    private List<String> targetInstanceIds;
}
```

### 3.5 gateway-reactive 端改造

#### 3.5.1 CommonEventStreamListener 扩展

```java
// handlerEvent 方法中路由同步处理
if (message.getPayload() instanceof RouteSyncMsg routeEvent) {
    
    // 检查推送模式
    if ("specified".equals(routeEvent.getPushMode())) {
        // 指定实例模式：检查当前实例是否在目标列表中
        String currentInstanceId = appName + ":" + instanceId;
        if (!routeEvent.getTargetInstanceIds().contains(currentInstanceId)) {
            log.info("[RouteSync] 跳过同步，当前实例不在目标列表中 | instanceId: {}", currentInstanceId);
            smr.setHandledResult(true);
            return Mono.just(smr);
        }
    }
    
    // 发布事件更新路由
    publisher.publishEvent(new RefreshRoutesEvent(this));
    smr.setHandledResult(true);
    return Mono.just(smr);
}
```

---

## 4. 数据流设计

### 4.1 Redis 模式数据流

```
┌─────────────┐     ┌─────────────┐     ┌─────────────────┐
│   前端页面   │────▶│ RouteService │────▶│ Redis Hash      │
│             │     │             │     │ routes:{suffix} │
└─────────────┘     └─────────────┘     └─────────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │ Redis Stream │────▶ gateway-reactive (广播/指定)
                    │ GATEWAY_EVENT│
                    └─────────────┘
```

### 4.2 Nacos 模式数据流

```
┌─────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   前端页面   │────▶│NacosRouteService│────▶│ Nacos Config    │
│             │     │                 │     │ gateway-routes  │
└─────────────┘     └─────────────────┘     └─────────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │ Redis Stream │────▶ gateway-reactive (广播/指定)
                    │ GATEWAY_EVENT│
                    └─────────────┘
```

---

## 5. 实现要点

### 5.1 Nacos Config 操作

使用 `NacosConfigManager.getConfigService()` API：

```java
// 获取配置
String config = configService.getConfig(dataId, group, 5000);

// 发布配置
configService.publishConfig(dataId, group, newConfigContent);
```

### 5.2 实例状态获取

复用现有 `GatewayInstanceServiceImpl`，筛选在线实例：

```java
List<ServiceInstance> instances = discoveryClient.getInstances("gateway-app");
// 过滤在线状态
```

### 5.3 消息路由实现

Redis Stream 使用独立消费者组实现广播，扩展消息字段实现指定实例过滤。

---

## 6. 验收标准

| 功能 | 验收点 |
|------|--------|
| 存储方式切换 | 切换后路由列表正确刷新，localStorage 记忆选择 |
| Redis 路由管理 | 增删改查功能正常，路由组筛选有效 |
| Nacos 路由管理 | 增删改查功能正常，配置同步到 Nacos |
| 广播推送 | 所有在线实例收到同步消息，路由生效 |
| 指定实例推送 | 仅选中实例收到消息，其他实例不更新 |
| 离线实例处理 | 离线实例不可勾选，推送时跳过 |

---

## 7. 模块影响范围

### 7.1 后端模块

| 模块 | 改动内容 |
|------|----------|
| gateway-admin | 新增接口、DTO、Service |
| blink-gateway-admin-api-dubbo | RouteSyncMsg 扩展 |
| blink-gateway-reactive | CommonEventStreamListener 扩展 |

### 7.2 前端模块

| 文件 | 改动内容 |
|------|----------|
| views/route/index.vue | 存储方式切换、同步弹窗 |
| api/route.ts | 新增 API 接口 |
| types/route.ts | 新增类型定义 |
| components/SyncInstanceDialog.vue | 新增同步弹窗组件 |

---

## 8. 风险评估

| 风险 | 级别 | 缓解措施 |
|------|------|----------|
| Nacos Config 发布失败 | 中 | 重试机制 + 错误记录 |
| 大量路由配置性能 | 低 | 分页查询 + 懒加载 |
| 实例离线时推送丢失 | 低 | 离线实例禁用勾选 |
| 消息乱序 | 低 | 版本号机制已存在 |

---

## 附录：接口详细定义

### A.1 getStorageModes 响应

```json
{
  "code": "00000",
  "msg": "success",
  "body": {
    "rows": [
      { "mode": "redis", "name": "Redis 存储", "description": "路由存储在 Redis Hash" },
      { "mode": "nacos", "name": "Nacos 配置", "description": "路由存储在 Nacos Config" }
    ]
  }
}
```

### A.2 syncRoutesToInstances 请求

```json
{
  "requestId": "uuid",
  "body": {
    "storageMode": "redis",
    "routesGroup": "default",
    "pushMode": "specified",
    "targetInstanceIds": ["gateway-reactive:01", "gateway-reactive:02"],
    "routeIds": ["route-001", "route-002"]
  }
}
```

### A.3 getNacosRouteList 请求

```json
{
  "requestId": "uuid",
  "body": {
    "dataId": "gateway-routes.json",
    "group": "DEFAULT_GROUP",
    "pageNum": 1,
    "pageSize": 10
  }
}
```