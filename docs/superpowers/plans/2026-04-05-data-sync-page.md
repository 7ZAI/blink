# 数据同步页面实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 gateway-admin 添加数据同步功能页面，支持渠道、路由、配置数据的同步操作和一致性检查。

**Architecture:** 前端调用 Admin 后端接口，Admin 通过 Nacos 发现网关实例，调用各网关的 Actuator 端点获取缓存状态，对比数据库数据返回一致性检查结果。

**Tech Stack:** Spring Boot Actuator, Nacos Discovery, Vue 3, Element Plus, MySQL

---

## 文件结构

### 网关侧 (blink-gateway-reactive)

| 文件 | 职责 |
|------|------|
| `endpoint/CacheStatusEndpoint.java` | Actuator 端点，返回缓存项列表和 checksum |
| `dto/CacheStatusResponse.java` | 端点响应 DTO |

### Admin 侧 (gateway-admin)

| 文件 | 职责 |
|------|------|
| `controller/CacheStatusController.java` | 一致性检查和同步接口 |
| `service/CacheStatusService.java` | 服务接口 |
| `service/impl/CacheStatusServiceImpl.java` | 服务实现 |
| `mapper/SyncLogMapper.java` | 同步日志 Mapper |
| `entity/SyncLogDO.java` | 同步日志实体 |
| `dto/req/CacheCheckReq.java` | 检查请求 DTO |
| `dto/req/CacheSyncReq.java` | 同步请求 DTO |
| `dto/rsp/CacheCheckRsp.java` | 检查响应 DTO |
| `dto/rsp/InstanceCacheStatus.java` | 实例缓存状态 DTO |
| `dto/rsp/CacheItemStatus.java` | 缓存项状态 DTO |
| `dto/rsp/SyncLogRsp.java` | 同步日志响应 DTO |
| `db/migration/V20260405__add_sync_log_table.sql` | 数据库迁移脚本 |
| `db/migration/V20260405__add_sync_menu.sql` | 菜单数据脚本 |

### 前端 (gateway-admin-web)

| 文件 | 职责 |
|------|------|
| `api/dataSync.ts` | API 接口封装 |
| `views/dataSync/index.vue` | 数据同步页面 |
| `router/index.ts` | 添加路由 (修改) |
| `locales/zh-cn.ts` | 中文国际化 (修改) |
| `locales/en-us.ts` | 英文国际化 (修改) |

---

## Task 1: 网关侧 - 创建 Actuator 端点 DTO

**Files:**
- Create: `blink-gateway-reactive/src/main/java/com/blink/gateway/dto/CacheStatusResponse.java`
- Create: `blink-gateway-reactive/src/main/java/com/blink/gateway/dto/CacheItemDTO.java`

- [ ] **Step 1: 创建 CacheItemDTO**

```java
package com.blink.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 缓存项 DTO
 *
 * @author binblink
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheItemDTO {

    /**
     * 缓存 key
     */
    private String key;

    /**
     * 数据 checksum (MD5)
     */
    private String checksum;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: 创建 CacheStatusResponse**

```java
package com.blink.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 缓存状态响应
 *
 * @author binblink
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStatusResponse {

    /**
     * 实例 ID
     */
    private String instanceId;

    /**
     * 缓存类型: channel / route / config
     */
    private String type;

    /**
     * 查询时间
     */
    private LocalDateTime timestamp;

    /**
     * 缓存项列表
     */
    private List<CacheItemDTO> items;
}
```

---

## Task 2: 网关侧 - 创建 Actuator 端点

**Files:**
- Create: `blink-gateway-reactive/src/main/java/com/blink/gateway/endpoint/CacheStatusEndpoint.java`

- [ ] **Step 1: 创建 CacheStatusEndpoint**

```java
package com.blink.gateway.endpoint;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.ReactiveRedisClient;
import com.blink.gateway.dto.CacheItemDTO;
import com.blink.gateway.dto.CacheStatusResponse;
import com.blink.gateway.route.RedisRouteDefinitionRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.blink.gateway.constant.RedisConstans.*;

/**
 * 缓存状态 Actuator 端点
 * 用于一致性检查，返回各类型缓存的 key 和 checksum
 *
 * @author binblink
 */
@Endpoint(id = "cache-status")
@Component
@Slf4j
public class CacheStatusEndpoint {

    @Resource
    private ReactiveRedisClient redisClient;

    @Resource
    private RedisRouteDefinitionRepository routeRepository;

    @Value("${server.port}")
    private String serverPort;

    /**
     * 获取指定类型的缓存状态
     *
     * @param type 缓存类型: channel / route / config
     * @return 缓存状态响应
     */
    @ReadOperation
    public Mono<CacheStatusResponse> cacheStatus(@Selector String type) {
        String instanceId = getInstanceId();

        return switch (type.toLowerCase()) {
            case "channel" -> getChannelCacheStatus(instanceId);
            case "route" -> getRouteCacheStatus(instanceId);
            case "config" -> getConfigCacheStatus(instanceId);
            default -> Mono.just(CacheStatusResponse.builder()
                    .instanceId(instanceId)
                    .type(type)
                    .timestamp(LocalDateTime.now())
                    .items(new ArrayList<>())
                    .build());
        };
    }

    /**
     * 获取渠道缓存状态
     */
    private Mono<CacheStatusResponse> getChannelCacheStatus(String instanceId) {
        String pattern = BLINK_CHANNEL_PREFIX + "*";
        return redisClient.keys(pattern)
                .flatMap(key -> {
                    return redisClient.get(key)
                            .map(value -> {
                                String checksum = DigestUtil.md5Hex(value.toString());
                                return CacheItemDTO.builder()
                                        .key(extractChannelKey(key))
                                        .checksum(checksum)
                                        .updateTime(LocalDateTime.now())
                                        .build();
                            });
                })
                .collectList()
                .map(items -> CacheStatusResponse.builder()
                        .instanceId(instanceId)
                        .type("channel")
                        .timestamp(LocalDateTime.now())
                        .items(items)
                        .build())
                .onErrorResume(e -> {
                    log.error("[CacheStatusEndpoint] 获取渠道缓存状态失败 | error: {}", e.getMessage(), e);
                    return Mono.just(CacheStatusResponse.builder()
                            .instanceId(instanceId)
                            .type("channel")
                            .timestamp(LocalDateTime.now())
                            .items(new ArrayList<>())
                            .build());
                });
    }

    /**
     * 获取路由缓存状态
     */
    private Mono<CacheStatusResponse> getRouteCacheStatus(String instanceId) {
        return routeRepository.getRouteDefinitions()
                .map(route -> {
                    String checksum = calculateRouteChecksum(route);
                    return CacheItemDTO.builder()
                            .key(route.getId())
                            .checksum(checksum)
                            .updateTime(LocalDateTime.now())
                            .build();
                })
                .collectList()
                .map(items -> CacheStatusResponse.builder()
                        .instanceId(instanceId)
                        .type("route")
                        .timestamp(LocalDateTime.now())
                        .items(items)
                        .build())
                .onErrorResume(e -> {
                    log.error("[CacheStatusEndpoint] 获取路由缓存状态失败 | error: {}", e.getMessage(), e);
                    return Mono.just(CacheStatusResponse.builder()
                            .instanceId(instanceId)
                            .type("route")
                            .timestamp(LocalDateTime.now())
                            .items(new ArrayList<>())
                            .build());
                });
    }

    /**
     * 获取配置缓存状态
     */
    private Mono<CacheStatusResponse> getConfigCacheStatus(String instanceId) {
        String pattern = GATEWAY_CONFIG_KEY_PREFIX + "*";
        return redisClient.keys(pattern)
                .flatMap(key -> {
                    return redisClient.get(key)
                            .map(value -> {
                                String checksum = DigestUtil.md5Hex(value.toString());
                                return CacheItemDTO.builder()
                                        .key(extractConfigKey(key))
                                        .checksum(checksum)
                                        .updateTime(LocalDateTime.now())
                                        .build();
                            });
                })
                .collectList()
                .map(items -> CacheStatusResponse.builder()
                        .instanceId(instanceId)
                        .type("config")
                        .timestamp(LocalDateTime.now())
                        .items(items)
                        .build())
                .onErrorResume(e -> {
                    log.error("[CacheStatusEndpoint] 获取配置缓存状态失败 | error: {}", e.getMessage(), e);
                    return Mono.just(CacheStatusResponse.builder()
                            .instanceId(instanceId)
                            .type("config")
                            .timestamp(LocalDateTime.now())
                            .items(new ArrayList<>())
                            .build());
                });
    }

    /**
     * 计算路由 checksum
     */
    private String calculateRouteChecksum(RouteDefinition route) {
        String json = JacksonUtil.toJson(route);
        return DigestUtil.md5Hex(json);
    }

    /**
     * 从 Redis key 中提取渠道标识
     */
    private String extractChannelKey(String redisKey) {
        // blink:channel:appKey -> appKey
        return redisKey.replace(BLINK_CHANNEL_PREFIX, "");
    }

    /**
     * 从 Redis key 中提取配置 key
     */
    private String extractConfigKey(String redisKey) {
        // blink:config:gateway:keyName -> keyName
        return redisKey.replace(GATEWAY_CONFIG_KEY_PREFIX, "");
    }

    /**
     * 获取实例 ID
     */
    private String getInstanceId() {
        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            return hostAddress + ":" + serverPort;
        } catch (Exception e) {
            return "unknown:" + serverPort;
        }
    }
}
```

---

## Task 3: Admin 侧 - 创建数据库迁移脚本

**Files:**
- Create: `gateway-admin/src/main/resources/db/migration/V20260405__add_sync_log_table.sql`

- [ ] **Step 1: 创建同步日志表和菜单 SQL**

```sql
-- ============================================
-- 1. 同步日志表
-- ============================================
CREATE TABLE IF NOT EXISTS sync_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    sync_type VARCHAR(32) NOT NULL COMMENT '同步类型: channel/route/config',
    sync_mode TINYINT DEFAULT 0 COMMENT '同步模式: 0-全量, 1-增量/单项',
    sync_keys TEXT COMMENT '同步的key列表(JSON数组)',
    operator VARCHAR(64) COMMENT '操作人',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-成功, 1-部分失败, 2-失败',
    instance_count INT COMMENT '同步实例数量',
    success_count INT COMMENT '成功实例数量',
    detail TEXT COMMENT '详细结果(JSON)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_sync_type (sync_type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据同步日志表';

-- ============================================
-- 2. 数据同步菜单 (在网关管理目录 menu_id=49 下)
-- ============================================
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(50, '数据同步', 'DataSync', 2, 'Refresh', '/dataSync', 5, 0, 49, 2, 'views/dataSync/index.vue', NULL, 0, 'admin', NOW(), 0);

-- 数据同步按钮权限
INSERT INTO sys_menu (menu_id, menu_name, menu_en_name, type, icon, url, order_number, status, parent_id, menu_level, component_path, perm_id, hasChildren, create_by, create_time, delFlag) VALUES
(51, '执行同步', 'SyncData', 3, NULL, NULL, 1, 0, 50, 3, NULL, NULL, 0, 'admin', NOW(), 0),
(52, '一致性检查', 'CheckConsistency', 3, NULL, NULL, 2, 0, 50, 3, NULL, NULL, 0, 'admin', NOW(), 0);

-- ============================================
-- 3. 角色菜单关联 (超级管理员拥有所有菜单)
-- ============================================
INSERT INTO sys_role_menu_rela (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (50, 51, 52) AND delFlag = 0;
```

---

## Task 4: Admin 侧 - 创建实体和 DTO

**Files:**
- Create: `gateway-admin/src/main/java/com/blink/gateway/admin/entity/SyncLogDO.java`
- Create: `gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/CacheCheckReq.java`
- Create: `gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/CacheSyncReq.java`
- Create: `gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/CacheCheckRsp.java`
- Create: `gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/InstanceCacheStatus.java`
- Create: `gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/CacheItemStatus.java`
- Create: `gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/SyncLogRsp.java`

- [ ] **Step 1: 创建 SyncLogDO 实体**

```java
package com.blink.gateway.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据同步日志实体
 *
 * @author binblink
 */
@Data
@TableName("sync_log")
public class SyncLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 同步类型: channel/route/config
     */
    private String syncType;

    /**
     * 同步模式: 0-全量, 1-增量/单项
     */
    private Byte syncMode;

    /**
     * 同步的key列表(JSON数组)
     */
    private String syncKeys;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 状态: 0-成功, 1-部分失败, 2-失败
     */
    private Byte status;

    /**
     * 同步实例数量
     */
    private Integer instanceCount;

    /**
     * 成功实例数量
     */
    private Integer successCount;

    /**
     * 详细结果(JSON)
     */
    private String detail;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

- [ ] **Step 2: 创建 CacheCheckReq**

```java
package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 缓存一致性检查请求
 *
 * @author binblink
 */
@Data
public class CacheCheckReq implements Serializable {

    /**
     * 检查类型: channel / route / config
     */
    private String type;

    /**
     * 指定检查的 key 列表，为空则检查全部
     */
    private List<String> keys;
}
```

- [ ] **Step 3: 创建 CacheSyncReq**

```java
package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 缓存同步请求
 *
 * @author binblink
 */
@Data
public class CacheSyncReq implements Serializable {

    /**
     * 同步类型: channel / route / config
     */
    private String type;

    /**
     * 指定同步的 key 列表
     */
    private List<String> keys;

    /**
     * 是否全量同步
     */
    private Boolean syncAll;
}
```

- [ ] **Step 4: 创建 CacheItemStatus**

```java
package com.blink.gateway.admin.dto.rsp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 缓存项状态
 *
 * @author binblink
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheItemStatus implements Serializable {

    /**
     * 缓存 key
     */
    private String key;

    /**
     * 状态: MATCH / MISMATCH / MISSING
     */
    private String status;

    /**
     * checksum
     */
    private String checksum;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
```

- [ ] **Step 5: 创建 InstanceCacheStatus**

```java
package com.blink.gateway.admin.dto.rsp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 实例缓存状态
 *
 * @author binblink
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstanceCacheStatus implements Serializable {

    /**
     * 实例 ID
     */
    private String instanceId;

    /**
     * IP 地址
     */
    private String ip;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 各缓存项状态
     */
    private List<CacheItemStatus> items;
}
```

- [ ] **Step 6: 创建 CacheCheckRsp**

```java
package com.blink.gateway.admin.dto.rsp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 缓存一致性检查响应
 *
 * @author binblink
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheCheckRsp implements Serializable {

    /**
     * 检查类型
     */
    private String type;

    /**
     * 数据库中的数据
     */
    private List<CacheItemStatus> dbItems;

    /**
     * 各实例的缓存状态
     */
    private List<InstanceCacheStatus> instances;

    /**
     * 检查时间
     */
    private LocalDateTime checkTime;
}
```

- [ ] **Step 7: 创建 SyncLogRsp**

```java
package com.blink.gateway.admin.dto.rsp;

import com.blink.datasource.utils.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 同步日志响应
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SyncLogRsp extends PageDTO<SyncLogRsp.SyncLogItem> {

    /**
     * 同步日志项
     */
    @Data
    public static class SyncLogItem {
        /**
         * ID
         */
        private Long id;

        /**
         * 同步类型
         */
        private String syncType;

        /**
         * 同步模式: 0-全量, 1-增量
         */
        private Byte syncMode;

        /**
         * 同步的 key 列表
         */
        private List<String> syncKeys;

        /**
         * 操作人
         */
        private String operator;

        /**
         * 状态: 0-成功, 1-部分失败, 2-失败
         */
        private Byte status;

        /**
         * 实例数量
         */
        private Integer instanceCount;

        /**
         * 成功数量
         */
        private Integer successCount;

        /**
         * 创建时间
         */
        private LocalDateTime createTime;
    }
}
```

---

## Task 5: Admin 侧 - 创建 Mapper

**Files:**
- Create: `gateway-admin/src/main/java/com/blink/gateway/admin/mapper/SyncLogMapper.java`

- [ ] **Step 1: 创建 SyncLogMapper**

```java
package com.blink.gateway.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blink.gateway.admin.entity.SyncLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 同步日志 Mapper
 *
 * @author binblink
 */
@Mapper
public interface SyncLogMapper extends BaseMapper<SyncLogDO> {
}
```

---

## Task 6: Admin 侧 - 创建 Service 接口和实现

**Files:**
- Create: `gateway-admin/src/main/java/com/blink/gateway/admin/service/CacheStatusService.java`
- Create: `gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/CacheStatusServiceImpl.java`

- [ ] **Step 1: 创建 CacheStatusService 接口**

```java
package com.blink.gateway.admin.service;

import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.CacheCheckReq;
import com.blink.gateway.admin.dto.req.CacheSyncReq;
import com.blink.gateway.admin.dto.rsp.CacheCheckRsp;
import com.blink.gateway.admin.dto.rsp.SyncLogRsp;

/**
 * 缓存状态服务接口
 *
 * @author binblink
 */
public interface CacheStatusService {

    /**
     * 获取网关实例列表
     *
     * @return 实例列表
     */
    ResponseDTO<?> getGatewayInstances();

    /**
     * 执行一致性检查
     *
     * @param req 检查请求
     * @return 检查结果
     */
    ResponseDTO<CacheCheckRsp> checkConsistency(CacheCheckReq req);

    /**
     * 同步数据到网关
     *
     * @param req 同步请求
     * @return 操作结果
     */
    ResponseDTO<Void> syncData(CacheSyncReq req);

    /**
     * 获取同步日志列表
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 日志列表
     */
    ResponseDTO<SyncLogRsp> getSyncLogs(Integer pageNum, Integer pageSize);
}
```

- [ ] **Step 2: 创建 CacheStatusServiceImpl 实现**

```java
package com.blink.gateway.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.gateway.admin.dto.req.CacheCheckReq;
import com.blink.gateway.admin.dto.req.CacheSyncReq;
import com.blink.gateway.admin.dto.rsp.CacheCheckRsp;
import com.blink.gateway.admin.dto.rsp.CacheItemStatus;
import com.blink.gateway.admin.dto.rsp.InstanceCacheStatus;
import com.blink.gateway.admin.dto.rsp.SyncLogRsp;
import com.blink.gateway.admin.entity.GaChannelDO;
import com.blink.gateway.admin.entity.SyncLogDO;
import com.blink.gateway.admin.mapper.GaChannelMapper;
import com.blink.gateway.admin.mapper.SyncLogMapper;
import com.blink.gateway.admin.producer.GateWayStreamMessageProducer;
import com.blink.gateway.admin.service.CacheStatusService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.blink.gateway.admin.constants.ErrCodeConstant.DATA_SYNC_FAILED;
import static com.blink.gateway.admin.constants.RedisKeyConstant.CHANNEL_INFO;
import static com.blink.gateway.admin.constants.RedisKeyConstant.GATEWAY_DYNAMIC_ROUTES;

/**
 * 缓存状态服务实现
 *
 * @author binblink
 */
@Service
@Slf4j
public class CacheStatusServiceImpl implements CacheStatusService {

    private static final String GATEWAY_SERVICE_NAME = "gateway-reactive";

    @Resource
    private DiscoveryClient discoveryClient;

    @Resource
    private GaChannelMapper channelMapper;

    @Resource
    private SyncLogMapper syncLogMapper;

    @Resource
    private GateWayStreamMessageProducer messageProducer;

    @Override
    public ResponseDTO<?> getGatewayInstances() {
        List<ServiceInstance> instances = discoveryClient.getInstances(GATEWAY_SERVICE_NAME);

        List<Map<String, Object>> instanceList = new ArrayList<>();
        for (ServiceInstance instance : instances) {
            Map<String, Object> map = new HashMap<>();
            map.put("instanceId", instance.getInstanceId());
            map.put("host", instance.getHost());
            map.put("port", instance.getPort());
            map.put("uri", instance.getUri().toString());
            instanceList.add(map);
        }

        log.info("[CacheStatus] 获取网关实例列表成功 | total: {}", instanceList.size());

        return ResponseDTO.newSuccessInstance(instanceList);
    }

    @Override
    public ResponseDTO<CacheCheckRsp> checkConsistency(CacheCheckReq req) {
        String type = req.getType();

        if (StrUtil.isBlank(type)) {
            BlinkException.throwBusinessException("检查类型不能为空");
        }

        try {
            // 获取数据库中的数据
            List<CacheItemStatus> dbItems = getDbItems(type, req.getKeys());

            // 获取各网关实例的缓存状态
            List<ServiceInstance> instances = discoveryClient.getInstances(GATEWAY_SERVICE_NAME);
            List<InstanceCacheStatus> instanceStatusList = new ArrayList<>();

            for (ServiceInstance instance : instances) {
                InstanceCacheStatus instanceStatus = getInstanceCacheStatus(instance, type);
                instanceStatusList.add(instanceStatus);
            }

            // 对比并设置状态
            for (InstanceCacheStatus instanceStatus : instanceStatusList) {
                for (CacheItemStatus item : instanceStatus.getItems()) {
                    CacheItemStatus dbItem = findDbItem(dbItems, item.getKey());
                    if (dbItem == null) {
                        // 数据库中不存在，标记为多余
                        item.setStatus("ORPHAN");
                    } else if (dbItem.getChecksum().equals(item.getChecksum())) {
                        item.setStatus("MATCH");
                    } else {
                        item.setStatus("MISMATCH");
                    }
                }

                // 检查缺失的项
                for (CacheItemStatus dbItem : dbItems) {
                    CacheItemStatus instanceItem = findInstanceItem(instanceStatus.getItems(), dbItem.getKey());
                    if (instanceItem == null) {
                        instanceStatus.getItems().add(CacheItemStatus.builder()
                                .key(dbItem.getKey())
                                .status("MISSING")
                                .checksum(null)
                                .build());
                    }
                }
            }

            CacheCheckRsp rsp = CacheCheckRsp.builder()
                    .type(type)
                    .dbItems(dbItems)
                    .instances(instanceStatusList)
                    .checkTime(LocalDateTime.now())
                    .build();

            log.info("[CacheStatus] 一致性检查完成 | type: {}, dbCount: {}, instanceCount: {}",
                    type, dbItems.size(), instanceStatusList.size());

            return ResponseDTO.newSuccessInstance(rsp);
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[CacheStatus] 一致性检查失败 | type: {}, error: {}", type, e.getMessage(), e);
            throw new BlinkException("一致性检查失败: " + e.getMessage(), e, DATA_SYNC_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Void> syncData(CacheSyncReq req) {
        String type = req.getType();
        boolean syncAll = Boolean.TRUE.equals(req.getSyncAll());

        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(GATEWAY_SERVICE_NAME);
            int successCount = 0;

            // 发送同步消息
            switch (type.toLowerCase()) {
                case "channel" -> {
                    if (syncAll) {
                        messageProducer.cacheOnChange("channel:*");
                    } else if (CollUtil.isNotEmpty(req.getKeys())) {
                        for (String key : req.getKeys()) {
                            messageProducer.cacheOnChange(CHANNEL_INFO + key);
                        }
                    }
                }
                case "route" -> messageProducer.routesOnChange(GATEWAY_DYNAMIC_ROUTES);
                case "config" -> messageProducer.cacheOnChange("config:*");
                default -> BlinkException.throwBusinessException("不支持的同步类型: " + type);
            }

            successCount = instances.size();

            // 记录同步日志
            SyncLogDO syncLog = new SyncLogDO();
            syncLog.setSyncType(type);
            syncLog.setSyncMode((byte) (syncAll ? 0 : 1));
            syncLog.setSyncKeys(CollUtil.isNotEmpty(req.getKeys()) ? JacksonUtil.toJson(req.getKeys()) : null);
            syncLog.setOperator("admin"); // TODO: 从上下文获取当前用户
            syncLog.setStatus((byte) 0);
            syncLog.setInstanceCount(instances.size());
            syncLog.setSuccessCount(successCount);
            syncLog.setCreateTime(LocalDateTime.now());
            syncLogMapper.insert(syncLog);

            log.info("[CacheStatus] 同步完成 | type: {}, syncAll: {}, successCount: {}", type, syncAll, successCount);

            return ResponseDTO.newSuccessInstance();
        } catch (BlinkException e) {
            throw e;
        } catch (Exception e) {
            log.error("[CacheStatus] 同步失败 | type: {}, error: {}", type, e.getMessage(), e);
            throw new BlinkException("同步失败: " + e.getMessage(), e, DATA_SYNC_FAILED);
        }
    }

    @Override
    public ResponseDTO<SyncLogRsp> getSyncLogs(Integer pageNum, Integer pageSize) {
        SyncLogRsp rsp = new SyncLogRsp();
        rsp.setPageNum(pageNum);
        rsp.setPageSize(pageSize);

        LambdaQueryWrapper<SyncLogDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(SyncLogDO::getCreateTime);

        Long total = syncLogMapper.selectCount(queryWrapper);
        rsp.setTotal(total.intValue());

        if (total > 0) {
            queryWrapper.last("LIMIT " + pageSize + " OFFSET " + (pageNum - 1) * pageSize);
            List<SyncLogDO> logs = syncLogMapper.selectList(queryWrapper);

            List<SyncLogRsp.SyncLogItem> items = logs.stream().map(log -> {
                SyncLogRsp.SyncLogItem item = new SyncLogRsp.SyncLogItem();
                item.setId(log.getId());
                item.setSyncType(log.getSyncType());
                item.setSyncMode(log.getSyncMode());
                item.setSyncKeys(StrUtil.isNotBlank(log.getSyncKeys())
                        ? JacksonUtil.parseList(log.getSyncKeys(), String.class)
                        : new ArrayList<>());
                item.setOperator(log.getOperator());
                item.setStatus(log.getStatus());
                item.setInstanceCount(log.getInstanceCount());
                item.setSuccessCount(log.getSuccessCount());
                item.setCreateTime(log.getCreateTime());
                return item;
            }).collect(Collectors.toList());

            rsp.setRows(items);
        }

        return ResponseDTO.newSuccessInstance(rsp);
    }

    /**
     * 获取数据库中的数据
     */
    private List<CacheItemStatus> getDbItems(String type, List<String> keys) {
        List<CacheItemStatus> items = new ArrayList<>();

        switch (type.toLowerCase()) {
            case "channel" -> {
                List<GaChannelDO> channels = channelMapper.selectList(
                        new LambdaQueryWrapper<GaChannelDO>()
                                .in(CollUtil.isNotEmpty(keys), GaChannelDO::getChannelId, keys)
                );
                for (GaChannelDO channel : channels) {
                    items.add(CacheItemStatus.builder()
                            .key(channel.getChannelId())
                            .checksum(calculateChecksum(channel))
                            .updateTime(channel.getUpdateTime())
                            .build());
                }
            }
            case "route" -> {
                // TODO: 从 Redis 或数据库获取路由数据
            }
            case "config" -> {
                // TODO: 获取配置数据
            }
        }

        return items;
    }

    /**
     * 获取实例缓存状态
     */
    private InstanceCacheStatus getInstanceCacheStatus(ServiceInstance instance, String type) {
        try {
            String url = String.format("%s/actuator/cache-status/%s", instance.getUri(), type);

            WebClient webClient = WebClient.builder()
                    .baseUrl(url)
                    .build();

            String response = webClient.get()
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 解析响应
            Map<String, Object> responseMap = JacksonUtil.parseMessyJson(response, Map.class);
            List<Map<String, Object>> itemsMap = (List<Map<String, Object>>) responseMap.get("items");

            List<CacheItemStatus> items = new ArrayList<>();
            if (CollUtil.isNotEmpty(itemsMap)) {
                for (Map<String, Object> itemMap : itemsMap) {
                    items.add(CacheItemStatus.builder()
                            .key((String) itemMap.get("key"))
                            .checksum((String) itemMap.get("checksum"))
                            .build());
                }
            }

            return InstanceCacheStatus.builder()
                    .instanceId(instance.getInstanceId())
                    .ip(instance.getHost())
                    .port(instance.getPort())
                    .items(items)
                    .build();
        } catch (Exception e) {
            log.error("[CacheStatus] 获取实例缓存状态失败 | instance: {}, error: {}",
                    instance.getInstanceId(), e.getMessage(), e);

            return InstanceCacheStatus.builder()
                    .instanceId(instance.getInstanceId())
                    .ip(instance.getHost())
                    .port(instance.getPort())
                    .items(new ArrayList<>())
                    .build();
        }
    }

    /**
     * 计算对象 checksum
     */
    private String calculateChecksum(Object obj) {
        String json = JacksonUtil.toJson(obj);
        return DigestUtil.md5Hex(json);
    }

    /**
     * 从数据库项列表中查找指定 key
     */
    private CacheItemStatus findDbItem(List<CacheItemStatus> items, String key) {
        return items.stream()
                .filter(item -> key.equals(item.getKey()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 从实例项列表中查找指定 key
     */
    private CacheItemStatus findInstanceItem(List<CacheItemStatus> items, String key) {
        return items.stream()
                .filter(item -> key.equals(item.getKey()))
                .findFirst()
                .orElse(null);
    }
}
```

---

## Task 7: Admin 侧 - 创建 Controller

**Files:**
- Create: `gateway-admin/src/main/java/com/blink/gateway/admin/controller/CacheStatusController.java`

- [ ] **Step 1: 创建 CacheStatusController**

```java
package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.CacheCheckReq;
import com.blink.gateway.admin.dto.req.CacheSyncReq;
import com.blink.gateway.admin.dto.rsp.CacheCheckRsp;
import com.blink.gateway.admin.dto.rsp.SyncLogRsp;
import com.blink.gateway.admin.service.CacheStatusService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 缓存状态控制器
 * 提供一致性检查和数据同步功能
 *
 * @author binblink
 */
@RestController
@RequestMapping("/cacheStatus")
public class CacheStatusController {

    @Resource
    private CacheStatusService cacheStatusService;

    /**
     * 获取网关实例列表
     *
     * @return 实例列表
     */
    @GetMapping("/instances")
    public ResponseDTO<?> getInstances() {
        return cacheStatusService.getGatewayInstances();
    }

    /**
     * 执行一致性检查
     *
     * @param reqDto 请求参数
     * @return 检查结果
     */
    @PostMapping("/check")
    public ResponseDTO<CacheCheckRsp> check(@RequestBody RequestDTO<CacheCheckReq> reqDto) {
        return cacheStatusService.checkConsistency(reqDto.getBody());
    }

    /**
     * 同步数据到网关
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/sync")
    public ResponseDTO<Void> sync(@RequestBody RequestDTO<CacheSyncReq> reqDto) {
        return cacheStatusService.syncData(reqDto.getBody());
    }

    /**
     * 获取同步日志列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 日志列表
     */
    @GetMapping("/logs")
    public ResponseDTO<SyncLogRsp> getLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return cacheStatusService.getSyncLogs(pageNum, pageSize);
    }
}
```

---

## Task 8: 前端 - 创建 API 文件

**Files:**
- Create: `gateway-admin-web/src/api/dataSync.ts`

- [ ] **Step 1: 创建 dataSync.ts**

```typescript
import request from '@/utils/request'

// Types
export interface CacheCheckReq {
  type: 'channel' | 'route' | 'config'
  keys?: string[]
}

export interface CacheSyncReq {
  type: 'channel' | 'route' | 'config'
  keys?: string[]
  syncAll?: boolean
}

export interface CacheItemStatus {
  key: string
  status: 'MATCH' | 'MISMATCH' | 'MISSING' | 'ORPHAN'
  checksum: string | null
  updateTime?: string
}

export interface InstanceCacheStatus {
  instanceId: string
  ip: string
  port: number
  items: CacheItemStatus[]
}

export interface CacheCheckRsp {
  type: string
  dbItems: CacheItemStatus[]
  instances: InstanceCacheStatus[]
  checkTime: string
}

export interface SyncLogItem {
  id: number
  syncType: string
  syncMode: number
  syncKeys: string[]
  operator: string
  status: number
  instanceCount: number
  successCount: number
  createTime: string
}

export interface SyncLogRsp {
  total: number
  pageNum: number
  pageSize: number
  rows: SyncLogItem[]
}

export interface GatewayInstance {
  instanceId: string
  host: string
  port: number
  uri: string
}

/**
 * 获取网关实例列表
 */
export const getGatewayInstances = (): Promise<GatewayInstance[]> => {
  return request.get('/cacheStatus/instances')
}

/**
 * 执行一致性检查
 */
export const checkConsistency = (params: CacheCheckReq): Promise<CacheCheckRsp> => {
  return request.post('/cacheStatus/check', { body: params })
}

/**
 * 同步数据到网关
 */
export const syncData = (params: CacheSyncReq): Promise<void> => {
  return request.post('/cacheStatus/sync', { body: params })
}

/**
 * 获取同步日志列表
 */
export const getSyncLogs = (pageNum = 1, pageSize = 10): Promise<SyncLogRsp> => {
  return request.get('/cacheStatus/logs', { params: { pageNum, pageSize } })
}

// API object
export const dataSyncApi = {
  getInstances: getGatewayInstances,
  check: checkConsistency,
  sync: syncData,
  getLogs: getSyncLogs
}
```

---

## Task 9: 前端 - 创建页面组件

**Files:**
- Create: `gateway-admin-web/src/views/dataSync/index.vue`

- [ ] **Step 1: 创建数据同步页面**

```vue
<template>
  <div class="data-sync-page table-page-container">
    <!-- 同步操作区 -->
    <el-card class="sync-card shrink-0" shadow="never">
      <template #header>
        <span class="card-title">{{ t('dataSync.syncOperation') }}</span>
      </template>
      <div class="sync-buttons">
        <el-button type="primary" :loading="syncing" @click="handleSync('channel')">
          <el-icon><Refresh /></el-icon>{{ t('dataSync.channelSync') }}
        </el-button>
        <el-button type="primary" :loading="syncing" @click="handleSync('route')">
          <el-icon><Refresh /></el-icon>{{ t('dataSync.routeSync') }}
        </el-button>
        <el-button type="primary" :loading="syncing" @click="handleSync('config')">
          <el-icon><Refresh /></el-icon>{{ t('dataSync.configSync') }}
        </el-button>
      </div>
    </el-card>

    <!-- 一致性检查区 -->
    <el-card class="check-card flex-1 flex flex-col overflow-hidden" shadow="never">
      <template #header>
        <div class="check-header">
          <span class="card-title">{{ t('dataSync.consistencyCheck') }}</span>
          <div class="check-actions">
            <el-select v-model="checkType" style="width: 120px">
              <el-option :label="t('dataSync.channel')" value="channel" />
              <el-option :label="t('dataSync.route')" value="route" />
              <el-option :label="t('dataSync.config')" value="config" />
            </el-select>
            <el-button type="primary" :loading="checking" @click="handleCheck">
              <el-icon><Search /></el-icon>{{ t('dataSync.startCheck') }}
            </el-button>
          </div>
        </div>
      </template>

      <!-- 检查结果表格 -->
      <div class="table-wrapper">
        <el-table
          v-loading="checking"
          :data="checkResult?.dbItems || []"
          height="100%"
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column :label="t('dataSync.name')" prop="key" min-width="160" />
          <el-table-column :label="t('dataSync.database')" width="100" align="center">
            <template #default>
              <el-tag type="success">✅</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-for="instance in checkResult?.instances"
            :key="instance.instanceId"
            :label="instance.instanceId"
            min-width="120"
            align="center"
          >
            <template #default="{ row }">
              <template v-if="getInstanceItemStatus(instance, row.key) === 'MATCH'">
                <el-tag type="success">{{ t('dataSync.match') }}</el-tag>
              </template>
              <template v-else-if="getInstanceItemStatus(instance, row.key) === 'MISMATCH'">
                <el-tag type="warning">{{ t('dataSync.mismatch') }}</el-tag>
              </template>
              <template v-else-if="getInstanceItemStatus(instance, row.key) === 'MISSING'">
                <el-tag type="danger">{{ t('dataSync.missing') }}</el-tag>
              </template>
              <template v-else>
                <el-tag type="info">-</el-tag>
              </template>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.operation')" width="100" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleSyncSingle(row.key)">
                {{ t('dataSync.sync') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 批量操作 -->
      <div class="batch-actions" v-if="selectedKeys.length > 0">
        <el-button type="primary" @click="handleSyncSelected">
          {{ t('dataSync.syncSelected') }} ({{ selectedKeys.length }})
        </el-button>
      </div>
    </el-card>

    <!-- 同步日志区 -->
    <el-card class="log-card shrink-0" shadow="never">
      <template #header>
        <span class="card-title">{{ t('dataSync.syncLog') }}</span>
      </template>
      <el-table v-loading="loadingLogs" :data="syncLogs" max-height="300" stripe>
        <el-table-column :label="t('dataSync.syncTime')" prop="createTime" width="180" />
        <el-table-column :label="t('dataSync.syncType')" prop="syncType" width="100">
          <template #default="{ row }">
            <el-tag>{{ t(`dataSync.${row.syncType}`) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('dataSync.operator')" prop="operator" width="100" />
        <el-table-column :label="t('dataSync.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
              {{ row.status === 0 ? t('common.success') : t('common.failed') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('dataSync.instanceCount')" width="120">
          <template #default="{ row }">
            {{ row.successCount }} / {{ row.instanceCount }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  checkConsistency,
  syncData,
  getSyncLogs,
  type CacheCheckRsp,
  type SyncLogItem,
  type InstanceCacheStatus
} from '@/api/dataSync'

defineOptions({
  name: 'DataSync'
})

const { t } = useI18n()

// 状态
const checkType = ref<'channel' | 'route' | 'config'>('channel')
const checking = ref(false)
const syncing = ref(false)
const loadingLogs = ref(false)
const checkResult = ref<CacheCheckRsp | null>(null)
const syncLogs = ref<SyncLogItem[]>([])
const selectedKeys = ref<string[]>([])

/**
 * 执行一致性检查
 */
const handleCheck = async () => {
  checking.value = true
  try {
    const res = await checkConsistency({ type: checkType.value })
    checkResult.value = res
    ElMessage.success(t('dataSync.checkSuccess'))
  } catch (error) {
    console.error('[DataSync] Check failed:', error)
  } finally {
    checking.value = false
  }
}

/**
 * 全量同步
 */
const handleSync = async (type: 'channel' | 'route' | 'config') => {
  try {
    await ElMessageBox.confirm(t('dataSync.confirmSyncAll'), t('message.tips'), { type: 'warning' })
    syncing.value = true
    await syncData({ type, syncAll: true })
    ElMessage.success(t('dataSync.syncSuccess'))
    loadSyncLogs()
  } catch {
    // 用户取消
  } finally {
    syncing.value = false
  }
}

/**
 * 单项同步
 */
const handleSyncSingle = async (key: string) => {
  try {
    syncing.value = true
    await syncData({ type: checkType.value, keys: [key], syncAll: false })
    ElMessage.success(t('dataSync.syncSuccess'))
    handleCheck()
    loadSyncLogs()
  } catch (error) {
    console.error('[DataSync] Sync failed:', error)
  } finally {
    syncing.value = false
  }
}

/**
 * 批量同步选中项
 */
const handleSyncSelected = async () => {
  try {
    await ElMessageBox.confirm(
      t('dataSync.confirmSync', { count: selectedKeys.value.length }),
      t('message.tips'),
      { type: 'warning' }
    )
    syncing.value = true
    await syncData({ type: checkType.value, keys: selectedKeys.value, syncAll: false })
    ElMessage.success(t('dataSync.syncSuccess'))
    handleCheck()
    loadSyncLogs()
  } catch {
    // 用户取消
  } finally {
    syncing.value = false
  }
}

/**
 * 表格选择变更
 */
const handleSelectionChange = (selection: { key: string }[]) => {
  selectedKeys.value = selection.map(item => item.key)
}

/**
 * 获取实例中某项的状态
 */
const getInstanceItemStatus = (instance: InstanceCacheStatus, key: string): string => {
  const item = instance.items.find(i => i.key === key)
  return item?.status || 'MISSING'
}

/**
 * 加载同步日志
 */
const loadSyncLogs = async () => {
  loadingLogs.value = true
  try {
    const res = await getSyncLogs(1, 10)
    syncLogs.value = res.rows || []
  } catch (error) {
    console.error('[DataSync] Load logs failed:', error)
  } finally {
    loadingLogs.value = false
  }
}

onMounted(() => {
  loadSyncLogs()
})
</script>

<style scoped lang="scss">
.data-sync-page {
  gap: 16px;
}

.sync-card {
  .sync-buttons {
    display: flex;
    gap: 12px;
  }
}

.check-card {
  .check-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .check-actions {
      display: flex;
      gap: 12px;
    }
  }
}

.batch-actions {
  padding: 12px 0;
  border-top: 1px solid var(--el-border-color-lighter);
}

.log-card {
  max-height: 380px;
}

.card-title {
  font-weight: 500;
  font-size: 15px;
}
</style>
```

---

## Task 10: 前端 - 修改路由配置

**Files:**
- Modify: `gateway-admin-web/src/router/index.ts`

- [ ] **Step 1: 在路由配置中添加数据同步路由**

找到 `children` 数组中 `monitor` 路由后面，添加：

```typescript
      {
        path: 'dataSync',
        name: 'DataSync',
        component: () => import('@/views/dataSync/index.vue'),
        meta: { title: 'dataSync.title' }
      },
```

---

## Task 11: 前端 - 添加国际化文本

**Files:**
- Modify: `gateway-admin-web/src/locales/zh-cn.ts`
- Modify: `gateway-admin-web/src/locales/en-us.ts`

- [ ] **Step 1: 在 zh-cn.ts 中添加 dataSync 配置**

在文件末尾的 `}` 之前添加：

```typescript
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
    name: '名称',
    database: '数据库',
    sync: '同步',
    instanceCount: '实例数量',
    checkSuccess: '检查完成',
    syncSuccess: '同步成功',
    confirmSync: '确认同步选中的 {count} 项？',
    confirmSyncAll: '确认执行全量同步？',
  },
```

- [ ] **Step 2: 在 en-us.ts 中添加 dataSync 配置**

在文件末尾的 `}` 之前添加：

```typescript
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
    name: 'Name',
    database: 'Database',
    sync: 'Sync',
    instanceCount: 'Instances',
    checkSuccess: 'Check completed',
    syncSuccess: 'Sync successful',
    confirmSync: 'Confirm to sync {count} selected items?',
    confirmSyncAll: 'Confirm to perform full sync?',
  },
```

---

## Task 12: 最终验证

- [ ] **Step 1: 验证后端编译**

```bash
./gradlew :blink-gateway:blink-gateway-reactive:build
./gradlew :blink-gateway:gateway-admin:build
```

- [ ] **Step 2: 验证前端编译**

```bash
cd blink-gateway/gateway-admin-web
npm run build
```

- [ ] **Step 3: 验证数据库迁移**

启动 gateway-admin 应用，确认 flyway 执行了迁移脚本。

- [ ] **Step 4: 功能测试**

1. 登录系统，进入"网关管理" → "数据同步"菜单
2. 选择类型，点击"开始检查"，验证一致性检查结果
3. 点击"渠道同步"，验证全量同步功能
4. 查看同步日志列表

---

## 执行顺序总结

1. **Task 1-2**: 网关侧代码（可独立开发测试）
2. **Task 3-7**: Admin 侧代码（依赖 Task 3 数据库）
3. **Task 8-11**: 前端代码（依赖 Task 7 接口）
4. **Task 12**: 最终验证

所有代码完成后，统一进行代码审查和提交。