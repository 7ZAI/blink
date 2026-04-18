# 实例管理页面分组切换功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在实例管理页面添加分组切换功能，支持将离线/下线状态的实例切换到不同的路由分组

**Architecture:** 后端新增 `switchInstanceGroup` API，通过修改 Nacos 配置文件中的 `dynamicRoute.group` 字段实现分组切换；前端在操作列添加「切换分组」按钮和弹窗

**Tech Stack:** Spring Boot 3.2, MyBatis-Plus, Nacos Config, Vue 3, Element Plus, TypeScript

---

## 文件结构

### 后端新增文件
- `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/SwitchInstanceGroupReq.java` - 切换分组请求 DTO

### 后端修改文件
- `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/ErrCodeConstant.java` - 新增错误码
- `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/GatewayInstanceService.java` - 新增服务接口方法
- `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/GatewayInstanceServiceImpl.java` - 实现切换分组逻辑
- `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/GatewayInstanceController.java` - 新增 API 接口

### 前端修改文件
- `frontend/packages/gateway-admin/src/api/instance.ts` - 新增 API 方法
- `frontend/packages/gateway-admin/src/views/instance/index.vue` - 添加按钮和弹窗
- `frontend/packages/gateway-admin/src/locales/zh-cn.ts` - 中文国际化
- `frontend/packages/gateway-admin/src/locales/en-us.ts` - 英文国际化

---

## Task 1: 后端新增错误码常量

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/ErrCodeConstant.java`

- [ ] **Step 1: 添加新的错误码常量**

在 `ErrCodeConstant.java` 中添加以下常量：

```java
// 在 ErrCodeConstant 接口中添加

/**
 * 在线实例不允许切换分组
 */
String INSTANCE_ONLINE_CANNOT_SWITCH = "GATE0030";

/**
 * 目标分组配置不存在
 */
String TARGET_GROUP_CONFIG_NOT_EXIST = "GATE0031";

/**
 * 实例配置文件不存在
 */
String INSTANCE_CONFIG_NOT_EXIST = "GATE0032";

/**
 * 切换分组失败
 */
String SWITCH_GROUP_FAILED = "GATE0033";
```

- [ ] **Step 2: 提交代码**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/ErrCodeConstant.java
git commit -m "feat(gateway-admin): 新增实例分组切换相关错误码

GATE0030: 在线实例不允许切换分组
GATE0031: 目标分组配置不存在
GATE0032: 实例配置文件不存在
GATE0033: 切换分组失败

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 2: 后端新增请求 DTO

**Files:**
- Create: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/SwitchInstanceGroupReq.java`

- [ ] **Step 1: 创建 SwitchInstanceGroupReq.java**

```java
package com.blink.gateway.admin.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 切换实例分组请求参数
 *
 * @author binblink
 */
@Getter
@Setter
public class SwitchInstanceGroupReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    @NotBlank(message = "实例ID不能为空")
    private String instanceId;

    /**
     * 目标分组标识
     */
    @NotBlank(message = "目标分组不能为空")
    private String targetGroupKey;
}
```

- [ ] **Step 2: 提交代码**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/SwitchInstanceGroupReq.java
git commit -m "feat(gateway-admin): 新增切换实例分组请求 DTO

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 3: 后端服务接口新增方法

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/GatewayInstanceService.java`

- [ ] **Step 1: 添加 switchInstanceGroup 方法**

在 `GatewayInstanceService.java` 接口中添加：

```java
// 在接口末尾添加（import 需要添加 SwitchInstanceGroupReq）

import com.blink.gateway.admin.dto.req.SwitchInstanceGroupReq;

/**
 * 切换实例分组
 * 将实例从当前分组切换到目标分组
 *
 * @param req 切换分组请求参数
 * @return 操作结果
 */
ResponseDTO<EmptyBody> switchInstanceGroup(SwitchInstanceGroupReq req);
```

- [ ] **Step 2: 提交代码**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/GatewayInstanceService.java
git commit -m "feat(gateway-admin): 服务接口新增切换实例分组方法

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 4: 后端服务实现类添加依赖注入

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/GatewayInstanceServiceImpl.java`

- [ ] **Step 1: 添加 NacosConfigComponent 依赖注入**

在 `GatewayInstanceServiceImpl.java` 中添加依赖注入（如果不存在）：

```java
// 在类开头添加 import
import com.blink.gateway.admin.component.NacosConfigComponent;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;
import java.util.Map;
import java.util.LinkedHashMap;

// 在 Resource 注入区域添加
@Resource
private NacosConfigComponent nacosConfigComponent;
```

- [ ] **Step 2: 验证 NacosConfigComponent 存在**

确认 `NacosConfigComponent` 类中已有 `getConfig` 和 `configPublisher` 方法。如果没有，需要添加。

- [ ] **Step 3: 提交代码**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/GatewayInstanceServiceImpl.java
git commit -m "feat(gateway-admin): 服务实现类添加 Nacos 配置组件依赖

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 5: 后端实现切换分组核心逻辑

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/GatewayInstanceServiceImpl.java`

- [ ] **Step 1: 添加错误码 import**

```java
// 在静态 import 区域添加
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_ONLINE_CANNOT_SWITCH;
import static com.blink.gateway.admin.constants.ErrCodeConstant.TARGET_GROUP_CONFIG_NOT_EXIST;
import static com.blink.gateway.admin.constants.ErrCodeConstant.INSTANCE_CONFIG_NOT_EXIST;
import static com.blink.gateway.admin.constants.ErrCodeConstant.SWITCH_GROUP_FAILED;
```

- [ ] **Step 2: 添加请求 DTO import**

```java
import com.blink.gateway.admin.dto.req.SwitchInstanceGroupReq;
```

- [ ] **Step 3: 实现 switchInstanceGroup 方法**

在 `GatewayInstanceServiceImpl.java` 末尾添加以下方法：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public ResponseDTO<EmptyBody> switchInstanceGroup(SwitchInstanceGroupReq req) {
    try {
        String instanceId = req.getInstanceId();
        String targetGroupKey = req.getTargetGroupKey();

        // 1. 查询实例信息
        LambdaQueryWrapper<GatewayInstanceDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GatewayInstanceDO::getInstanceId, instanceId);
        GatewayInstanceDO instanceDO = gatewayInstanceMapper.selectOne(queryWrapper);

        if (ObjectUtil.isNull(instanceDO)) {
            BlinkException.throwBusinessException(GATEWAY_INSTANCE_NOT_EXIST);
        }

        // 2. 校验实例状态（必须是离线或下线状态）
        if (INSTANCE_STATUS_ONLINE.equals(instanceDO.getStatus())
                || INSTANCE_STATUS_DRAINING.equals(instanceDO.getStatus())) {
            log.warn("[GatewayInstance] 在线实例不允许切换分组 | instanceId: {}, status: {}",
                    instanceId, instanceDO.getStatus());
            BlinkException.throwBusinessException(INSTANCE_ONLINE_CANNOT_SWITCH);
        }

        // 3. 校验目标分组配置是否存在
        validateTargetGroupConfig(targetGroupKey, instanceDO.getStorageMode());

        // 4. 更新 Nacos 配置文件中的分组
        String oldGroupKey = instanceDO.getGroupKey();
        updateInstanceGroupConfig(instanceId, targetGroupKey);

        // 5. 更新数据库
        instanceDO.setGroupKey(targetGroupKey);
        gatewayInstanceMapper.updateById(instanceDO);

        log.info("[GatewayInstance] 实例分组切换成功 | instanceId: {}, oldGroup: {}, newGroup: {}",
                instanceId, oldGroupKey, targetGroupKey);

        return ResponseDTO.newSuccessInstance();
    } catch (BlinkException e) {
        throw e;
    } catch (Exception e) {
        log.error("[GatewayInstance] 切换实例分组失败 | error: {}", e.getMessage(), e);
        throw new BlinkException("切换实例分组失败：" + e.getMessage(), e, SWITCH_GROUP_FAILED);
    }
}

/**
 * 校验目标分组配置是否存在
 *
 * @param targetGroupKey 目标分组标识
 * @param storageMode    存储模式
 */
private void validateTargetGroupConfig(String targetGroupKey, String storageMode) {
    // 如果是默认分组，直接通过
    if ("default".equals(targetGroupKey)) {
        return;
    }

    // 根据存储模式校验配置是否存在
    if ("redis".equals(storageMode)) {
        // Redis 模式：检查 Key 是否存在
        String routeKey = "blink:gateway:routes:" + targetGroupKey + ":default";
        boolean exists = redisClient.hasKey(routeKey);
        if (!exists) {
            log.warn("[GatewayInstance] 目标分组配置不存在 | storageMode: redis, routeKey: {}", routeKey);
            BlinkException.throwBusinessException(TARGET_GROUP_CONFIG_NOT_EXIST);
        }
    } else if ("nacos".equals(storageMode)) {
        // Nacos 模式：检查配置文件是否存在
        String dataId = "gateway-routes-" + targetGroupKey + ".json";
        String config = nacosConfigComponent.getConfig(dataId, "DEFAULT_GROUP");
        if (StrUtil.isBlank(config)) {
            log.warn("[GatewayInstance] 目标分组配置不存在 | storageMode: nacos, dataId: {}", dataId);
            BlinkException.throwBusinessException(TARGET_GROUP_CONFIG_NOT_EXIST);
        }
    }

    log.info("[GatewayInstance] 目标分组配置校验通过 | targetGroupKey: {}, storageMode: {}",
            targetGroupKey, storageMode);
}

/**
 * 更新实例配置文件中的分组字段
 *
 * @param instanceId     实例ID
 * @param targetGroupKey 目标分组标识
 */
private void updateInstanceGroupConfig(String instanceId, String targetGroupKey) {
    String dataId = "blink-gateway-" + instanceId + ".yaml";
    String group = "DEFAULT_GROUP";

    // 1. 获取当前配置
    String configContent = nacosConfigComponent.getConfig(dataId, group);
    if (StrUtil.isBlank(configContent)) {
        log.warn("[GatewayInstance] 实例配置文件不存在 | dataId: {}", dataId);
        BlinkException.throwBusinessException(INSTANCE_CONFIG_NOT_EXIST);
    }

    // 2. 解析 YAML 并修改 group 字段
    String updatedConfig = updateYamlGroupField(configContent, targetGroupKey);

    // 3. 发布更新后的配置
    nacosConfigComponent.configPublisher(dataId, group, updatedConfig);

    log.info("[GatewayInstance] Nacos 配置更新成功 | dataId: {}, newGroup: {}", dataId, targetGroupKey);
}

/**
 * 更新 YAML 配置中的 group 字段
 *
 * @param yamlContent    原始 YAML 内容
 * @param targetGroupKey 目标分组标识
 * @return 更新后的 YAML 内容
 */
@SuppressWarnings("unchecked")
private String updateYamlGroupField(String yamlContent, String targetGroupKey) {
    try {
        Yaml yaml = new Yaml();
        Map<String, Object> yamlMap = yaml.load(yamlContent);

        if (yamlMap == null) {
            yamlMap = new LinkedHashMap<>();
        }

        // 获取或创建 blink.gateway.dynamicRoute 路径
        Map<String, Object> blinkMap = (Map<String, Object>) yamlMap.computeIfAbsent("blink", k -> new LinkedHashMap<>());
        Map<String, Object> gatewayMap = (Map<String, Object>) blinkMap.computeIfAbsent("gateway", k -> new LinkedHashMap<>());
        Map<String, Object> dynamicRouteMap = (Map<String, Object>) gatewayMap.computeIfAbsent("dynamicRoute", k -> new LinkedHashMap<>());

        // 更新 group 字段
        dynamicRouteMap.put("group", targetGroupKey);

        // 重新序列化为 YAML
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml outputYaml = new Yaml(options);

        return outputYaml.dump(yamlMap);
    } catch (Exception e) {
        log.error("[GatewayInstance] 解析 YAML 配置失败 | error: {}", e.getMessage(), e);
        throw new BlinkException("解析配置文件失败：" + e.getMessage(), e, SWITCH_GROUP_FAILED);
    }
}
```

- [ ] **Step 4: 提交代码**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/GatewayInstanceServiceImpl.java
git commit -m "feat(gateway-admin): 实现实例分组切换核心逻辑

- 校验实例状态必须为离线或下线
- 校验目标分组配置存在性
- 更新 Nacos 配置文件中的 dynamicRoute.group
- 更新数据库 group_key 字段

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 6: 后端控制器新增 API 接口

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/GatewayInstanceController.java`

- [ ] **Step 1: 添加 import**

```java
import com.blink.gateway.admin.dto.req.SwitchInstanceGroupReq;
```

- [ ] **Step 2: 添加切换分组接口**

在 `GatewayInstanceController.java` 中添加：

```java
/**
 * 切换实例分组
 * 将实例从当前分组切换到目标分组（仅限离线/下线状态实例）
 *
 * @param reqDto 请求参数
 * @return 操作结果
 */
@RecordLog(type = LogType.OPERATION, description = "切换实例分组")
@PostMapping("/switchInstanceGroup")
public ResponseDTO<EmptyBody> switchInstanceGroup(@RequestBody @Validated RequestDTO<SwitchInstanceGroupReq> reqDto) {
    return gatewayInstanceService.switchInstanceGroup(reqDto.getBody());
}
```

- [ ] **Step 3: 提交代码**

```bash
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/controller/GatewayInstanceController.java
git commit -m "feat(gateway-admin): 新增切换实例分组 API 接口

POST /gatewayInstance/switchInstanceGroup

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 7: 前端新增 API 方法

**Files:**
- Modify: `frontend/packages/gateway-admin/src/api/instance.ts`

- [ ] **Step 1: 添加切换分组 API 方法**

在 `instance.ts` 文件末尾添加：

```typescript
/**
 * 切换实例分组请求参数
 */
export interface SwitchInstanceGroupParams {
  instanceId: string
  targetGroupKey: string
}

/**
 * 切换实例分组
 */
export const switchInstanceGroup = (params: SwitchInstanceGroupParams): Promise<void> => {
  return request.post('/gatewayInstance/switchInstanceGroup', { body: params })
}
```

- [ ] **Step 2: 更新 API 对象导出**

在 `instanceApi` 对象中添加：

```typescript
export const instanceApi = {
  queryInstanceList,
  deleteInstance,
  getInstanceDetailWithMetrics,
  onlineInstance,
  offlineInstance,
  gracefulOfflineInstance,
  refreshInstanceStatus,
  switchInstanceGroup,  // 新增
}
```

- [ ] **Step 3: 提交代码**

```bash
git add frontend/packages/gateway-admin/src/api/instance.ts
git commit -m "feat(gateway-admin): 前端新增切换实例分组 API 方法

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 8: 前端添加国际化文本

**Files:**
- Modify: `frontend/packages/gateway-admin/src/locales/zh-cn.ts`
- Modify: `frontend/packages/gateway-admin/src/locales/en-us.ts`

- [ ] **Step 1: 添加中文国际化**

在 `zh-cn.ts` 的 `instance` 对象中添加：

```typescript
instance: {
  // ... 现有内容 ...

  // 分组切换相关
  switchGroup: '切换分组',
  currentGroup: '当前分组',
  targetGroup: '目标分组',
  switchGroupSuccess: '分组切换成功',
  switchGroupConfirm: '确定将实例切换到分组「{group}」吗？',
  switchGroupOnlineWarning: '在线实例不允许切换分组',
},
```

- [ ] **Step 2: 添加英文国际化**

在 `en-us.ts` 的 `instance` 对象中添加（如果文件存在）：

```typescript
instance: {
  // ... 现有内容 ...

  // Group switch
  switchGroup: 'Switch Group',
  currentGroup: 'Current Group',
  targetGroup: 'Target Group',
  switchGroupSuccess: 'Group switched successfully',
  switchGroupConfirm: 'Are you sure to switch instance to group "{group}"?',
  switchGroupOnlineWarning: 'Online instance cannot switch group',
},
```

- [ ] **Step 3: 提交代码**

```bash
git add frontend/packages/gateway-admin/src/locales/zh-cn.ts frontend/packages/gateway-admin/src/locales/en-us.ts
git commit -m "feat(gateway-admin): 添加实例分组切换国际化文本

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 9: 前端添加切换分组按钮和弹窗

**Files:**
- Modify: `frontend/packages/gateway-admin/src/views/instance/index.vue`

- [ ] **Step 1: 添加 import**

在 `<script setup>` 的 import 区域添加：

```typescript
import { Switch } from '@element-plus/icons-vue'
import { switchInstanceGroup } from '@/api/instance'
```

- [ ] **Step 2: 添加弹窗状态和方法**

在 `// ==================== 弹窗状态 ====================` 区域后添加：

```typescript
// ==================== 切换分组弹窗 ====================

const switchGroupDialogVisible = ref(false)

const switchGroupForm = reactive({
  instanceId: '',
  currentGroup: '',
  targetGroupKey: '',
})

const handleSwitchGroup = (row: InstanceInfo) => {
  switchGroupForm.instanceId = row.instanceId
  switchGroupForm.currentGroup = row.groupKey || 'default'
  switchGroupForm.targetGroupKey = ''
  switchGroupDialogVisible.value = true
}

const confirmSwitchGroup = async () => {
  if (!switchGroupForm.targetGroupKey) {
    ElMessage.warning(t('common.pleaseSelect'))
    return
  }

  submitting.value = true
  try {
    await switchInstanceGroup({
      instanceId: switchGroupForm.instanceId,
      targetGroupKey: switchGroupForm.targetGroupKey,
    })
    ElMessage.success(t('instance.switchGroupSuccess'))
    switchGroupDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('Switch group error:', error)
  } finally {
    submitting.value = false
  }
}
```

- [ ] **Step 3: 在操作列添加切换分组按钮**

在模板的操作列 `<div class="operation-buttons">` 中，在「详情」按钮后添加：

```vue
<!-- 切换分组按钮 - 仅离线/下线状态显示 -->
<el-button
  v-if="row.status === INSTANCE_STATUS.OFFLINE || row.status === INSTANCE_STATUS.SHUTDOWN"
  type="primary"
  link
  size="small"
  @click="handleSwitchGroup(row)"
>
  <el-icon><Switch /></el-icon>
  {{ t('instance.switchGroup') }}
</el-button>
```

- [ ] **Step 4: 添加切换分组弹窗**

在下线弹窗 `</el-dialog>` 后添加：

```vue
<!-- 切换分组弹窗 -->
<el-dialog
  v-model="switchGroupDialogVisible"
  :title="t('instance.switchGroup')"
  width="500px"
  :close-on-click-modal="false"
  :lock-scroll="false"
>
  <el-form :model="switchGroupForm" label-width="100px">
    <el-form-item :label="t('common.instanceId')">
      <el-input v-model="switchGroupForm.instanceId" disabled />
    </el-form-item>
    <el-form-item :label="t('instance.currentGroup')">
      <el-input v-model="switchGroupForm.currentGroup" disabled />
    </el-form-item>
    <el-form-item :label="t('instance.targetGroup')" required>
      <el-select v-model="switchGroupForm.targetGroupKey" :placeholder="t('common.pleaseSelect')" style="width: 100%">
        <el-option
          v-for="group in groupOptions"
          :key="group.groupKey"
          :label="group.groupName"
          :value="group.groupKey"
          :disabled="group.groupKey === switchGroupForm.currentGroup"
        />
      </el-select>
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="switchGroupDialogVisible = false">{{ t('common.cancel') }}</el-button>
    <el-button type="primary" :loading="submitting" @click="confirmSwitchGroup">
      {{ t('common.confirm') }}
    </el-button>
  </template>
</el-dialog>
```

- [ ] **Step 5: 提交代码**

```bash
git add frontend/packages/gateway-admin/src/views/instance/index.vue
git commit -m "feat(gateway-admin): 实例管理页面添加切换分组功能

- 操作列新增「切换分组」按钮（仅离线/下线实例可见）
- 新增分组切换弹窗
- 支持选择目标分组并切换

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 10: 编译验证

**Files:**
- 无文件修改，仅验证

- [ ] **Step 1: 编译后端代码**

```bash
./gradlew :blink-gateway:gateway-admin:compileJava
```

预期输出：`BUILD SUCCESSFUL`

- [ ] **Step 2: 检查前端类型**

```bash
cd frontend && pnpm --filter @blink/gateway-admin type-check
```

预期输出：无错误

- [ ] **Step 3: 如果有编译错误，修复后提交**

---

## Task 11: 最终提交

- [ ] **Step 1: 确认所有文件已提交**

```bash
git status
```

- [ ] **Step 2: 如果有未提交的文件，完成提交**

```bash
git add .
git commit -m "feat(gateway-admin): 实现实例管理页面分组切换功能

功能概述：
- 在操作列添加「切换分组」按钮
- 仅对离线/下线状态的实例显示
- 切换前校验目标分组配置是否存在
- 通过修改 Nacos 配置文件实现分组切换

后端改动：
- 新增 SwitchInstanceGroupReq DTO
- 新增 switchInstanceGroup API
- 实现 Nacos 配置文件 YAML 解析和更新

前端改动：
- 新增切换分组按钮和弹窗
- 新增 API 调用方法
- 新增国际化文本

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 自检清单

- [ ] 所有代码遵循项目编码规范
- [ ] 后端使用 Hutool 工具类进行空值判断
- [ ] 后端日志使用 `@Slf4j` 注解
- [ ] 后端错误码使用预定义常量
- [ ] 前端使用 TypeScript 类型定义
- [ ] 前端国际化文本完整
- [ ] 所有 commit 消息使用中文
