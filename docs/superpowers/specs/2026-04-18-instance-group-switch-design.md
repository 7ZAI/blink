# 实例管理页面分组切换功能设计

## 1. 功能概述

在实例管理页面的操作列添加「切换分组」功能，允许管理员将离线/下线状态的实例切换到不同的路由分组。

## 2. 业务规则

### 2.1 前置条件

- 实例状态必须是**离线(1)**或**下线(2)**状态
- 在线实例和排空中实例不允许切换分组
- 目标分组必须存在对应的路由配置

### 2.2 配置校验规则

切换前需要校验目标分组是否存在对应的配置：

| 存储模式 | 校验方式 |
|---------|---------|
| Nacos | 检查配置文件 `gateway-routes-{targetGroupKey}.json` 是否存在 |
| Redis | 检查 Key `blink:gateway:routes:{targetGroupKey}:default` 是否存在 |

## 3. 配置文件规则

### 3.1 实例配置文件

- **dataId 格式**：`blink-gateway-{instanceId}.yaml`
- **group**：`DEFAULT_GROUP`
- **instanceId 格式**：`{serviceId}:{host}:{port}`，例如 `gateway-app:10.141.92.120:8002`

### 3.2 需要修改的配置项

```yaml
blink:
  gateway:
    dynamicRoute:
      mode: redis          # 存储模式：redis/nacos
      group: default       # ← 需要修改的字段
      redis:
        routeSuffix: default
```

### 3.3 路由配置文件规则

**Nacos 模式：**
- dataId 格式：`gateway-routes-{groupKey}.json`（默认分组为 `gateway-routes.json`）
- group：`DEFAULT_GROUP`

**Redis 模式：**
- Key 格式：`blink:gateway:routes:{groupKey}:{routeSuffix}`
- 默认 routeSuffix：`default`

## 4. 接口设计

### 4.1 切换分组接口

**请求：**
```
POST /gatewayInstance/switchInstanceGroup
```

**请求参数：**
```json
{
  "body": {
    "instanceId": "gateway-app:10.141.92.120:8002",
    "targetGroupKey": "group-a"
  }
}
```

**参数说明：**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| instanceId | String | 是 | 实例ID，格式：`{serviceId}:{host}:{port}` |
| targetGroupKey | String | 是 | 目标分组标识 |

**响应：**
```json
{
  "code": "00000",
  "msg": "success",
  "body": {}
}
```

### 4.2 错误码定义

| 错误码 | 说明 |
|-------|------|
| GATE0020 | 实例不存在 |
| GATE0030 | 在线实例不允许切换分组 |
| GATE0031 | 目标分组配置不存在 |
| GATE0032 | 实例配置文件不存在 |

## 5. 后端实现

### 5.1 新增类

**请求 DTO：**
```java
// SwitchInstanceGroupReq.java
@Getter
@Setter
public class SwitchInstanceGroupReq implements Serializable {

    @NotBlank(message = "实例ID不能为空")
    private String instanceId;

    @NotBlank(message = "目标分组不能为空")
    private String targetGroupKey;
}
```

### 5.2 服务层逻辑

```java
@Override
@Transactional(rollbackFor = Exception.class)
public ResponseDTO<EmptyBody> switchInstanceGroup(SwitchInstanceGroupReq req) {
    // 1. 查询实例信息
    GatewayInstanceDO instance = findByInstanceId(req.getInstanceId());
    if (instance == null) {
        BlinkException.throwBusinessException(GATEWAY_INSTANCE_NOT_EXIST);
    }

    // 2. 校验实例状态（必须是离线或下线状态）
    if (instance.getStatus() == INSTANCE_STATUS_ONLINE
        || instance.getStatus() == INSTANCE_STATUS_DRAINING) {
        BlinkException.throwBusinessException(INSTANCE_ONLINE_CANNOT_SWITCH);
    }

    // 3. 校验目标分组配置是否存在
    validateTargetGroupConfig(req.getTargetGroupKey(), instance.getStorageMode());

    // 4. 更新 Nacos 配置文件
    updateInstanceGroupConfig(req.getInstanceId(), req.getTargetGroupKey());

    // 5. 更新数据库
    instance.setGroupKey(req.getTargetGroupKey());
    gatewayInstanceMapper.updateById(instance);

    // 6. 记录日志
    log.info("[GatewayInstance] 实例分组切换成功 | instanceId: {}, oldGroup: {}, newGroup: {}",
        req.getInstanceId(), instance.getGroupKey(), req.getTargetGroupKey());

    return ResponseDTO.newSuccessInstance();
}
```

### 5.3 配置更新方法

```java
private void updateInstanceGroupConfig(String instanceId, String targetGroupKey) {
    String dataId = "blink-gateway-" + instanceId + ".yaml";
    String group = "DEFAULT_GROUP";

    // 1. 获取当前配置
    String configContent = nacosConfigComponent.getConfig(dataId, group);
    if (StrUtil.isBlank(configContent)) {
        BlinkException.throwBusinessException(INSTANCE_CONFIG_NOT_EXIST);
    }

    // 2. 解析 YAML 并修改 group 字段
    String updatedConfig = updateYamlGroupField(configContent, targetGroupKey);

    // 3. 发布更新后的配置
    nacosConfigComponent.configPublisher(dataId, group, updatedConfig);
}
```

## 6. 前端实现

### 6.1 操作列变更

在操作列添加「切换分组」按钮，位于「详情」按钮之后：

```vue
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

### 6.2 分组切换弹窗

```vue
<el-dialog
  v-model="switchGroupDialogVisible"
  :title="t('instance.switchGroup')"
  width="500px"
>
  <el-form :model="switchGroupForm" label-width="100px">
    <el-form-item :label="t('common.instanceId')">
      <el-input v-model="switchGroupForm.instanceId" disabled />
    </el-form-item>
    <el-form-item :label="t('instance.currentGroup')">
      <el-input v-model="switchGroupForm.currentGroup" disabled />
    </el-form-item>
    <el-form-item :label="t('instance.targetGroup')">
      <el-select v-model="switchGroupForm.targetGroupKey" style="width: 100%">
        <el-option
          v-for="group in groupOptions"
          :key="group.groupKey"
          :label="group.groupName"
          :value="group.groupKey"
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

### 6.3 API 调用

```typescript
// api/instance.ts 新增
export const switchInstanceGroup = (params: {
  instanceId: string
  targetGroupKey: string
}): Promise<void> => {
  return request.post('/gatewayInstance/switchInstanceGroup', { body: params })
}
```

## 7. 国际化

### 7.1 中文 (zh-CN)

```json
{
  "instance": {
    "switchGroup": "切换分组",
    "currentGroup": "当前分组",
    "targetGroup": "目标分组",
    "switchGroupSuccess": "分组切换成功",
    "switchGroupConfirm": "确定将实例切换到分组「{group}」吗？"
  }
}
```

### 7.2 英文 (en-US)

```json
{
  "instance": {
    "switchGroup": "Switch Group",
    "currentGroup": "Current Group",
    "targetGroup": "Target Group",
    "switchGroupSuccess": "Group switched successfully",
    "switchGroupConfirm": "Are you sure to switch instance to group \"{group}\"?"
  }
}
```

## 8. 权限控制

复用现有实例管理权限，不新增独立权限点。

## 9. 测试要点

### 9.1 功能测试

| 测试场景 | 预期结果 |
|---------|---------|
| 在线实例切换分组 | 提示「在线实例不允许切换分组」 |
| 离线实例切换到不存在的分组 | 提示「目标分组配置不存在」 |
| 离线实例切换到存在的分组 | 成功切换，Nacos配置和数据库都已更新 |
| 实例配置文件不存在 | 提示「实例配置文件不存在」 |

### 9.2 边界测试

- 分组配置文件格式错误（非有效 YAML）
- 网络超时处理
- 并发切换同一实例

## 10. 影响范围

### 10.1 后端修改

- 新增 `SwitchInstanceGroupReq.java`
- 修改 `GatewayInstanceController.java` 新增接口
- 修改 `GatewayInstanceService.java` 新增方法
- 修改 `GatewayInstanceServiceImpl.java` 实现逻辑
- 新增错误码常量

### 10.2 前端修改

- 修改 `views/instance/index.vue` 添加按钮和弹窗
- 修改 `api/instance.ts` 新增 API 方法
- 修改国际化文件
