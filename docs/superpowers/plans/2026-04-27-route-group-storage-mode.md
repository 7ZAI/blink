# 路由分组存储方式实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为路由分组增加存储方式字段，实现新增分组时自动创建配置，删除分组时自动清理配置。

**Architecture:** 在 gateway_route_group 表添加 storage_mode 字段，Service 层在新增/删除分组时调用 Nacos/Redis 组件创建/删除配置。

**Tech Stack:** Spring Boot 3.2, MyBatis-Plus, Nacos Config, Redis, Vue 3, Element Plus

---

## 文件结构

**后端文件（gateway-admin 模块）：**

| 文件 | 操作 | 职责 |
|------|------|------|
| `constants/RouteConstant.java` | 修改 | 新增 Nacos 配置相关常量 |
| `constants/ErrCodeConstant.java` | 修改 | 新增存储相关错误码 |
| `entity/GatewayRouteGroupDO.java` | 修改 | 新增 storageMode 字段 |
| `dto/req/AddRouteGroupReq.java` | 修改 | 新增 storageMode 参数 |
| `dto/vo/RouteGroupVO.java` | 修改 | 新增 storageMode 响应字段 |
| `service/impl/GatewayRouteGroupServiceImpl.java` | 修改 | 新增配置创建/删除逻辑 |
| `component/NacosConfigComponent.java` | 修改 | 新增配置删除方法 |
| `sql/gateway_admin_init.sql` | 修改 | 新增字段 DDL |

**前端文件（gateway-admin 应用）：**

| 文件 | 操作 | 职责 |
|------|------|------|
| `api/routeGroup.ts` | 修改 | 更新类型定义 |
| `views/routeGroup/index.vue` | 修改 | 新增存储方式选择 |
| `locales/zh-cn.ts` | 修改 | 新增中文翻译 |
| `locales/en-us.ts` | 修改 | 新增英文翻译 |

---

## Task 1: 数据库变更

**Files:**
- Modify: `blink-gateway/sql/gateway_admin_init.sql`

- [ ] **Step 1: 添加 storage_mode 字段 DDL**

在 `gateway_route_group` 表定义中添加 `storage_mode` 字段。

找到 `gateway_route_group` 表创建语句，在 `group_name` 字段后添加：

```sql
CREATE TABLE `gateway_route_group` (
  `group_id` int NOT NULL AUTO_INCREMENT COMMENT '分组ID',
  `group_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分组标识（业务唯一键）',
  `group_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分组名称',
  `storage_mode` varchar(16) DEFAULT 'nacos' COMMENT '存储方式：nacos/redis',
  `status` tinyint DEFAULT '1' COMMENT '状态：1启用 0禁用',
  `remark` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注说明',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`group_id`),
  UNIQUE KEY `uk_group_key` (`group_key`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关路由分组表';
```

同时更新初始化数据：

```sql
INSERT INTO gateway_route_group (group_id, group_key, group_name, storage_mode, status, remark, create_by, create_time) VALUES
(1, 'default', '默认分组', 'nacos', 1, '系统默认分组', 'admin', NOW());
```

- [ ] **Step 2: 添加增量变更 SQL**

在文件末尾添加增量变更 SQL（用于已有数据库升级）：

```sql
-- ============================================
-- 路由分组表增加存储方式字段
-- ============================================
ALTER TABLE gateway_route_group
ADD COLUMN IF NOT EXISTS storage_mode VARCHAR(16) DEFAULT 'nacos' COMMENT '存储方式：nacos/redis'
AFTER group_name;

-- 更新已有数据的默认值
UPDATE gateway_route_group SET storage_mode = 'nacos' WHERE storage_mode IS NULL;
```

---

## Task 2: 后端常量定义

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/RouteConstant.java`
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/ErrCodeConstant.java`

- [ ] **Step 1: 添加 Nacos 配置常量**

在 `RouteConstant.java` 中添加：

```java
// ==================== Nacos 路由配置常量 ====================

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

- [ ] **Step 2: 添加错误码常量**

在 `ErrCodeConstant.java` 中添加：

```java
// ============ 存储方式错误码 GATE0213-GATE0215 ============

/**
 * 存储方式不合法
 */
String STORAGE_MODE_INVALID = "GATE0213";

/**
 * 创建路由配置失败
 */
String CREATE_ROUTE_CONFIG_FAILED = "GATE0214";

/**
 * 删除路由配置失败
 */
String DELETE_ROUTE_CONFIG_FAILED = "GATE0215";
```

---

## Task 3: 后端实体和 DTO 变更

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/entity/GatewayRouteGroupDO.java`
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/AddRouteGroupReq.java`
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/RouteGroupVO.java`

- [ ] **Step 1: 实体类添加 storageMode 字段**

在 `GatewayRouteGroupDO.java` 中，在 `groupName` 字段后添加：

```java
/**
 * 存储方式：nacos/redis
 */
@TableField("storage_mode")
private String storageMode;
```

- [ ] **Step 2: 新增请求 DTO 添加 storageMode 字段**

在 `AddRouteGroupReq.java` 中，在 `groupName` 字段后添加：

```java
/**
 * 存储方式：nacos/redis，默认 nacos
 */
@NotBlank(message = "存储方式不能为空")
private String storageMode = "nacos";
```

- [ ] **Step 3: 响应 VO 添加 storageMode 字段**

在 `RouteGroupVO.java` 中，在 `groupName` 字段后添加：

```java
/**
 * 存储方式：nacos/redis
 */
private String storageMode;
```

---

## Task 4: Nacos 配置组件扩展

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/component/NacosConfigComponent.java`

- [ ] **Step 1: 添加配置删除方法**

在 `NacosConfigComponent.java` 中添加：

```java
/**
 * 删除 Nacos 配置
 *
 * @param dataId  配置文件id
 * @param groupId 配置文件组别
 * @throws BlinkException 删除失败时抛出异常
 */
public void deleteConfig(String dataId, String groupId) throws BlinkException {
    try {
        ConfigService configService = nacosConfigManager.getConfigService();
        boolean isDeleted = configService.removeConfig(dataId, groupId);

        if (isDeleted) {
            log.info("[NacosConfig] 删除配置成功 | dataId: {}, groupId: {}", dataId, groupId);
        } else {
            log.warn("[NacosConfig] 删除配置失败，配置不存在 | dataId: {}, groupId: {}", dataId, groupId);
        }
    } catch (NacosException e) {
        log.error("[NacosConfig] 删除配置失败 | dataId: {}, groupId: {}, error: {}", dataId, groupId, e.getMessage(), e);
        throw new BlinkException(e, e.getMessage());
    }
}
```

---

## Task 5: Service 层逻辑变更

**Files:**
- Modify: `blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/GatewayRouteGroupServiceImpl.java`

- [ ] **Step 1: 添加依赖注入**

在 `GatewayRouteGroupServiceImpl` 类中添加依赖：

```java
@Resource
private NacosConfigComponent nacosConfigComponent;

@Resource
private RedisClient redisClient;
```

- [ ] **Step 2: 新增私有方法 - 校验存储方式**

```java
/**
 * 校验存储方式是否合法
 *
 * @param storageMode 存储方式
 */
private void validateStorageMode(String storageMode) {
    if (StrUtil.isBlank(storageMode)) {
        BlinkException.throwBusinessException(PARAMETER_NOT_NULL);
    }
    if (!RouteConstant.STORAGE_MODE_NACOS.equals(storageMode)
            && !RouteConstant.STORAGE_MODE_REDIS.equals(storageMode)) {
        log.warn("[RouteGroup] 存储方式不合法 | storageMode: {}", storageMode);
        BlinkException.throwBusinessException(STORAGE_MODE_INVALID);
    }
}
```

- [ ] **Step 3: 新增私有方法 - 创建路由配置**

```java
/**
 * 创建路由配置
 *
 * @param groupKey    分组标识
 * @param storageMode 存储方式
 */
private void createRouteConfig(String groupKey, String storageMode) {
    try {
        if (RouteConstant.STORAGE_MODE_NACOS.equals(storageMode)) {
            // Nacos: 发布空数组配置
            String dataId = RouteConstant.NACOS_ROUTE_CONFIG_PREFIX + "-" + groupKey + RouteConstant.NACOS_ROUTE_CONFIG_SUFFIX;
            nacosConfigComponent.configPublisher(dataId, RouteConstant.NACOS_ROUTE_CONFIG_GROUP, "[]");
            log.info("[RouteGroup] 创建 Nacos 路由配置成功 | dataId: {}, group: {}", dataId, RouteConstant.NACOS_ROUTE_CONFIG_GROUP);
        } else if (RouteConstant.STORAGE_MODE_REDIS.equals(storageMode)) {
            // Redis: 创建空 Hash（通过删除后重建确保干净状态）
            String routeKey = RedisCacheKeyConstant.GATEWAY_DYNAMIC_ROUTES_PREFIX + groupKey + ":default";
            redisClient.del(routeKey);
            log.info("[RouteGroup] 创建 Redis 路由配置成功 | routeKey: {}", routeKey);
        }
    } catch (Exception e) {
        log.error("[RouteGroup] 创建路由配置失败 | groupKey: {}, storageMode: {}, error: {}",
                groupKey, storageMode, e.getMessage(), e);
        BlinkException.throwBusinessException(CREATE_ROUTE_CONFIG_FAILED);
    }
}
```

- [ ] **Step 4: 新增私有方法 - 删除路由配置**

```java
/**
 * 删除路由配置
 *
 * @param groupKey    分组标识
 * @param storageMode 存储方式
 */
private void deleteRouteConfig(String groupKey, String storageMode) {
    try {
        if (RouteConstant.STORAGE_MODE_NACOS.equals(storageMode)) {
            // Nacos: 删除配置文件
            String dataId = RouteConstant.NACOS_ROUTE_CONFIG_PREFIX + "-" + groupKey + RouteConstant.NACOS_ROUTE_CONFIG_SUFFIX;
            nacosConfigComponent.deleteConfig(dataId, RouteConstant.NACOS_ROUTE_CONFIG_GROUP);
            log.info("[RouteGroup] 删除 Nacos 路由配置成功 | dataId: {}", dataId);
        } else if (RouteConstant.STORAGE_MODE_REDIS.equals(storageMode)) {
            // Redis: 删除 Hash Key
            String routeKey = RedisCacheKeyConstant.GATEWAY_DYNAMIC_ROUTES_PREFIX + groupKey + ":default";
            redisClient.del(routeKey);
            log.info("[RouteGroup] 删除 Redis 路由配置成功 | routeKey: {}", routeKey);
        }
    } catch (Exception e) {
        log.error("[RouteGroup] 删除路由配置失败 | groupKey: {}, storageMode: {}, error: {}",
                groupKey, storageMode, e.getMessage(), e);
        BlinkException.throwBusinessException(DELETE_ROUTE_CONFIG_FAILED);
    }
}
```

- [ ] **Step 5: 修改 addRouteGroup 方法**

修改 `addRouteGroup` 方法，在保存数据库前添加校验，保存后创建配置：

```java
@Override
public ResponseDTO<Void> addRouteGroup(AddRouteGroupReq req) {
    // 校验存储方式
    validateStorageMode(req.getStorageMode());

    // 检查 groupKey 是否已存在
    LambdaQueryWrapper<GatewayRouteGroupDO> existQuery = new LambdaQueryWrapper<>();
    existQuery.eq(GatewayRouteGroupDO::getGroupKey, req.getGroupKey());
    Long count = gatewayRouteGroupMapper.selectCount(existQuery);

    if (count > 0) {
        log.warn("[RouteGroup] 新增分组失败，groupKey 已存在 | groupKey: {}", req.getGroupKey());
        BlinkException.throwBusinessException(ROUTE_GROUP_KEY_EXISTS);
    }

    // 构建实体并保存
    GatewayRouteGroupDO groupDO = BeanUtil.copyProperties(req, GatewayRouteGroupDO.class);
    gatewayRouteGroupMapper.insert(groupDO);

    // 创建路由配置（Nacos/Redis）
    createRouteConfig(req.getGroupKey(), req.getStorageMode());

    log.info("[RouteGroup] 新增分组成功 | groupId: {}, groupKey: {}, groupName: {}, storageMode: {}",
            groupDO.getGroupId(), groupDO.getGroupKey(), groupDO.getGroupName(), groupDO.getStorageMode());

    return ResponseDTO.newSuccessInstance(null);
}
```

- [ ] **Step 6: 修改 deleteRouteGroup 方法**

修改 `deleteRouteGroup` 方法，先删除配置再删除数据库：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public ResponseDTO<Void> deleteRouteGroup(DeleteRouteGroupReq req) {
    // 检查分组是否存在
    GatewayRouteGroupDO existGroup = gatewayRouteGroupMapper.selectById(req.getGroupId());

    if (ObjectUtil.isNull(existGroup)) {
        BlinkException.throwBusinessException(ROUTE_GROUP_NOT_EXIST);
    }

    // 检查分组下是否有关联的实例
    LambdaQueryWrapper<GatewayInstanceDO> instanceQuery = new LambdaQueryWrapper<>();
    instanceQuery.eq(GatewayInstanceDO::getGroupKey, existGroup.getGroupKey());
    Long instanceCount = gatewayInstanceMapper.selectCount(instanceQuery);

    if (instanceCount > 0) {
        log.warn("[RouteGroup] 删除分组失败，分组下存在关联实例 | groupId: {}, instanceCount: {}",
                req.getGroupId(), instanceCount);
        BlinkException.throwBusinessException(ROUTE_GROUP_HAS_INSTANCES);
    }

    // 先删除路由配置（Nacos/Redis）
    deleteRouteConfig(existGroup.getGroupKey(), existGroup.getStorageMode());

    // 删除分组
    gatewayRouteGroupMapper.deleteById(req.getGroupId());

    log.info("[RouteGroup] 删除分组成功 | groupId: {}, groupKey: {}, storageMode: {}",
            req.getGroupId(), existGroup.getGroupKey(), existGroup.getStorageMode());

    return ResponseDTO.newSuccessInstance(null);
}
```

- [ ] **Step 7: 修改 convertToVO 方法**

确保 `convertToVO` 方法复制 `storageMode` 字段：

```java
/**
 * 将 DO 转换为 VO
 *
 * @param groupDO 分组实体
 * @return 分组视图对象
 */
private RouteGroupVO convertToVO(GatewayRouteGroupDO groupDO) {
    RouteGroupVO vo = BeanUtil.copyProperties(groupDO, RouteGroupVO.class);
    return vo;
}
```

- [ ] **Step 8: 添加必要的 import**

```java
import com.blink.framework.common.constrant.RedisCacheKeyConstant;
import com.blink.framework.redis.component.RedisClient;
import com.blink.gateway.admin.component.NacosConfigComponent;

import static com.blink.gateway.admin.constants.ErrCodeConstant.CREATE_ROUTE_CONFIG_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.DELETE_ROUTE_CONFIG_FAILED;
import static com.blink.gateway.admin.constants.ErrCodeConstant.STORAGE_MODE_INVALID;
```

---

## Task 6: 前端 API 类型定义

**Files:**
- Modify: `frontend/packages/gateway-admin/src/api/routeGroup.ts`

- [ ] **Step 1: 更新 RouteGroup 接口**

修改 `RouteGroup` 接口，添加 `storageMode` 字段：

```typescript
/**
 * 路由分组
 */
export interface RouteGroup {
  groupId?: number
  groupKey: string
  groupName: string
  storageMode: string
  instanceCount?: number
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}
```

- [ ] **Step 2: 更新 AddRouteGroupParams 接口**

修改 `AddRouteGroupParams` 接口：

```typescript
/**
 * 新增分组请求参数
 */
export interface AddRouteGroupParams {
  groupKey: string
  groupName: string
  storageMode: string
  remark?: string
}
```

---

## Task 7: 前端国际化

**Files:**
- Modify: `frontend/packages/gateway-admin/src/locales/zh-cn.ts`
- Modify: `frontend/packages/gateway-admin/src/locales/en-us.ts`

- [ ] **Step 1: 添加中文翻译**

在 `zh-cn.ts` 的 `routeGroup` 对象中添加：

```typescript
routeGroup: {
  title: '路由分组',
  groupKey: '分组标识',
  groupName: '分组名称',
  storageMode: '存储方式',
  instanceCount: '实例数量',
  groupKeyPlaceholder: '请输入分组标识',
  groupNamePlaceholder: '请输入分组名称',
  storageModePlaceholder: '请选择存储方式',
  remarkPlaceholder: '请输入备注说明',
  addGroup: '新增分组',
  editGroup: '编辑分组',
  deleteConfirm: '确定要删除该分组吗？删除后不可恢复',
  groupKeyRequired: '分组标识不能为空',
  groupNameRequired: '分组名称不能为空',
  storageModeRequired: '存储方式不能为空',
  groupKeyExists: '分组标识已存在',
  hasInstances: '该分组下存在实例，无法删除',
  defaultGroupCannotDisable: '默认分组不可禁用',
  nacos: 'Nacos',
  redis: 'Redis',
  nacosDesc: '配置中心管理，支持版本历史，适合配置审计场景',
  redisDesc: '高性能存储，支持实时同步，适合高并发场景',
},
```

- [ ] **Step 2: 添加英文翻译**

在 `en-us.ts` 中添加相应的英文翻译：

```typescript
routeGroup: {
  title: 'Route Group',
  groupKey: 'Group Key',
  groupName: 'Group Name',
  storageMode: 'Storage Mode',
  instanceCount: 'Instance Count',
  groupKeyPlaceholder: 'Please enter group key',
  groupNamePlaceholder: 'Please enter group name',
  storageModePlaceholder: 'Please select storage mode',
  remarkPlaceholder: 'Please enter remark',
  addGroup: 'Add Group',
  editGroup: 'Edit Group',
  deleteConfirm: 'Are you sure to delete this group? This action cannot be undone',
  groupKeyRequired: 'Group key is required',
  groupNameRequired: 'Group name is required',
  storageModeRequired: 'Storage mode is required',
  groupKeyExists: 'Group key already exists',
  hasInstances: 'Cannot delete group with instances',
  defaultGroupCannotDisable: 'Default group cannot be disabled',
  nacos: 'Nacos',
  redis: 'Redis',
  nacosDesc: 'Config center management, supports version history, suitable for audit scenarios',
  redisDesc: 'High-performance storage, supports real-time sync, suitable for high-concurrency scenarios',
},
```

---

## Task 8: 前端页面变更

**Files:**
- Modify: `frontend/packages/gateway-admin/src/views/routeGroup/index.vue`

- [ ] **Step 1: 表格添加存储方式列**

在 `<el-table>` 中，`groupName` 列后添加存储方式列：

```vue
<el-table-column :label="t('routeGroup.storageMode')" width="100" align="center">
  <template #default="{ row }">
    <el-tag :type="row.storageMode === 'nacos' ? 'primary' : 'success'" effect="plain" size="small">
      {{ row.storageMode === 'nacos' ? t('routeGroup.nacos') : t('routeGroup.redis') }}
    </el-tag>
  </template>
</el-table-column>
```

- [ ] **Step 2: 表单添加存储方式选择**

在弹窗表单中，`groupName` 表单项后添加存储方式选择：

```vue
<el-form-item :label="t('routeGroup.storageMode')" prop="storageMode">
  <el-radio-group v-model="formData.storageMode">
    <el-radio value="nacos">
      <span>{{ t('routeGroup.nacos') }}</span>
      <el-tooltip :content="t('routeGroup.nacosDesc')" placement="top">
        <el-icon class="ml-1 cursor-pointer"><QuestionFilled /></el-icon>
      </el-tooltip>
    </el-radio>
    <el-radio value="redis">
      <span>{{ t('routeGroup.redis') }}</span>
      <el-tooltip :content="t('routeGroup.redisDesc')" placement="top">
        <el-icon class="ml-1 cursor-pointer"><QuestionFilled /></el-icon>
      </el-tooltip>
    </el-radio>
  </el-radio-group>
</el-form-item>
```

- [ ] **Step 3: 更新表单数据初始值**

修改 `formData` 响应式对象：

```typescript
const formData = reactive({
  groupKey: '',
  groupName: '',
  storageMode: 'nacos',
  remark: '',
})
```

- [ ] **Step 4: 更新表单校验规则**

在 `formRules` 中添加 `storageMode` 校验：

```typescript
const formRules = computed<FormRules>(() => ({
  groupKey: [
    { required: true, message: t('routeGroup.groupKeyRequired'), trigger: 'blur' },
    { min: 1, max: 50, message: t('validation.length', { min: 1, max: 50 }), trigger: 'blur' },
  ],
  groupName: [
    { required: true, message: t('routeGroup.groupNameRequired'), trigger: 'blur' },
    { min: 1, max: 100, message: t('validation.length', { min: 1, max: 100 }), trigger: 'blur' },
  ],
  storageMode: [
    { required: true, message: t('routeGroup.storageModeRequired'), trigger: 'change' },
  ],
}))
```

- [ ] **Step 5: 更新 handleAdd 方法**

```typescript
const handleAdd = () => {
  formData.groupKey = ''
  formData.groupName = ''
  formData.storageMode = 'nacos'
  formData.remark = ''
  dialogVisible.value = true
}
```

- [ ] **Step 6: 更新 handleSubmit 方法**

```typescript
const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await addRouteGroup({
      groupKey: formData.groupKey,
      groupName: formData.groupName,
      storageMode: formData.storageMode,
      remark: formData.remark,
    })
    ElMessage.success(t('common.success'))
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('Submit error:', error)
  } finally {
    submitting.value = false
  }
}
```

- [ ] **Step 7: 添加图标导入**

在 script 部分添加 `QuestionFilled` 图标导入：

```typescript
import {
  Search,
  RefreshLeft,
  Plus,
  Delete,
  QuestionFilled,
} from '@element-plus/icons-vue'
```

- [ ] **Step 8: 添加样式**

在 `<style>` 部分添加：

```scss
.group-form {
  .el-radio-group {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .el-radio {
    height: auto;
    align-items: flex-start;
  }

  .ml-1 {
    margin-left: 4px;
  }

  .cursor-pointer {
    cursor: pointer;
  }
}
```

---

## Task 9: 提交代码

- [ ] **Step 1: 提交后端变更**

```bash
git add blink-gateway/sql/gateway_admin_init.sql
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/RouteConstant.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/constants/ErrCodeConstant.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/entity/GatewayRouteGroupDO.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/req/AddRouteGroupReq.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/dto/vo/RouteGroupVO.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/component/NacosConfigComponent.java
git add blink-gateway/gateway-admin/src/main/java/com/blink/gateway/admin/service/impl/GatewayRouteGroupServiceImpl.java
git commit -m "$(cat <<'EOF'
feat(gateway-admin): 路由分组新增存储方式字段

- 数据库 gateway_route_group 表添加 storage_mode 字段
- 新增分组时根据 storageMode 自动创建 Nacos 配置或 Redis 缓存
- 删除分组时自动清理对应的配置文件或缓存
- 存储方式支持 nacos 和 redis 两种选项

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 2: 提交前端变更**

```bash
git add frontend/packages/gateway-admin/src/api/routeGroup.ts
git add frontend/packages/gateway-admin/src/locales/zh-cn.ts
git add frontend/packages/gateway-admin/src/locales/en-us.ts
git add frontend/packages/gateway-admin/src/views/routeGroup/index.vue
git commit -m "$(cat <<'EOF'
feat(gateway-admin): 前端路由分组页面支持存储方式选择

- 新增分组弹窗添加存储方式单选框（Nacos/Redis）
- 分组列表显示存储方式列
- 添加存储方式相关国际化文本

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

---

## 自检清单

**1. Spec 覆盖检查：**
- [x] 数据库 storage_mode 字段 → Task 1
- [x] 实体类/DTO 变更 → Task 3
- [x] 常量定义 → Task 2
- [x] Nacos 配置删除方法 → Task 4
- [x] Service 新增/删除逻辑 → Task 5
- [x] 前端 API 类型 → Task 6
- [x] 前端国际化 → Task 7
- [x] 前端页面变更 → Task 8

**2. 占位符检查：**
- 无 TBD、TODO 等占位符
- 所有代码步骤都有完整实现

**3. 类型一致性检查：**
- `storageMode` 在所有文件中类型一致（String/string）
- 常量名使用一致（`STORAGE_MODE_NACOS`、`STORAGE_MODE_REDIS`）
- Redis Key 格式与设计文档一致
