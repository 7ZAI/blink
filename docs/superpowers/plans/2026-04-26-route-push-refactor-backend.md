# 路由推送页面重构 - 后端实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 getGroupInstanceRoutes API，支持前端获取分组下实例的实际路由配置。

**Architecture:** 从分组下第一个在线实例通过 Actuator HTTP 端点获取路由定义，返回路由列表及来源信息。

**Tech Stack:** Spring Boot 3.2, MyBatis-Plus, Redis/Nacos, Actuator HTTP Client

---

## 文件结构

### 新建文件

| 文件 | 职责 |
|------|------|
| `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/GetGroupInstanceRoutesReq.java` | 获取分组实例路由请求 DTO |
| `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/GroupInstanceRoutesRsp.java` | 分组实例路由响应 DTO |

### 修改文件

| 文件 | 责任 |
|------|------|
| `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/RouteController.java` | 新增 getGroupInstanceRoutes 接口 |
| `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/RouteService.java` | 新增 getGroupInstanceRoutes 方法定义 |
| `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/RouteServiceImpl.java` | 实现 getGroupInstanceRoutes 方法 |

---

## Task 1: 创建请求 DTO

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/GetGroupInstanceRoutesReq.java`

- [ ] **Step 1: 创建 GetGroupInstanceRoutesReq 类**

```java
package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 获取分组实例路由请求
 * 从分组下在线实例获取实际加载的路由配置
 *
 * @author binblink
 * @since 2026-04-26
 */
@Getter
@Setter
public class GetGroupInstanceRoutesReq {

    /**
     * 路由分组（必填）
     */
    @NotBlank(message = "路由分组不能为空")
    private String routesGroup;
}
```

- [ ] **Step 2: 运行 Gradle 编译检查**

Run: `./gradlew :blink-gateway:gateway-admin:compileJava`
Expected: 编译成功，无错误

- [ ] **Step 3: Commit**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/GetGroupInstanceRoutesReq.java
git commit -m "feat(route): 新增 GetGroupInstanceRoutesReq 请求 DTO"
```

---

## Task 2: 创建响应 DTO

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/GroupInstanceRoutesRsp.java`

- [ ] **Step 1: 创建 GroupInstanceRoutesRsp 类**

```java
package com.blink.gateway.admin.dto.rsp;

import com.blink.gateway.admin.entity.GaRouteDO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 分组实例路由响应
 * 包含从实例获取的路由列表及来源信息
 *
 * @author binblink
 * @since 2026-04-26
 */
@Getter
@Setter
public class GroupInstanceRoutesRsp {

    /**
     * 来源实例 ID
     */
    private String instanceId;

    /**
     * 存储模式（redis/nacos）
     */
    private String storageMode;

    /**
     * 获取时间
     */
    private String timestamp;

    /**
     * 路由列表
     */
    private List<GaRouteDO> rows;

    /**
     * 路由总数
     */
    private Integer total;

    /**
     * 是否来自 Actuator
     */
    private Boolean fromActuator;

    /**
     * 错误信息（可选，获取失败时返回）
     */
    private String error;
}
```

- [ ] **Step 2: 运行 Gradle 编译检查**

Run: `./gradlew :blink-gateway:gateway-admin:compileJava`
Expected: 编译成功，无错误

- [ ] **Step 3: Commit**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/rsp/GroupInstanceRoutesRsp.java
git commit -m "feat(route): 新增 GroupInstanceRoutesRsp 响应 DTO"
```

---

## Task 3: 在 RouteService 接口中添加方法定义

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/RouteService.java`

- [ ] **Step 1: 添加 getGroupInstanceRoutes 方法签名**

首先阅读现有的 RouteService 接口：

Run: `head -100 /home/binblink/project/blink/blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/RouteService.java`

在接口末尾添加新方法（需要先阅读文件确定插入位置）：

```java
    /**
     * 获取分组下实例的实际路由
     * 从分组下第一个在线实例通过 Actuator 获取路由配置
     *
     * @param req 请求参数（包含路由分组）
     * @return 实例路由响应
     */
    ResponseDTO<GroupInstanceRoutesRsp> getGroupInstanceRoutes(GetGroupInstanceRoutesReq req);
```

同时需要添加 import：

```java
import com.blink.gateway.admin.dto.req.GetGroupInstanceRoutesReq;
import com.blink.gateway.admin.dto.rsp.GroupInstanceRoutesRsp;
```

- [ ] **Step 2: 运行 Gradle 编译检查**

Run: `./gradlew :blink-gateway:gateway-admin:compileJava`
Expected: 编译成功，无错误

- [ ] **Step 3: Commit**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/RouteService.java
git commit -m "feat(route): 在 RouteService 接口添加 getGroupInstanceRoutes 方法定义"
```

---

## Task 4: 实现 getGroupInstanceRoutes 方法

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/RouteServiceImpl.java`

- [ ] **Step 1: 在 RouteServiceImpl 中实现 getGroupInstanceRoutes 方法**

在 `getRouteDiff` 方法后添加实现：

```java
    @Override
    public ResponseDTO<GroupInstanceRoutesRsp> getGroupInstanceRoutes(GetGroupInstanceRoutesReq req) {
        // 参数校验
        if (StrUtil.isBlank(req.getRoutesGroup())) {
            BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
        }

        GroupInstanceRoutesRsp rsp = new GroupInstanceRoutesRsp();
        rsp.setTimestamp(java.time.LocalDateTime.now().toString());
        rsp.setFromActuator(true);

        // 1. 查询分组下第一个在线实例
        QueryInstanceReq queryReq = new QueryInstanceReq();
        queryReq.setGroupKey(req.getRoutesGroup());
        queryReq.setStatus(INSTANCE_STATUS_ONLINE);
        queryReq.setPageNum(1);
        queryReq.setPageSize(1);

        ResponseDTO<QueryInstanceListRsp> instanceListRsp = gatewayInstanceService.queryInstanceList(queryReq);
        if (ObjectUtil.isNull(instanceListRsp.getBody())
            || CollUtil.isEmpty(instanceListRsp.getBody().getRows())) {
            log.warn("[Route] 当前分组无在线实例，无法获取实例路由 | routesGroup: {}", req.getRoutesGroup());
            rsp.setRows(new ArrayList<>());
            rsp.setTotal(0);
            rsp.setError("当前分组无在线实例");
            return ResponseDTO.newSuccessInstance(rsp);
        }

        InstanceInfoVO firstInstance = instanceListRsp.getBody().getRows().get(0);
        String instanceId = firstInstance.getInstanceId();
        String storageMode = firstInstance.getStorageMode();

        rsp.setInstanceId(instanceId);
        rsp.setStorageMode(storageMode);

        log.info("[Route] 自动选择在线实例获取路由 | routesGroup: {}, instanceId: {}, storageMode: {}",
            req.getRoutesGroup(), instanceId, storageMode);

        // 2. 从实例获取路由
        GetInstanceRoutesFromActuatorReq actuatorReq = new GetInstanceRoutesFromActuatorReq();
        actuatorReq.setInstanceId(instanceId);

        try {
            ResponseDTO<InstanceRoutesRsp> instanceRoutesRsp = routePushService.getInstanceRoutesFromActuator(actuatorReq);

            if (ObjectUtil.isNull(instanceRoutesRsp) || ObjectUtil.isNull(instanceRoutesRsp.getBody())) {
                log.warn("[Route] 从实例获取路由失败 | instanceId: {}", instanceId);
                rsp.setRows(new ArrayList<>());
                rsp.setTotal(0);
                rsp.setError("从实例获取路由失败");
                return ResponseDTO.newSuccessInstance(rsp);
            }

            InstanceRoutesRsp instanceRoutes = instanceRoutesRsp.getBody();
            List<GaRouteDO> routes = instanceRoutes.getRows();

            rsp.setRows(routes);
            rsp.setTotal(instanceRoutes.getTotal());
            rsp.setFromActuator(instanceRoutes.getFromActuator());

            if (StrUtil.isNotBlank(instanceRoutes.getError())) {
                rsp.setError(instanceRoutes.getError());
            }

            log.info("[Route] 成功获取分组实例路由 | routesGroup: {}, instanceId: {}, count: {}",
                req.getRoutesGroup(), instanceId, rsp.getTotal());

        } catch (Exception e) {
            log.error("[Route] 获取实例路由异常 | instanceId: {}, error: {}", instanceId, e.getMessage(), e);
            rsp.setRows(new ArrayList<>());
            rsp.setTotal(0);
            rsp.setError("获取实例路由异常: " + e.getMessage());
        }

        return ResponseDTO.newSuccessInstance(rsp);
    }
```

同时需要添加 import（如果尚未存在）：

```java
import com.blink.gateway.admin.dto.req.GetGroupInstanceRoutesReq;
import com.blink.gateway.admin.dto.rsp.GroupInstanceRoutesRsp;
import com.blink.gateway.admin.dto.vo.InstanceInfoVO;
```

- [ ] **Step 2: 运行 Gradle 编译检查**

Run: `./gradlew :blink-gateway:gateway-admin:compileJava`
Expected: 编译成功，无错误

- [ ] **Step 3: 运行单元测试（如有）**

Run: `./gradlew :blink-gateway:gateway-admin:test --tests "*RouteService*"`
Expected: 测试通过

- [ ] **Step 4: Commit**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/RouteServiceImpl.java
git commit -m "feat(route): 实现 getGroupInstanceRoutes 方法"
```

---

## Task 5: 在 RouteController 中添加接口

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/RouteController.java`

- [ ] **Step 1: 添加 getGroupInstanceRoutes 接口方法**

在 RouteController 类的 `getRouteDiff` 方法附近添加新接口：

```java
    /**
     * 获取分组下实例的实际路由
     * 从分组下第一个在线实例通过 Actuator 获取路由配置
     *
     * @param reqDto 请求参数
     * @return 实例路由响应
     */
    @PostMapping("/getGroupInstanceRoutes")
    public ResponseDTO<GroupInstanceRoutesRsp> getGroupInstanceRoutes(
        @RequestBody @Validated RequestDTO<GetGroupInstanceRoutesReq> reqDto) {
        return routeService.getGroupInstanceRoutes(reqDto.getBody());
    }
```

同时需要添加 import：

```java
import com.blink.gateway.admin.dto.req.GetGroupInstanceRoutesReq;
import com.blink.gateway.admin.dto.rsp.GroupInstanceRoutesRsp;
```

- [ ] **Step 2: 运行 Gradle 编译检查**

Run: `./gradlew :blink-gateway:gateway-admin:compileJava`
Expected: 编译成功，无错误

- [ ] **Step 3: 启动应用验证接口**

Run: `./gradlew :blink-gateway:gateway-admin:bootRun`
Expected: 应用启动成功，日志显示接口注册成功

验证接口可访问（使用 curl 或 Postman）：

```bash
curl -X POST http://localhost:8080/route/getGroupInstanceRoutes \
  -H "Content-Type: application/json" \
  -d '{"body":{"routesGroup":"default"}}'
```

Expected: 返回响应包含 instanceId、rows、total 等字段

- [ ] **Step 4: Commit**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/RouteController.java
git commit -m "feat(route): 在 RouteController 添加 getGroupInstanceRoutes 接口"
```

---

## Task 6: 整体验证

**Files:**
- None (测试验证)

- [ ] **Step 1: 运行完整构建**

Run: `./gradlew :blink-gateway:gateway-admin:build`
Expected: 构建成功，包含编译、测试

- [ ] **Step 2: 验证前端调用**

启动前端开发服务器：

Run: `cd frontend && pnpm dev:gateway-admin`

在浏览器访问路由推送页面，选择路由分组，验证：
1. 左列仓库路由正确加载
2. 中列实例路由正确加载（包含 instanceId、storageMode）
3. 右列关联实例正确加载

- [ ] **Step 3: 最终 Commit**

```bash
git add -A
git commit -m "feat(route): 完成 getGroupInstanceRoutes API 实现 - 支持前端获取分组实例路由"
```

---

## 自我审查清单

完成后检查以下内容：

| 项目 | 检查结果 |
|------|----------|
| GetGroupInstanceRoutesReq 参数校验正确 | ✓/✗ |
| GroupInstanceRoutesRsp 包含所有必要字段 | ✓/✗ |
| RouteService 接口方法签名正确 | ✓/✗ |
| RouteServiceImpl 实现正确处理异常 | ✓/✗ |
| RouteController 接口注册成功 | ✓/✗ |
| 无 Java 编译错误 | ✓/✗ |
| 单元测试通过 | ✓/✗ |
| 前端调用成功 | ✓/✗ |