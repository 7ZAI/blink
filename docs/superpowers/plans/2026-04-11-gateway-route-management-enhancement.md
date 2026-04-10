# Gateway Admin 路由管理增强实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 增强路由管理功能，支持存储方式运行时切换、多实例推送和 Nacos 路由完整管理。

**Architecture:** 后端扩展 DTO/Service/Controller 实现 Nacos 路由管理和实例同步，扩展 RouteSyncMsg 支持指定实例推送，前端新增存储方式切换和同步弹窗组件。

**Tech Stack:** Spring Boot 3.2 + Nacos Config API + Redis Stream + Vue 3 + TypeScript

---

## 文件结构

### 后端新增/修改文件

| 文件 | 责任 |
|------|------|
| `blink-gateway-admin-api-dubbo/.../dto/RouteSyncMsg.java` | 扩展消息字段支持指定实例推送 |
| `gateway-admin/.../dto/req/SyncRoutesReq.java` | 同步路由请求 DTO |
| `gateway-admin/.../dto/req/QueryNacosRouteReq.java` | Nacos 路由查询请求 |
| `gateway-admin/.../dto/req/SaveNacosRouteReq.java` | Nacos 路由保存请求 |
| `gateway-admin/.../dto/req/DeleteNacosRouteReq.java` | Nacos 路由删除请求 |
| `gateway-admin/.../dto/vo/StorageModeVO.java` | 存储方式 VO |
| `gateway-admin/.../service/RouteService.java` | 接口扩展 |
| `gateway-admin/.../service/impl/RouteServiceImpl.java` | Redis 路由实现扩展 |
| `gateway-admin/.../service/NacosRouteService.java` | Nacos 路由服务接口 |
| `gateway-admin/.../service/impl/NacosRouteServiceImpl.java` | Nacos 路由实现 |
| `gateway-admin/.../controller/RouteController.java` | 新增接口 |
| `gateway-admin/.../producer/GateWayStreamMessageProducer.java` | 扩展推送方法 |
| `blink-gateway-reactive/.../listener/CommonEventStreamListener.java` | 实例过滤逻辑 |

### 前端新增/修改文件

| 文件 | 责任 |
|------|------|
| `frontend/.../api/route.ts` | 新增 API 接口 |
| `frontend/.../types/route.ts` | 新增类型定义 |
| `frontend/.../components/SyncInstanceDialog.vue` | 同步实例弹窗 |
| `frontend/.../views/route/index.vue` | 存储方式切换和弹窗集成 |

---

## Task 1: 扩展 RouteSyncMsg 消息

**Files:**
- Modify: `blink-gateway/blink-gateway-admin-api-dubbo/src/main/java/com/blink/gateway/dto/RouteSyncMsg.java`

- [ ] **Step 1: 扩展 RouteSyncMsg 类**

```java
package com.blink.gateway.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 路由同步消息 DTO
 * 支持广播模式和指定实例推送
 *
 * @author binblink
 */
@Getter
@Setter
public class RouteSyncMsg implements Serializable {
    @Serial
    private static final long serialVersionUID = 6707034006158344769L;

    /**
     * 动态路由 Redis Key（Redis模式）
     */
    private String dynamicRouteKey;

    /**
     * 存储方式: redis / nacos
     */
    private String storageMode;

    /**
     * Nacos dataId（Nacos模式）
     */
    private String dataId;

    /**
     * Nacos group（Nacos模式）
     */
    private String group;

    /**
     * 推送模式: broadcast / specified
     */
    private String pushMode;

    /**
     * 目标实例ID列表（指定实例模式）
     */
    private List<String> targetInstanceIds;

    @Override
    public String toString() {
        return "RouteSyncMsg{" +
                "dynamicRouteKey='" + dynamicRouteKey + '\'' +
                ", storageMode='" + storageMode + '\'' +
                ", dataId='" + dataId + '\'' +
                ", group='" + group + '\'' +
                ", pushMode='" + pushMode + '\'' +
                ", targetInstanceIds=" + targetInstanceIds +
                '}';
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew :blink-gateway:blink-gateway-admin-api-dubbo:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add blink-gateway/blink-gateway-admin-api-dubbo/src/main/java/com/blink/gateway/dto/RouteSyncMsg.java
git commit -m "feat(gateway): 扩展 RouteSyncMsg 支持存储方式和指定实例推送

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 2: 新增后端 DTO 类

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/SyncRoutesReq.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/QueryNacosRouteReq.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/SaveNacosRouteReq.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/DeleteNacosRouteReq.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/StorageModeVO.java`

- [ ] **Step 1: 创建 SyncRoutesReq**

```java
package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 同步路由到实例请求 DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class SyncRoutesReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 存储方式: redis / nacos
     */
    private String storageMode;

    /**
     * 路由组（Redis模式必填）
     */
    private String routesGroup;

    /**
     * Nacos dataId（Nacos模式必填）
     */
    private String dataId;

    /**
     * Nacos group（Nacos模式必填）
     */
    private String group;

    /**
     * 推送模式: broadcast / specified
     */
    private String pushMode;

    /**
     * 目标实例ID列表（指定实例模式必填）
     */
    private List<String> targetInstanceIds;

    /**
     * 待同步的路由ID列表（可选，为空则同步全部）
     */
    private List<String> routeIds;
}
```

- [ ] **Step 2: 创建 QueryNacosRouteReq**

```java
package com.blink.gateway.admin.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * Nacos 路由查询请求 DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class QueryNacosRouteReq extends Page {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Nacos dataId
     */
    private String dataId;

    /**
     * Nacos group
     */
    private String group;
}
```

- [ ] **Step 3: 创建 SaveNacosRouteReq**

```java
package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Nacos 路由保存请求 DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class SaveNacosRouteReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Nacos dataId
     */
    private String dataId;

    /**
     * Nacos group
     */
    private String group;

    /**
     * 路由定义列表
     */
    private List<RouteDefinitionReq> routes;
}
```

- [ ] **Step 4: 创建 DeleteNacosRouteReq**

```java
package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Nacos 路由删除请求 DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class DeleteNacosRouteReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Nacos dataId
     */
    private String dataId;

    /**
     * Nacos group
     */
    private String group;

    /**
     * 待删除的路由ID列表
     */
    private List<String> routeIds;
}
```

- [ ] **Step 5: 创建 StorageModeVO**

```java
package com.blink.gateway.admin.dto.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 存储方式 VO
 *
 * @author binblink
 */
@Getter
@Setter
public class StorageModeVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 存储模式标识
     */
    private String mode;

    /**
     * 存储方式名称
     */
    private String name;

    /**
     * 存储方式描述
     */
    private String description;
}
```

- [ ] **Step 6: 编译验证**

Run: `./gradlew :blink-gateway:gateway-admin:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/
git commit -m "feat(gateway): 新增路由同步和 Nacos 路由管理 DTO

新增: SyncRoutesReq, QueryNacosRouteReq, SaveNacosRouteReq, DeleteNacosRouteReq, StorageModeVO

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 3: 扩展 RouteService 接口

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/RouteService.java`

- [ ] **Step 1: 扩展接口方法**

```java
package com.blink.gateway.admin.service;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.DeleteNacosRouteReq;
import com.blink.gateway.admin.dto.req.DeleteRouteReq;
import com.blink.gateway.admin.dto.req.QueryNacosRouteReq;
import com.blink.gateway.admin.dto.req.QueryRouteReq;
import com.blink.gateway.admin.dto.req.SaveNacosRouteReq;
import com.blink.gateway.admin.dto.req.SaveRouteReq;
import com.blink.gateway.admin.dto.req.SyncRoutesReq;
import com.blink.gateway.admin.dto.rsp.QueryGateWayRoutesRsp;
import com.blink.gateway.admin.dto.vo.StorageModeVO;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;

import java.util.List;

/**
 * 路由管理服务接口
 *
 * @author binblink
 */
public interface RouteService {

    // ========== Redis 路由管理 ==========

    /**
     * 查询 Redis 路由列表
     */
    ResponseDTO<QueryGateWayRoutesRsp> getRouteList(QueryRouteReq req);

    /**
     * 保存 Redis 路由
     */
    ResponseDTO<EmptyBody> saveRoute(SaveRouteReq req);

    /**
     * 删除 Redis 路由
     */
    ResponseDTO<EmptyBody> deleteRoute(DeleteRouteReq req);

    /**
     * 刷新路由缓存
     */
    ResponseDTO<EmptyBody> refreshRoutes();

    // ========== Nacos 路由管理 ==========

    /**
     * 查询 Nacos 路由列表
     */
    ResponseDTO<QueryGateWayRoutesRsp> getNacosRouteList(QueryNacosRouteReq req);

    /**
     * 保存 Nacos 路由
     */
    ResponseDTO<EmptyBody> saveNacosRoute(SaveNacosRouteReq req);

    /**
     * 删除 Nacos 路由
     */
    ResponseDTO<EmptyBody> deleteNacosRoute(DeleteNacosRouteReq req);

    // ========== 存储方式和实例同步 ==========

    /**
     * 获取支持的存储方式列表
     */
    ResponseDTO<List<StorageModeVO>> getStorageModes();

    /**
     * 获取在线网关实例列表
     */
    ResponseDTO<List<GatewayInstanceVO>> getOnlineGatewayInstances();

    /**
     * 同步路由到指定实例
     */
    ResponseDTO<EmptyBody> syncRoutesToInstances(SyncRoutesReq req);
}
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew :blink-gateway:gateway-admin:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/RouteService.java
git commit -m "feat(gateway): 扩展 RouteService 接口支持 Nacos 和实例同步

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 4: 扩展 GateWayStreamMessageProducer

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/producer/GateWayStreamMessageProducer.java`

- [ ] **Step 1: 新增路由同步方法**

在 `GateWayStreamMessageProducer.java` 的 `routesOnChange` 方法后添加新方法：

```java
/**
 * 路由同步（支持指定实例推送）
 *
 * @param routeSyncMsg 路由同步消息
 */
public void routesOnChangeWithTarget(RouteSyncMsg routeSyncMsg) {
    StreamMessage<RouteSyncMsg> msg = StreamMessage.of(GATEWAY_STREAM_EVENT, MessageType.EVENT, routeSyncMsg);
    msg.setSender(appName);
    msg.setPayloadClass(RouteSyncMsg.class.getName());
    sendAndRecord(msg, routeSyncMsg);
}
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew :blink-gateway:gateway-admin:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/producer/GateWayStreamMessageProducer.java
git commit -m "feat(gateway): 扩展 GateWayStreamMessageProducer 支持指定实例推送

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 5: 扩展 RouteServiceImpl

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/RouteServiceImpl.java`

- [ ] **Step 1: 新增依赖注入和常量**

在类开头添加：

```java
@Resource
private GatewayInstanceService gatewayInstanceService;

private static final String STORAGE_MODE_REDIS = "redis";
private static final String STORAGE_MODE_NACOS = "nacos";
private static final String PUSH_MODE_BROADCAST = "broadcast";
private static final String PUSH_MODE_SPECIFIED = "specified";
```

- [ ] **Step 2: 实现 getStorageModes**

```java
@Override
public ResponseDTO<List<StorageModeVO>> getStorageModes() {
    List<StorageModeVO> modes = new ArrayList<>();

    StorageModeVO redisMode = new StorageModeVO();
    redisMode.setMode(STORAGE_MODE_REDIS);
    redisMode.setName("Redis 存储");
    redisMode.setDescription("路由存储在 Redis Hash");
    modes.add(redisMode);

    StorageModeVO nacosMode = new StorageModeVO();
    nacosMode.setMode(STORAGE_MODE_NACOS);
    nacosMode.setName("Nacos 配置");
    nacosMode.setDescription("路由存储在 Nacos Config");
    modes.add(nacosMode);

    return ResponseDTO.newSuccessInstance(modes);
}
```

- [ ] **Step 3: 实现 getOnlineGatewayInstances**

```java
@Override
public ResponseDTO<List<GatewayInstanceVO>> getOnlineGatewayInstances() {
    ResponseDTO<GatewayInstanceListRsp> rsp = gatewayInstanceService.getGatewayInstances();
    List<GatewayInstanceVO> onlineInstances = rsp.getBody().getInstances().stream()
            .filter(instance -> instance.getStatus() == 0) // STATUS_ONLINE
            .collect(Collectors.toList());

    log.info("[Route] 获取在线网关实例成功 | count: {}", onlineInstances.size());
    return ResponseDTO.newSuccessInstance(onlineInstances);
}
```

- [ ] **Step 4: 实现 syncRoutesToInstances**

```java
@Override
public ResponseDTO<EmptyBody> syncRoutesToInstances(SyncRoutesReq req) {
    try {
        RouteSyncMsg routeSyncMsg = new RouteSyncMsg();
        routeSyncMsg.setStorageMode(req.getStorageMode());
        routeSyncMsg.setPushMode(req.getPushMode());
        routeSyncMsg.setTargetInstanceIds(req.getTargetInstanceIds());

        if (STORAGE_MODE_REDIS.equals(req.getStorageMode())) {
            String routesGroup = req.getRoutesGroup();
            if (StrUtil.isBlank(routesGroup)) {
                routesGroup = "default";
            }
            routeSyncMsg.setDynamicRouteKey(GATEWAY_DYNAMIC_ROUTES + ":" + routesGroup);
        } else if (STORAGE_MODE_NACOS.equals(req.getStorageMode())) {
            routeSyncMsg.setDataId(req.getDataId());
            routeSyncMsg.setGroup(req.getGroup());
        }

        messageProducer.routesOnChangeWithTarget(routeSyncMsg);

        log.info("[Route] 同步路由到实例成功 | storageMode: {}, pushMode: {}, targetInstances: {}",
                req.getStorageMode(), req.getPushMode(), req.getTargetInstanceIds());

        return ResponseDTO.newSuccessInstance();
    } catch (Exception e) {
        log.error("[Route] 同步路由到实例失败 | error: {}", e.getMessage(), e);
        throw new BlinkException("同步路由失败: " + e.getMessage(), e, SYNC_ROUTE_FAILED);
    }
}
```

- [ ] **Step 5: 添加错误码常量**

在 `ErrCodeConstant.java` 中添加：

```java
String SYNC_ROUTE_FAILED = "GATE0030";
```

- [ ] **Step 6: 添加必要的 import**

```java
import com.blink.gateway.admin.dto.req.SyncRoutesReq;
import com.blink.gateway.admin.dto.vo.StorageModeVO;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import com.blink.gateway.admin.dto.rsp.GatewayInstanceListRsp;
import com.blink.gateway.admin.service.GatewayInstanceService;
import com.blink.gateway.dto.RouteSyncMsg;
import java.util.stream.Collectors;
```

- [ ] **Step 7: 编译验证**

Run: `./gradlew :blink-gateway:gateway-admin:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/RouteServiceImpl.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/ErrCodeConstant.java
git commit -m "feat(gateway): RouteServiceImpl 实现 getStorageModes/getOnlineGatewayInstances/syncRoutesToInstances

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 6: 创建 NacosRouteServiceImpl

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/NacosRouteService.java`
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/NacosRouteServiceImpl.java`

- [ ] **Step 1: 创建 NacosRouteService 接口**

```java
package com.blink.gateway.admin.service;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.DeleteNacosRouteReq;
import com.blink.gateway.admin.dto.req.QueryNacosRouteReq;
import com.blink.gateway.admin.dto.req.SaveNacosRouteReq;
import com.blink.gateway.admin.dto.rsp.QueryGateWayRoutesRsp;

/**
 * Nacos 路由管理服务接口
 *
 * @author binblink
 */
public interface NacosRouteService {

    /**
     * 查询 Nacos 路由列表
     *
     * @param req 请求参数
     * @return 路由列表
     */
    ResponseDTO<QueryGateWayRoutesRsp> getNacosRouteList(QueryNacosRouteReq req);

    /**
     * 保存 Nacos 路由
     *
     * @param req 请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> saveNacosRoute(SaveNacosRouteReq req);

    /**
     * 删除 Nacos 路由
     *
     * @param req 请求参数
     * @return 操作结果
     */
    ResponseDTO<EmptyBody> deleteNacosRoute(DeleteNacosRouteReq req);
}
```

- [ ] **Step 2: 创建 NacosRouteServiceImpl**

```java
package com.blink.gateway.admin.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.gateway.admin.dto.req.DeleteNacosRouteReq;
import com.blink.gateway.admin.dto.req.QueryNacosRouteReq;
import com.blink.gateway.admin.dto.req.SaveNacosRouteReq;
import com.blink.gateway.admin.dto.rsp.QueryGateWayRoutesRsp;
import com.blink.gateway.admin.entity.RouteDefinitionDO;
import com.blink.gateway.admin.producer.GateWayStreamMessageProducer;
import com.blink.gateway.admin.service.NacosRouteService;
import com.blink.gateway.dto.RouteSyncMsg;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.blink.gateway.admin.constants.ErrCodeConstant.*;

/**
 * Nacos 路由管理服务实现
 *
 * @author binblink
 */
@Service
@Slf4j
public class NacosRouteServiceImpl implements NacosRouteService {

    @Resource
    private NacosConfigManager nacosConfigManager;

    @Resource
    private GateWayStreamMessageProducer messageProducer;

    private static final String STORAGE_MODE_NACOS = "nacos";
    private static final String PUSH_MODE_BROADCAST = "broadcast";

    @Override
    public ResponseDTO<QueryGateWayRoutesRsp> getNacosRouteList(QueryNacosRouteReq req) {
        QueryGateWayRoutesRsp rsp = new QueryGateWayRoutesRsp();

        String dataId = req.getDataId();
        String group = req.getGroup();

        if (StrUtil.isBlank(dataId)) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }
        if (StrUtil.isBlank(group)) {
            group = "DEFAULT_GROUP";
        }

        try {
            ConfigService configService = nacosConfigManager.getConfigService();
            String configContent = configService.getConfig(dataId, group, 5000);

            if (StrUtil.isBlank(configContent)) {
                log.warn("[NacosRoute] 未找到路由配置 | dataId: {}, group: {}", dataId, group);
                rsp.setTotal(0);
                rsp.setRows(new ArrayList<>());
                return ResponseDTO.newSuccessInstance(rsp);
            }

            List<RouteDefinitionDO> routes = JacksonUtil.fromJsonToList(configContent, RouteDefinitionDO.class);
            if (ObjectUtil.isNull(routes)) {
                routes = new ArrayList<>();
            }

            rsp.setTotal(routes.size());
            rsp.setRows(routes);

            log.info("[NacosRoute] 查询路由列表成功 | dataId: {}, group: {}, count: {}", dataId, group, routes.size());

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (NacosException e) {
            log.error("[NacosRoute] 从 Nacos 获取配置失败 | dataId: {}, group: {}, error: {}", dataId, group, e.getMessage(), e);
            throw new BlinkException("获取 Nacos 配置失败: " + e.getMessage(), e, GET_NACOS_CONFIG_FAILED);
        }
    }

    @Override
    public ResponseDTO<EmptyBody> saveNacosRoute(SaveNacosRouteReq req) {
        String dataId = req.getDataId();
        String group = req.getGroup();
        List<RouteDefinitionDO> newRoutes = req.getRoutes();

        if (StrUtil.isBlank(dataId)) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }
        if (StrUtil.isBlank(group)) {
            group = "DEFAULT_GROUP";
        }
        if (ObjectUtil.isNull(newRoutes) || newRoutes.isEmpty()) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        try {
            ConfigService configService = nacosConfigManager.getConfigService();
            String currentConfig = configService.getConfig(dataId, group, 5000);

            List<RouteDefinitionDO> existingRoutes = new ArrayList<>();
            if (StrUtil.isNotBlank(currentConfig)) {
                existingRoutes = JacksonUtil.fromJsonToList(currentConfig, RouteDefinitionDO.class);
                if (ObjectUtil.isNull(existingRoutes)) {
                    existingRoutes = new ArrayList<>();
                }
            }

            // 合合路由：按 ID 去重更新
            Map<String, RouteDefinitionDO> routeMap = existingRoutes.stream()
                    .collect(Collectors.toMap(RouteDefinitionDO::getId, Function.identity(), (a, b) -> a));

            for (RouteDefinitionDO newRoute : newRoutes) {
                if (StrUtil.isNotBlank(newRoute.getId())) {
                    routeMap.put(newRoute.getId(), newRoute);
                }
            }

            List<RouteDefinitionDO> mergedRoutes = new ArrayList<>(routeMap.values());
            String newConfigContent = JacksonUtil.toJson(mergedRoutes);

            configService.publishConfig(dataId, group, newConfigContent);

            // 发送同步消息
            RouteSyncMsg syncMsg = new RouteSyncMsg();
            syncMsg.setStorageMode(STORAGE_MODE_NACOS);
            syncMsg.setDataId(dataId);
            syncMsg.setGroup(group);
            syncMsg.setPushMode(PUSH_MODE_BROADCAST);
            messageProducer.routesOnChangeWithTarget(syncMsg);

            log.info("[NacosRoute] 保存路由成功 | dataId: {}, group: {}, count: {}", dataId, group, mergedRoutes.size());

            return ResponseDTO.newSuccessInstance();
        } catch (NacosException e) {
            log.error("[NacosRoute] 发布配置失败 | dataId: {}, group: {}, error: {}", dataId, group, e.getMessage(), e);
            throw new BlinkException("发布 Nacos 配置失败: " + e.getMessage(), e, PUBLISH_NACOS_CONFIG_FAILED);
        }
    }

    @Override
    public ResponseDTO<EmptyBody> deleteNacosRoute(DeleteNacosRouteReq req) {
        String dataId = req.getDataId();
        String group = req.getGroup();
        List<String> routeIds = req.getRouteIds();

        if (StrUtil.isBlank(dataId)) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }
        if (StrUtil.isBlank(group)) {
            group = "DEFAULT_GROUP";
        }
        if (ObjectUtil.isNull(routeIds) || routeIds.isEmpty()) {
            return ResponseDTO.newSuccessInstance();
        }

        try {
            ConfigService configService = nacosConfigManager.getConfigService();
            String currentConfig = configService.getConfig(dataId, group, 5000);

            if (StrUtil.isBlank(currentConfig)) {
                log.warn("[NacosRoute] 配置不存在，无需删除 | dataId: {}, group: {}", dataId, group);
                return ResponseDTO.newSuccessInstance();
            }

            List<RouteDefinitionDO> existingRoutes = JacksonUtil.fromJsonToList(currentConfig, RouteDefinitionDO.class);
            if (ObjectUtil.isNull(existingRoutes)) {
                return ResponseDTO.newSuccessInstance();
            }

            // 移除指定路由
            existingRoutes.removeIf(route -> routeIds.contains(route.getId()));

            String newConfigContent = JacksonUtil.toJson(existingRoutes);
            configService.publishConfig(dataId, group, newConfigContent);

            // 发送同步消息
            RouteSyncMsg syncMsg = new RouteSyncMsg();
            syncMsg.setStorageMode(STORAGE_MODE_NACOS);
            syncMsg.setDataId(dataId);
            syncMsg.setGroup(group);
            syncMsg.setPushMode(PUSH_MODE_BROADCAST);
            messageProducer.routesOnChangeWithTarget(syncMsg);

            log.info("[NacosRoute] 删除路由成功 | dataId: {}, group: {}, deletedIds: {}", dataId, group, routeIds);

            return ResponseDTO.newSuccessInstance();
        } catch (NacosException e) {
            log.error("[NacosRoute] 删除路由失败 | dataId: {}, group: {}, error: {}", dataId, group, e.getMessage(), e);
            throw new BlinkException("删除 Nacos 路由失败: " + e.getMessage(), e, DELETE_NACOS_ROUTE_FAILED);
        }
    }
}
```

- [ ] **Step 3: 添加错误码常量**

在 `ErrCodeConstant.java` 中添加：

```java
String GET_NACOS_CONFIG_FAILED = "GATE0031";
String PUBLISH_NACOS_CONFIG_FAILED = "GATE0032";
String DELETE_NACOS_ROUTE_FAILED = "GATE0033";
```

- [ ] **Step 4: 编译验证**

Run: `./gradlew :blink-gateway:gateway-admin:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/NacosRouteService.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/NacosRouteServiceImpl.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/ErrCodeConstant.java
git commit -m "feat(gateway): 实现 NacosRouteService 支持 Nacos 路由增删改查

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 7: 扩展 RouteController

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/RouteController.java`

- [ ] **Step 1: 新增依赖注入**

```java
@Resource
private NacosRouteService nacosRouteService;
```

- [ ] **Step 2: 新增接口方法**

```java
/**
 * 获取支持的存储方式列表
 */
@PostMapping("/getStorageModes")
public ResponseDTO<List<StorageModeVO>> getStorageModes(@RequestBody RequestDTO<EmptyBody> reqDto) {
    return routeService.getStorageModes();
}

/**
 * 获取在线网关实例列表
 */
@PostMapping("/getOnlineGatewayInstances")
public ResponseDTO<List<GatewayInstanceVO>> getOnlineGatewayInstances(@RequestBody RequestDTO<EmptyBody> reqDto) {
    return routeService.getOnlineGatewayInstances();
}

/**
 * 同步路由到指定实例
 */
@PostMapping("/syncRoutesToInstances")
public ResponseDTO<EmptyBody> syncRoutesToInstances(@RequestBody @Validated RequestDTO<SyncRoutesReq> reqDto) {
    return routeService.syncRoutesToInstances(reqDto.getBody());
}

/**
 * 查询 Nacos 路由列表
 */
@PostMapping("/getNacosRouteList")
public ResponseDTO<QueryGateWayRoutesRsp> getNacosRouteList(@RequestBody @Validated RequestDTO<QueryNacosRouteReq> reqDto) {
    return nacosRouteService.getNacosRouteList(reqDto.getBody());
}

/**
 * 保存 Nacos 路由
 */
@PostMapping("/saveNacosRoute")
public ResponseDTO<EmptyBody> saveNacosRoute(@RequestBody @Validated RequestDTO<SaveNacosRouteReq> reqDto) {
    return nacosRouteService.saveNacosRoute(reqDto.getBody());
}

/**
 * 删除 Nacos 路由
 */
@PostMapping("/deleteNacosRoute")
public ResponseDTO<EmptyBody> deleteNacosRoute(@RequestBody @Validated RequestDTO<DeleteNacosRouteReq> reqDto) {
    return nacosRouteService.deleteNacosRoute(reqDto.getBody());
}
```

- [ ] **Step 3: 添加必要的 import**

```java
import com.blink.gateway.admin.dto.req.SyncRoutesReq;
import com.blink.gateway.admin.dto.req.QueryNacosRouteReq;
import com.blink.gateway.admin.dto.req.SaveNacosRouteReq;
import com.blink.gateway.admin.dto.req.DeleteNacosRouteReq;
import com.blink.gateway.admin.dto.vo.StorageModeVO;
import com.blink.gateway.admin.dto.vo.GatewayInstanceVO;
import com.blink.gateway.admin.service.NacosRouteService;
import java.util.List;
```

- [ ] **Step 4: 编译验证**

Run: `./gradlew :blink-gateway:gateway-admin:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/RouteController.java
git commit -m "feat(gateway): 扩展 RouteController 新增存储方式和实例同步接口

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 8: 扩展 gateway-reactive CommonEventStreamListener

**Files:**
- Modify: `blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/listener/CommonEventStreamListener.java`

- [ ] **Step 1: 在 handlerEvent 方法中扩展路由同步逻辑**

找到 `if (message.getPayload() instanceof RouteSyncMsg routeEvent)` 块，替换为：

```java
// 路由更新同步
if (message.getPayload() instanceof RouteSyncMsg routeEvent) {
    try {
        // 检查推送模式
        String pushMode = routeEvent.getPushMode();
        if ("specified".equals(pushMode)) {
            // 指定实例模式：检查当前实例是否在目标列表中
            String currentInstanceId = appName + ":" + instanceId;
            List<String> targetInstanceIds = routeEvent.getTargetInstanceIds();
            if (targetInstanceIds == null || !targetInstanceIds.contains(currentInstanceId)) {
                log.info("[RouteSync] 跳过同步，当前实例不在目标列表中 | instanceId: {}, targetIds: {}", 
                        currentInstanceId, targetInstanceIds);
                smr.setHandledResult(true);
                return Mono.just(smr);
            }
            log.info("[RouteSync] 指定实例推送，当前实例在目标列表中 | instanceId: {}", currentInstanceId);
        }

        // 发布事件更新路由
        publisher.publishEvent(new RefreshRoutesEvent(this));
        smr.setHandledResult(true);
        log.info("[RouteSync] 路由刷新事件已发布 | storageMode: {}, pushMode: {}", 
                routeEvent.getStorageMode(), pushMode);
        return Mono.just(smr);
    } catch (Exception e) {
        log.error("[RouteSync] 路由发布刷新事件失败 | error: {}", e.getMessage(), e);
        smr.setHandledResult(false);
        return Mono.just(smr);
    }
}
```

- [ ] **Step 2: 添加必要的 import**

```java
import java.util.List;
```

- [ ] **Step 3: 编译验证**

Run: `./gradlew :blink-gateway:blink-gateway-reactive:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add blink-gateway/blink-gateway-reactive/src/main/java/com/blink/gateway/listener/CommonEventStreamListener.java
git commit -m "feat(gateway): CommonEventStreamListener 支持指定实例过滤

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 9: 前端 API 扩展

**Files:**
- Modify: `frontend/packages/gateway-admin/src/api/route.ts`

- [ ] **Step 1: 新增类型定义和 API 接口**

在文件末尾添加：

```typescript
// ========== 新增类型定义 ==========

/** 存储方式 VO */
export interface StorageModeVO {
  mode: string
  name: string
  description: string
}

/** 网关实例 VO */
export interface GatewayInstanceVO {
  instanceId: string
  serviceId: string
  host: string
  port: number
  uri: string
  status: number
  statusDesc: string
}

/** 同步路由请求 */
export interface SyncRoutesReq {
  storageMode: string
  routesGroup?: string
  dataId?: string
  group?: string
  pushMode: string
  targetInstanceIds?: string[]
  routeIds?: string[]
}

/** Nacos 路由查询请求 */
export interface QueryNacosRouteReq {
  dataId: string
  group?: string
  pageNum?: number
  pageSize?: number
}

/** Nacos 路由保存请求 */
export interface SaveNacosRouteReq {
  dataId: string
  group?: string
  routes: RouteDefinition[]
}

/** Nacos 路由删除请求 */
export interface DeleteNacosRouteReq {
  dataId: string
  group?: string
  routeIds: string[]
}

// ========== 新增 API 接口 ==========

/**
 * 获取支持的存储方式列表
 */
export const getStorageModes = (): Promise<StorageModeVO[]> => {
  return request.post('/route/getStorageModes', { body: {} })
}

/**
 * 获取在线网关实例列表
 */
export const getOnlineGatewayInstances = (): Promise<GatewayInstanceVO[]> => {
  return request.post('/route/getOnlineGatewayInstances', { body: {} })
}

/**
 * 同步路由到指定实例
 */
export const syncRoutesToInstances = (data: SyncRoutesReq): Promise<void> => {
  return request.post('/route/syncRoutesToInstances', { body: data })
}

/**
 * 查询 Nacos 路由列表
 */
export const getNacosRouteList = (params: QueryNacosRouteReq): Promise<PageResult<RouteDefinition>> => {
  return request.post('/route/getNacosRouteList', { body: params })
}

/**
 * 保存 Nacos 路由
 */
export const saveNacosRoute = (data: SaveNacosRouteReq): Promise<void> => {
  return request.post('/route/saveNacosRoute', { body: data })
}

/**
 * 删除 Nacos 路由
 */
export const deleteNacosRoute = (params: DeleteNacosRouteReq): Promise<void> => {
  return request.post('/route/deleteNacosRoute', { body: params })
}

// 扩展 routeApi 对象
export const routeApi = {
  getList: getRouteList,
  save: saveRoute,
  delete: deleteRoute,
  refresh: refreshRoutes,
  getStorageModes,
  getOnlineGatewayInstances,
  syncRoutesToInstances,
  getNacosRouteList,
  saveNacosRoute,
  deleteNacosRoute,
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/packages/gateway-admin/src/api/route.ts
git commit -m "feat(frontend): 扩展路由 API 支持存储方式和实例同步

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 10: 创建 SyncInstanceDialog 组件

**Files:**
- Create: `frontend/packages/gateway-admin/src/views/route/components/SyncInstanceDialog.vue`

- [ ] **Step 1: 创建组件文件**

```vue
<template>
  <el-dialog
    v-model="visible"
    title="同步到网关实例"
    width="500px"
    :close-on-click-modal="false"
    class="sync-instance-dialog"
  >
    <el-form label-width="100px">
      <!-- 推送方式选择 -->
      <el-form-item :label="t('route.pushMode')">
        <el-radio-group v-model="pushMode">
          <el-radio value="broadcast">
            <div class="push-mode-option">
              <strong>{{ t('route.broadcastPush') }}</strong>
              <small>{{ t('route.broadcastPushDesc') }}</small>
            </div>
          </el-radio>
          <el-radio value="specified">
            <div class="push-mode-option">
              <strong>{{ t('route.specifiedPush') }}</strong>
              <small>{{ t('route.specifiedPushDesc') }}</small>
            </div>
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 实例选择（仅指定实例模式显示） -->
      <el-form-item v-if="pushMode === 'specified'" :label="t('route.targetInstances')">
        <div v-loading="loadingInstances" class="instance-list">
          <el-checkbox-group v-model="selectedInstances">
            <div v-for="instance in onlineInstances" :key="instance.instanceId" class="instance-item">
              <el-checkbox :value="instance.instanceId">
                <div class="instance-info">
                  <span class="instance-name">{{ instance.instanceId }}</span>
                  <span class="instance-address">{{ instance.host }}:{{ instance.port }}</span>
                  <el-tag type="success" size="small" effect="plain">{{ t('route.online') }}</el-tag>
                </div>
              </el-checkbox>
            </div>
          </el-checkbox-group>
          <el-empty v-if="onlineInstances.length === 0" :description="t('route.noOnlineInstances')" />
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleCancel">{{ t('common.cancel') }}</el-button>
      <el-button
        type="primary"
        :loading="submitting"
        :disabled="pushMode === 'specified' && selectedInstances.length === 0"
        @click="handleSubmit"
      >
        {{ t('route.confirmPush') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  getOnlineGatewayInstances,
  syncRoutesToInstances,
  type GatewayInstanceVO,
  type SyncRoutesReq,
} from '@/api/route'

const props = defineProps<{
  modelValue: boolean
  storageMode: string
  routesGroup?: string
  dataId?: string
  group?: string
  routeIds?: string[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const pushMode = ref<'broadcast' | 'specified'>('broadcast')
const selectedInstances = ref<string[]>([])
const onlineInstances = ref<GatewayInstanceVO[]>([])
const loadingInstances = ref(false)
const submitting = ref(false)

// 监听弹窗打开，加载在线实例
watch(visible, async (val) => {
  if (val) {
    pushMode.value = 'broadcast'
    selectedInstances.value = []
    await loadOnlineInstances()
  }
})

const loadOnlineInstances = async () => {
  loadingInstances.value = true
  try {
    const instances = await getOnlineGatewayInstances()
    onlineInstances.value = instances
  } catch (error) {
    console.error('[SyncInstanceDialog] 加载实例列表失败:', error)
    onlineInstances.value = []
  } finally {
    loadingInstances.value = false
  }
}

const handleCancel = () => {
  visible.value = false
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const req: SyncRoutesReq = {
      storageMode: props.storageMode,
      pushMode: pushMode.value,
      routeIds: props.routeIds,
    }

    if (props.storageMode === 'redis') {
      req.routesGroup = props.routesGroup || 'default'
    } else {
      req.dataId = props.dataId
      req.group = props.group
    }

    if (pushMode.value === 'specified') {
      req.targetInstanceIds = selectedInstances.value
    }

    await syncRoutesToInstances(req)
    ElMessage.success(t('message.success'))
    emit('success')
    visible.value = false
  } catch (error) {
    console.error('[SyncInstanceDialog] 同步失败:', error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.sync-instance-dialog {
  .push-mode-option {
    display: flex;
    flex-direction: column;
    small {
      color: #909399;
      font-size: 12px;
    }
  }

  .instance-list {
    max-height: 300px;
    overflow-y: auto;
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    padding: 8px;

    .instance-item {
      padding: 8px;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .instance-info {
        display: flex;
        align-items: center;
        gap: 12px;

        .instance-name {
          font-weight: 500;
        }

        .instance-address {
          color: #909399;
          font-size: 12px;
        }
      }
    }
  }
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/packages/gateway-admin/src/views/route/components/SyncInstanceDialog.vue
git commit -m "feat(frontend): 创建 SyncInstanceDialog 同步实例弹窗组件

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 11: 改造路由管理页面

**Files:**
- Modify: `frontend/packages/gateway-admin/src/views/route/index.vue`

- [ ] **Step 1: 新增存储方式选择和同步按钮**

在 `<el-card class="search-card">` 的 `<el-form>` 中添加存储方式下拉：

```vue
<el-form-item :label="t('route.storageMode')">
  <el-select
    v-model="searchForm.storageMode"
    :placeholder="t('route.storageModePlaceholder')"
    style="width: 120px"
    @change="handleStorageModeChange"
  >
    <el-option
      v-for="mode in storageModes"
      :key="mode.mode"
      :label="mode.name"
      :value="mode.mode"
    />
  </el-select>
</el-form-item>
<!-- Redis 模式显示路由组 -->
<el-form-item v-if="searchForm.storageMode === 'redis'" :label="t('route.routeGroup')">
  <el-input
    v-model.trim="searchForm.routesGroup"
    :placeholder="t('route.routeGroupPlaceholder')"
    clearable
    style="width: 180px"
    @keyup.enter="handleSearch"
  />
</el-form-item>
<!-- Nacos 模式显示 dataId/group -->
<template v-else-if="searchForm.storageMode === 'nacos'">
  <el-form-item :label="t('route.dataId')">
    <el-input
      v-model.trim="searchForm.dataId"
      placeholder="gateway-routes.json"
      clearable
      style="width: 180px"
      @keyup.enter="handleSearch"
    />
  </el-form-item>
  <el-form-item :label="t('route.group')">
    <el-input
      v-model.trim="searchForm.group"
      placeholder="DEFAULT_GROUP"
      clearable
      style="width: 120px"
      @keyup.enter="handleSearch"
    />
  </el-form-item>
</template>
```

在 `<template #header>` 的按钮区域添加同步按钮：

```vue
<AuthButton
  :has-permission="() => checkPermission(ButtonPerms.Route.Sync)"
  type="warning"
  @click="handleOpenSyncDialog"
>
  <el-icon><Promotion /></el-icon>
  {{ t('route.syncToInstances') }}
</AuthButton>
```

- [ ] **Step 2: 在页面底部添加同步弹窗组件**

```vue
<!-- 同步实例弹窗 -->
<SyncInstanceDialog
  v-model="syncDialogVisible"
  :storage-mode="searchForm.storageMode"
  :routes-group="searchForm.routesGroup"
  :data-id="searchForm.dataId"
  :group="searchForm.group"
  @success="loadData"
/>
```

- [ ] **Step 3: 在 script 中新增状态和方法**

```typescript
import { Promotion } from '@element-plus/icons-vue'
import {
  getRouteList,
  saveRoute,
  deleteRoute,
  refreshRoutes,
  getStorageModes,
  getNacosRouteList,
  saveNacosRoute,
  deleteNacosRoute,
  type RouteDefinition,
  type RouteForm,
  type StorageModeVO,
} from '@/api/route'
import SyncInstanceDialog from './components/SyncInstanceDialog.vue'

// 存储方式
const storageModes = ref<StorageModeVO[]>([])
const syncDialogVisible = ref(false)

// 扩展 searchForm
const searchForm = reactive({
  storageMode: 'redis',
  routesGroup: '',
  dataId: 'gateway-routes.json',
  group: 'DEFAULT_GROUP',
})

// 加载存储方式列表
const loadStorageModes = async () => {
  try {
    const modes = await getStorageModes()
    storageModes.value = modes
  } catch (error) {
    console.error('[RouteManagement] 加载存储方式失败:', error)
  }
}

// 存储方式切换
const handleStorageModeChange = () => {
  // 保存到 localStorage
  localStorage.setItem('route-storage-mode', searchForm.storageMode)
  pagination.pageNum = 1
  loadData()
}

// 打开同步弹窗
const handleOpenSyncDialog = () => {
  syncDialogVisible.value = true
}

// 修改 loadData 支持双存储方式
const loadData = async () => {
  loading.value = true
  try {
    if (searchForm.storageMode === 'redis') {
      const res = await getRouteList({
        routesGroup: searchForm.routesGroup,
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize,
      })
      tableData.value = res.routes || res.rows || []
      pagination.total = res.total || 0
    } else {
      const res = await getNacosRouteList({
        dataId: searchForm.dataId,
        group: searchForm.group,
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize,
      })
      tableData.value = res.routes || res.rows || []
      pagination.total = res.total || 0
    }
  } catch (error) {
    console.error('[RouteManagement] 加载路由列表失败:', error)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// 修改 handleSubmit 支持双存储方式
const handleSubmit = async () => {
  // ... 现有验证逻辑 ...

  submitting.value = true
  try {
    let routeData = currentRoute
    if (editMode.value === 'json') {
      try {
        routeData = JSON.parse(routeJson.value)
      } catch {
        ElMessage.error('Invalid JSON format')
        return
      }
    }

    if (searchForm.storageMode === 'redis') {
      formData.routes = [routeData]
      await saveRoute(formData)
    } else {
      await saveNacosRoute({
        dataId: searchForm.dataId,
        group: searchForm.group,
        routes: [routeData],
      })
    }

    ElMessage.success(t('message.success'))
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('[RouteManagement] 提交失败:', error)
  } finally {
    submitting.value = false
  }
}

// 修改 handleDelete 支持双存储方式
const handleDelete = async (row: RouteDefinition) => {
  try {
    await ElMessageBox.confirm(t('route.deleteConfirm'), t('message.tips'), { type: 'warning' })
    
    if (searchForm.storageMode === 'redis') {
      await deleteRoute({
        routesGroup: searchForm.routesGroup || 'default',
        routeIds: [row.id],
      })
    } else {
      await deleteNacosRoute({
        dataId: searchForm.dataId,
        group: searchForm.group,
        routeIds: [row.id],
      })
    }

    ElMessage.success(t('message.deleteSuccess'))
    loadData()
  } catch {
    // 用户取消
  }
}

// onMounted 中初始化存储方式
onMounted(() => {
  // 从 localStorage 恢复存储方式
  const savedMode = localStorage.getItem('route-storage-mode')
  if (savedMode) {
    searchForm.storageMode = savedMode
  }
  loadStorageModes()
  loadData()
})
```

- [ ] **Step 4: Commit**

```bash
git add frontend/packages/gateway-admin/src/views/route/index.vue
git commit -m "feat(frontend): 路由管理页面支持存储方式切换和多实例同步

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 12: 添加国际化文案

**Files:**
- Modify: `frontend/packages/gateway-admin/src/locales/zh-CN/route.ts` (如存在)
- Modify: `frontend/packages/gateway-admin/src/locales/en-US/route.ts` (如存在)

- [ ] **Step 1: 添加中文文案**

```typescript
export default {
  route: {
    storageMode: '存储方式',
    storageModePlaceholder: '请选择存储方式',
    routeGroup: '路由组',
    routeGroupPlaceholder: '请输入路由组名称',
    dataId: 'Data ID',
    group: 'Group',
    pushMode: '推送方式',
    broadcastPush: '广播推送',
    broadcastPushDesc: '推送到所有在线实例',
    specifiedPush: '指定实例',
    specifiedPushDesc: '手动选择目标实例',
    targetInstances: '目标实例',
    online: '在线',
    offline: '离线',
    noOnlineInstances: '暂无在线实例',
    syncToInstances: '同步到实例',
    confirmPush: '确认推送',
    // ... 其他已有文案
  }
}
```

- [ ] **Step 2: 添加英文文案**

```typescript
export default {
  route: {
    storageMode: 'Storage Mode',
    storageModePlaceholder: 'Select storage mode',
    routeGroup: 'Route Group',
    routeGroupPlaceholder: 'Enter route group name',
    dataId: 'Data ID',
    group: 'Group',
    pushMode: 'Push Mode',
    broadcastPush: 'Broadcast',
    broadcastPushDesc: 'Push to all online instances',
    specifiedPush: 'Specified Instances',
    specifiedPushDesc: 'Select target instances manually',
    targetInstances: 'Target Instances',
    online: 'Online',
    offline: 'Offline',
    noOnlineInstances: 'No online instances',
    syncToInstances: 'Sync to Instances',
    confirmPush: 'Confirm Push',
    // ... other existing translations
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/packages/gateway-admin/src/locales/
git commit -m "feat(frontend): 添加路由管理国际化文案

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 13: 编译测试和最终提交

- [ ] **Step 1: 编译后端所有模块**

Run: `./gradlew :blink-gateway:gateway-admin:build :blink-gateway:blink-gateway-reactive:build :blink-gateway:blink-gateway-admin-api-dubbo:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 编译前端**

Run: `cd frontend/packages/gateway-admin && npm run build`
Expected: Build successful

- [ ] **Step 3: 发布模块到本地 Maven**

Run: `./gradlew :blink-gateway:blink-gateway-admin-api-dubbo:publishToMavenLocal`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 最终 Commit（如有未提交文件）**

```bash
git status
git add -A
git commit -m "feat(gateway): 路由管理增强功能完整实现

支持:
- 运行时切换存储方式（Redis/Nacos）
- 多实例路由推送（广播/指定实例）
- Nacos 路由完整增删改查

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Spec Coverage 检查

| Spec 章节 | 实现任务 |
|-----------|----------|
| 1.1 存储方式切换 | Task 5, 9, 11 |
| 1.2 多实例推送 | Task 1, 4, 5, 8, 10, 11 |
| 1.3 Nacos 路由管理 | Task 2, 6, 7, 9, 11 |
| 3.1 新增接口 | Task 7 |
| 3.2 新增 DTO | Task 2 |
| 3.4 RouteSyncMsg 扩展 | Task 1 |
| 3.5 gateway-reactive 改造 | Task 8 |
| 7.1 后端模块 | Task 1-8 |
| 7.2 前端模块 | Task 9-12 |

---

## 验收检查清单

- [ ] 切换存储方式后路由列表正确刷新
- [ ] localStorage 记忆存储方式选择
- [ ] Redis 路由增删改查正常
- [ ] Nacos 路由增删改查正常
- [ ] 广播推送同步到所有在线实例
- [ ] 指定实例推送仅同步到选中实例
- [ ] 离线实例不可勾选
- [ ] 前端编译无错误
- [ ] 后端编译无错误