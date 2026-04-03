# 关联过滤规则设计文档

## 1. 概述

### 1.1 背景
当前数据权限系统支持以下过滤类型：
- `FIELD_FILTER` - 字段过滤（排除/包含字段）
- `CREATOR_FILTER` - 用户过滤（匹配 create_by、user_id 等）
- `DATE_RANGE_FILTER` - 时间范围过滤
- `CUSTOM_SQL` - 自定义SQL片段

**问题**：以上类型都只能处理主表字段过滤，无法处理通过**关联表**建立关系的场景。

### 1.2 典型场景

**场景1：用户-部门关联表过滤**
```
sys_user (主表)
sys_user_group_rela (关联表: user_id, group_id)
sys_group (部门表)

需求：用户只能看到自己部门的数据
问题：sys_user 表没有 group_id 字段，关系通过关联表建立
```

**场景2：用户-角色关联表过滤**
```
sys_role_perm_rela (关联表: role_id, ac_id)
sys_permission (权限表, ac_id)
sys_data_filter (数据过滤规则, data_filter_id)

需求：用户只能看到自己角色拥有权限的数据过滤规则
问题：权限关系通过角色-权限关联表建立
```

### 1.3 设计原则

**关联关系预定义**：
- 关联关系通过 `@DataScopeRelation` 注解在**关联表DO**上声明
- 扫描器构建以主表名为Key的关联关系缓存
- `/getEntityList` 接口返回实体时带上可用的关联关系
- 前端根据关联关系动态展示可用选项

## 2. 关联关系注解设计

### 2.1 设计思路

**双向对称设计**：
- 关联表连接两个实体表，关系是**双向对等**的
- 扫描时从每个实体表出发，构建该实体可用的关联关系
- `sys_user_role_rela` 示例：
  - 对于 `sys_user` 实体：可选择"角色关联"，按角色过滤用户
  - 对于 `sys_role` 实体：可选择"用户关联"，按用户过滤角色

### 2.2 注解定义

```java
package com.blink.datasource.annotation;

import java.lang.annotation.*;

/**
 * 数据范围关联关系注解
 * 用于标记关联表DO，声明该关联表连接的两个实体表
 *
 * 示例：sys_user_role_rela 连接 sys_user 和 sys_role
 * - sys_user 实体可以选择"角色关联"来过滤
 * - sys_role 实体可以选择"用户关联"来过滤
 *
 * @author binblink
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScopeRelation {

    /**
     * 关联表的一端
     */
    RelationEndpoint endpointA();

    /**
     * 关联表的另一端
     */
    RelationEndpoint endpointB();

    /**
     * 支持的匹配类型（两端通用）
     * CURRENT_USER, CURRENT_DEPT, USER_LIST, DEPT_LIST, ROLE_LIST
     */
    String[] supportMatchTypes() default {"CURRENT_USER", "CURRENT_DEPT", "USER_LIST", "DEPT_LIST", "ROLE_LIST"};
}
```

### 2.3 端点注解

```java
package com.blink.datasource.annotation;

import java.lang.annotation.*;

/**
 * 关联关系端点注解
 * 定义关联表的一个端点实体
 *
 * @author binblink
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface RelationEndpoint {

    /**
     * 端点名称（中文，如"用户"、"角色"）
     */
    String name();

    /**
     * 端点英文名称（可选）
     */
    String enName() default "";

    /**
     * 实体表名（如 sys_user）
     */
    String table();

    /**
     * 实体表关联字段（如 user_id）
     */
    String field();

    /**
     * 关联表中的关联字段（如 user_id）
     */
    String relationField();
}
```

### 2.4 关联表DO使用示例

**示例1：用户-角色关联表**

```java
@Getter
@Setter
@TableName("sys_user_role_rela")
@DataScopeRelation(
    endpointA = @RelationEndpoint(
        name = "用户",
        enName = "User",
        table = "sys_user",
        field = "user_id",
        relationField = "user_id"
    ),
    endpointB = @RelationEndpoint(
        name = "角色",
        enName = "Role",
        table = "sys_role",
        field = "role_id",
        relationField = "role_id"
    ),
    supportMatchTypes = {"CURRENT_USER", "USER_LIST", "ROLE_LIST"}
)
public class SysUserRoleRelaDO implements Serializable {
    @TableId("user_id")
    private Integer userId;

    @TableField("role_id")
    private Integer roleId;
}
```

**示例2：用户-部门关联表**

```java
@Getter
@Setter
@TableName("sys_user_group_rela")
@DataScopeRelation(
    endpointA = @RelationEndpoint(
        name = "用户",
        enName = "User",
        table = "sys_user",
        field = "user_id",
        relationField = "user_id"
    ),
    endpointB = @RelationEndpoint(
        name = "部门",
        enName = "Dept",
        table = "sys_group",
        field = "group_id",
        relationField = "group_id"
    ),
    supportMatchTypes = {"CURRENT_DEPT", "DEPT_LIST", "CURRENT_USER", "USER_LIST"}
)
public class SysUserGroupRelaDO implements Serializable {
    @TableId("user_id")
    private Integer userId;

    @TableField("group_id")
    private Integer groupId;
}
```

**示例3：角色-权限关联表**

```java
@Getter
@Setter
@TableName("sys_role_perm_rela")
@DataScopeRelation(
    endpointA = @RelationEndpoint(
        name = "角色",
        enName = "Role",
        table = "sys_role",
        field = "role_id",
        relationField = "role_id"
    ),
    endpointB = @RelationEndpoint(
        name = "权限",
        enName = "Permission",
        table = "sys_permission",
        field = "ac_id",
        relationField = "ac_id"
    ),
    supportMatchTypes = {"ROLE_LIST", "CURRENT_USER"}
)
public class SysRolePermRelaDO implements Serializable {
    @TableId("role_id")
    private Integer roleId;

    @TableField("ac_id")
    private Integer acId;
}
```

## 3. 缓存结构设计

### 3.1 RelationInfo VO类

```java
package com.blink.datasource.data;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 关联关系信息VO
 * 表示从某个实体出发可用的关联关系
 *
 * @author binblink
 */
@Data
public class RelationInfoVO implements Serializable {

    /**
     * 关联关系名称（如"角色关联"、"用户关联"）
     */
    private String name;

    /**
     * 关联关系英文名称
     */
    private String enName;

    /**
     * 关联表名
     */
    private String relationTable;

    /**
     * 当前实体的关联字段（如 sys_user.user_id）
     */
    private String sourceField;

    /**
     * 关联表中关联当前实体的字段
     */
    private String relationSourceField;

    /**
     * 关联表中关联目标实体的字段
     */
    private String relationTargetField;

    /**
     * 目标实体表名（用于前端显示匹配值选择器类型）
     */
    private String targetTable;

    /**
     * 目标实体字段名
     */
    private String targetField;

    /**
     * 目标实体名称（如"角色"、"部门"）
     */
    private String targetName;

    /**
     * 支持的匹配类型
     */
    private List<String> supportMatchTypes;
}
```

### 3.2 RegisteredEntityVO 扩展

```java
package com.blink.datasource.data;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 已注册实体信息VO
 *
 * @author binblink
 */
@Data
public class RegisteredEntityVO implements Serializable {

    /**
     * 实体类全限定名
     */
    private String entityClass;

    /**
     * 实体类中文名称
     */
    private String entityName;

    /**
     * 实体类英文名称
     */
    private String entityEnName;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 该实体可用的关联关系列表
     * 用于关联过滤类型配置
     */
    private List<RelationInfoVO> relations;
}
```

### 3.3 扫描器缓存构建逻辑

**双向构建**：

```java
// DataScopeEntityScanner 扩展

/**
 * 实体表 -> 关联关系列表 映射
 * Key: 实体表名 (如 sys_user)
 * Value: 该实体可用的关联关系列表
 */
private static final Map<String, List<RelationInfoVO>> TABLE_RELATIONS_MAP = new ConcurrentHashMap<>();

/**
 * 扫描关联关系注解，构建双向缓存
 */
private void scanRelationAnnotations() {
    // 扫描带有 @DataScopeRelation 注解的类
    Set<Class<?>> relationClasses = ClassUtil.scanPackageByAnnotation(
        "com.blink", DataScopeRelation.class
    );

    for (Class<?> clazz : relationClasses) {
        DataScopeRelation relation = clazz.getAnnotation(DataScopeRelation.class);
        TableName tableNameAnnotation = clazz.getAnnotation(TableName.class);

        if (relation == null || tableNameAnnotation == null) {
            continue;
        }

        String relationTable = tableNameAnnotation.value();
        RelationEndpoint endpointA = relation.endpointA();
        RelationEndpoint endpointB = relation.endpointB();
        String[] supportMatchTypes = relation.supportMatchTypes();

        // 为端点A构建关联关系（A视角：关联到B）
        RelationInfoVO relationForA = buildRelationInfo(
            endpointB.name(),           // 关联关系名称：B的名称
            endpointB.enName(),
            relationTable,
            endpointA.field(),          // A的字段
            endpointA.relationField(),  // 关联表关联A的字段
            endpointB.relationField(),  // 关联表关联B的字段
            endpointB.table(),          // 目标表：B
            endpointB.field(),
            supportMatchTypes
        );
        addToRelationMap(endpointA.table(), relationForA);

        // 为端点B构建关联关系（B视角：关联到A）
        RelationInfoVO relationForB = buildRelationInfo(
            endpointA.name(),           // 关联关系名称：A的名称
            endpointA.enName(),
            relationTable,
            endpointB.field(),          // B的字段
            endpointB.relationField(),  // 关联表关联B的字段
            endpointA.relationField(),  // 关联表关联A的字段
            endpointA.table(),          // 目标表：A
            endpointA.field(),
            supportMatchTypes
        );
        addToRelationMap(endpointB.table(), relationForB);

        log.debug("注册关联关系: {} <-> {} (表: {})",
            endpointA.table(), endpointB.table(), relationTable);
    }
}

/**
 * 构建关联关系VO
 */
private RelationInfoVO buildRelationInfo(
        String targetName, String targetEnName,
        String relationTable,
        String sourceField, String relationSourceField, String relationTargetField,
        String targetTable, String targetField,
        String[] supportMatchTypes) {

    RelationInfoVO vo = new RelationInfoVO();
    vo.setName(targetName + "关联");  // 如 "角色关联"
    vo.setEnName(targetEnName + "Relation");
    vo.setRelationTable(relationTable);
    vo.setSourceField(sourceField);
    vo.setRelationSourceField(relationSourceField);
    vo.setRelationTargetField(relationTargetField);
    vo.setTargetTable(targetTable);
    vo.setTargetField(targetField);
    vo.setTargetName(targetName);
    vo.setSupportMatchTypes(Arrays.asList(supportMatchTypes));
    return vo;
}

/**
 * 添加到关联关系缓存
 */
private void addToRelationMap(String table, RelationInfoVO relation) {
    TABLE_RELATIONS_MAP.computeIfAbsent(table, k -> new ArrayList<>()).add(relation);
}
```

### 3.4 缓存内容示例

扫描 `SysUserRoleRelaDO` 后生成的缓存：

```json
{
  "sys_user": [
    {
      "name": "角色关联",
      "enName": "RoleRelation",
      "relationTable": "sys_user_role_rela",
      "sourceField": "user_id",
      "relationSourceField": "user_id",
      "relationTargetField": "role_id",
      "targetTable": "sys_role",
      "targetField": "role_id",
      "targetName": "角色",
      "supportMatchTypes": ["CURRENT_USER", "USER_LIST", "ROLE_LIST"]
    }
  ],
  "sys_role": [
    {
      "name": "用户关联",
      "enName": "UserRelation",
      "relationTable": "sys_user_role_rela",
      "sourceField": "role_id",
      "relationSourceField": "role_id",
      "relationTargetField": "user_id",
      "targetTable": "sys_user",
      "targetField": "user_id",
      "targetName": "用户",
      "supportMatchTypes": ["CURRENT_USER", "USER_LIST", "ROLE_LIST"]
    }
  ]
}
```

扫描 `SysUserGroupRelaDO` 后，缓存更新为：

```json
{
  "sys_user": [
    {
      "name": "角色关联",
      "relationTable": "sys_user_role_rela",
      "sourceField": "user_id",
      "relationSourceField": "user_id",
      "relationTargetField": "role_id",
      "targetTable": "sys_role",
      "targetName": "角色",
      ...
    },
    {
      "name": "部门关联",
      "relationTable": "sys_user_group_rela",
      "sourceField": "user_id",
      "relationSourceField": "user_id",
      "relationTargetField": "group_id",
      "targetTable": "sys_group",
      "targetName": "部门",
      ...
    }
  ],
  "sys_role": [
    {
      "name": "用户关联",
      "relationTable": "sys_user_role_rela",
      ...
    }
  ],
  "sys_group": [
    {
      "name": "用户关联",
      "relationTable": "sys_user_group_rela",
      "sourceField": "group_id",
      "relationSourceField": "group_id",
      "relationTargetField": "user_id",
      "targetTable": "sys_user",
      "targetName": "用户",
      ...
    }
  ]
}
```

## 4. 接口设计

### 4.1 /getEntityList 接口响应

**请求**：
```
GET /system/dataFilter/getEntityList
```

**响应**：
```json
{
  "code": "00000",
  "msg": "success",
  "body": [
    {
      "entityClass": "com.blink.entity.com.blink.base.SysUserDO",
      "entityName": "用户",
      "entityEnName": "SysUser",
      "tableName": "sys_user",
      "relations": [
        {
          "name": "部门关联",
          "enName": "DeptRelation",
          "relationTable": "sys_user_group_rela",
          "sourceField": "user_id",
          "relationSourceField": "user_id",
          "relationTargetField": "group_id",
          "targetTable": "sys_group",
          "targetField": "group_id",
          "targetName": "部门",
          "supportMatchTypes": ["CURRENT_DEPT", "DEPT_LIST"]
        },
        {
          "name": "角色关联",
          "enName": "RoleRelation",
          "relationTable": "sys_user_role_rela",
          "sourceField": "user_id",
          "relationSourceField": "user_id",
          "relationTargetField": "role_id",
          "targetTable": "sys_role",
          "targetField": "role_id",
          "targetName": "角色",
          "supportMatchTypes": ["ROLE_LIST", "CURRENT_USER"]
        }
      ]
    },
    {
      "entityClass": "com.blink.entity.com.blink.base.SysRoleDO",
      "entityName": "角色",
      "entityEnName": "SysRole",
      "tableName": "sys_role",
      "relations": [
        {
          "name": "用户关联",
          "enName": "UserRelation",
          "relationTable": "sys_user_role_rela",
          "sourceField": "role_id",
          "relationSourceField": "role_id",
          "relationTargetField": "user_id",
          "targetTable": "sys_user",
          "targetField": "user_id",
          "targetName": "用户",
          "supportMatchTypes": ["ROLE_LIST", "CURRENT_USER"]
        }
      ]
    },
    {
      "entityClass": "com.blink.entity.com.blink.base.SysConfigDO",
      "entityName": "系统配置",
      "entityEnName": "SysConfig",
      "tableName": "sys_config",
      "relations": []
    }
  ]
}
```

### 4.2 前端判断逻辑

```typescript
// 判断实体是否支持关联过滤
const hasRelations = (entity: RegisteredEntityVO): boolean => {
  return entity.relations && entity.relations.length > 0
}

// 规则类型选项
const ruleTypeOptions = computed(() => {
  const types = [
    { value: 'FIELD_FILTER', label: t('dataScope.fieldFilter') },
    { value: 'CREATOR_FILTER', label: t('dataScope.creatorFilter') },
    { value: 'DATE_RANGE_FILTER', label: t('dataScope.dateRangeFilter') },
    { value: 'CUSTOM_SQL', label: t('dataScope.customSql') },
    { value: 'RELATION_FILTER', label: t('dataScope.relationFilter') }
  ]

  // 如果当前实体没有关联关系，禁用关联过滤选项
  if (selectedEntity.value && !hasRelations(selectedEntity.value)) {
    types[4].disabled = true
    types[4].label += ` (${t('dataScope.noRelationSupport')})`
  }

  return types
})
```

## 5. RuleConfig 字段设计

### 5.1 新增字段

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `relationTable` | String | 关联表名 | `sys_user_group_rela` |
| `sourceField` | String | 主表关联字段 | `user_id` |
| `relationSourceField` | String | 关联表源字段 | `user_id` |
| `relationTargetField` | String | 关联表目标字段 | `group_id` |
| `relationMatchType` | String | 匹配类型 | `CURRENT_DEPT` |
| `relationMatchValues` | List<Integer> | 匹配值列表 | `[1, 2, 3]` |

### 5.2 配置示例

```json
{
  "ruleType": "RELATION_FILTER",
  "entityClass": "com.blink.entity.com.blink.base.SysUserDO",
  "relationTable": "sys_user_group_rela",
  "sourceField": "user_id",
  "relationSourceField": "user_id",
  "relationTargetField": "group_id",
  "relationMatchType": "CURRENT_DEPT"
}
```

## 6. SQL生成逻辑

### 6.1 EXISTS子查询生成

**输入配置**：
```json
{
  "relationTable": "sys_user_group_rela",
  "sourceField": "user_id",
  "relationSourceField": "user_id",
  "relationTargetField": "group_id",
  "relationMatchType": "CURRENT_DEPT"
}
```

**生成SQL**：
```sql
SELECT * FROM sys_user t
WHERE EXISTS (
    SELECT 1 FROM sys_user_group_rela r
    WHERE r.user_id = t.user_id
    AND r.group_id = #{currentDeptId}
)
```

### 6.2 RelationFilterHandler 实现

```java
@Slf4j
public class RelationFilterHandler implements RuleHandler {

    @Override
    public void apply(StringBuilder sql, RuleConfig config, DataScopeParseResult context) {
        if (!validateConfig(config)) {
            return;
        }

        String existsCondition = buildExistsCondition(config, context);
        if (StrUtil.isNotBlank(existsCondition)) {
            DataScopeSqlUtil.appendWhereCondition(sql, existsCondition);
        }
    }

    private boolean validateConfig(RuleConfig config) {
        return StrUtil.isNotBlank(config.getRelationTable())
            && StrUtil.isNotBlank(config.getSourceField())
            && StrUtil.isNotBlank(config.getRelationSourceField())
            && StrUtil.isNotBlank(config.getRelationTargetField())
            && StrUtil.isNotBlank(config.getRelationMatchType());
    }

    private String buildExistsCondition(RuleConfig config, DataScopeParseResult context) {
        String tableAlias = StrUtil.isNotBlank(context.getTableAlias())
            ? context.getTableAlias()
            : "t";

        String matchCondition = buildMatchCondition(config, context);
        if (StrUtil.isBlank(matchCondition)) {
            return null;
        }

        return String.format(
            "EXISTS (SELECT 1 FROM %s r WHERE r.%s = %s.%s AND %s)",
            config.getRelationTable(),
            config.getRelationSourceField(),
            tableAlias,
            config.getSourceField(),
            matchCondition
        );
    }

    private String buildMatchCondition(RuleConfig config, DataScopeParseResult context) {
        return switch (config.getRelationMatchType()) {
            case "CURRENT_USER" -> {
                Integer userId = context.getUserInfo().getUserId();
                yield userId != null ? "r." + config.getRelationTargetField() + " = " + userId : null;
            }
            case "CURRENT_DEPT" -> {
                Integer deptId = context.getUserInfo().getDeptId();
                yield deptId != null ? "r." + config.getRelationTargetField() + " = " + deptId : null;
            }
            case "USER_LIST", "DEPT_LIST", "ROLE_LIST" -> {
                if (CollUtil.isEmpty(config.getRelationMatchValues())) {
                    yield null;
                }
                yield "r." + config.getRelationTargetField() + " IN " + buildInClause(config.getRelationMatchValues());
            }
            default -> null;
        };
    }

    @Override
    public String getRuleType() {
        return DataScopeRuleType.RELATION_FILTER.name();
    }
}
```

## 7. 前端配置组件设计

### 7.1 RelationFilterConfig.vue

```vue
<template>
  <div class="relation-filter-config">
    <!-- 无关联关系提示 -->
    <el-alert
      v-if="!hasRelations"
      type="warning"
      :closable="false"
      show-icon
    >
      {{ t('dataScope.noRelationSupport') }}
    </el-alert>

    <!-- 关联关系选择 -->
    <el-form-item v-else :label="t('dataScope.relationName')">
      <el-select v-model="selectedRelationIndex" @change="handleRelationChange">
        <el-option
          v-for="(relation, index) in relations"
          :key="index"
          :label="relation.name"
          :value="index"
        />
      </el-select>
    </el-form-item>

    <!-- 匹配类型选择 -->
    <el-form-item v-if="selectedRelation" :label="t('dataScope.matchType')">
      <el-select v-model="config.relationMatchType">
        <el-option
          v-for="type in availableMatchTypes"
          :key="type.value"
          :label="type.label"
          :value="type.value"
        />
      </el-select>
    </el-form-item>

    <!-- 匹配值选择 -->
    <el-form-item v-if="showMatchValues" :label="t('dataScope.selectMatchValues')">
      <MatchValueSelector
        :match-type="config.relationMatchType"
        :target-table="selectedRelation?.targetTable"
        v-model="config.relationMatchValues"
      />
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  entityInfo: Object,  // RegisteredEntityVO，包含 relations
  modelValue: Object
})

const emit = defineEmits(['update:modelValue'])
const { t } = useI18n()

const selectedRelationIndex = ref(0)

const relations = computed(() => props.entityInfo?.relations || [])
const hasRelations = computed(() => relations.value.length > 0)
const selectedRelation = computed(() => relations.value[selectedRelationIndex.value])

const config = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 可用的匹配类型
const availableMatchTypes = computed(() => {
  if (!selectedRelation.value) return []
  const typeMap = {
    'CURRENT_USER': t('dataScope.MATCH_CURRENT_USER'),
    'CURRENT_DEPT': t('dataScope.MATCH_CURRENT_DEPT'),
    'USER_LIST': t('dataScope.MATCH_USER_LIST'),
    'DEPT_LIST': t('dataScope.MATCH_DEPT_LIST'),
    'ROLE_LIST': t('dataScope.MATCH_ROLE_LIST')
  }
  return selectedRelation.value.supportMatchTypes.map(type => ({
    value: type,
    label: typeMap[type] || type
  }))
})

// 是否显示匹配值选择器
const showMatchValues = computed(() => {
  return ['USER_LIST', 'DEPT_LIST', 'ROLE_LIST'].includes(config.value?.relationMatchType)
})

// 关联关系变更
const handleRelationChange = () => {
  if (selectedRelation.value) {
    config.value = {
      ...config.value,
      relationTable: selectedRelation.value.relationTable,
      sourceField: selectedRelation.value.sourceField,
      relationSourceField: selectedRelation.value.relationSourceField,
      relationTargetField: selectedRelation.value.relationTargetField,
      relationMatchType: selectedRelation.value.supportMatchTypes[0],
      relationMatchValues: []
    }
  }
}

// 初始化
watch(() => props.entityInfo, () => {
  if (hasRelations.value) {
    handleRelationChange()
  }
}, { immediate: true })
</script>
```

## 8. 匹配类型说明

| 类型 | 说明 | 使用场景 |
|------|------|----------|
| `CURRENT_USER` | 当前用户ID | 用户只能看到自己相关的数据 |
| `CURRENT_DEPT` | 当前用户部门ID | 部门数据隔离 |
| `USER_LIST` | 指定用户ID列表 | 按用户过滤 |
| `DEPT_LIST` | 指定部门ID列表 | 按部门过滤 |
| `ROLE_LIST` | 指定角色ID列表 | 按角色过滤 |

## 9. 实现清单

| 层级 | 文件 | 说明 |
|------|------|------|
| 注解 | `DataScopeRelation.java` | 关联关系注解 |
| 注解 | `RelationEndpoint.java` | 端点注解 |
| VO | `RelationInfoVO.java` | 关联关系信息VO |
| VO | `RegisteredEntityVO.java` | 扩展relations字段 |
| 扫描器 | `DataScopeEntityScanner.java` | 扫描关联关系，构建缓存 |
| 处理器 | `RelationFilterHandler.java` | 关联过滤处理器 |
| 配置 | `RuleConfig.java` | 新增关联过滤字段 |
| 枚举 | `DataScopeRuleType.java` | 新增 RELATION_FILTER |
| 前端 | `RelationFilterConfig.vue` | 配置组件 |
| 国际化 | `zh-cn.ts` / `en-us.ts` | 新增文案 |

## 10. 总结

### 10.1 核心设计要点

1. **双向对称设计**：关联表注解声明两个端点，扫描时为每个端点构建关联关系
2. **关联关系定义在关联表DO上**：通过 `@DataScopeRelation` 注解声明两个端点
3. **缓存结构**：以实体表名为Key，存储该实体可用的关联关系列表
4. **接口整合**：`/getEntityList` 返回实体时带上关联关系
5. **前端联动**：根据实体是否有关联关系动态展示/禁用选项
6. **EXISTS子查询**：实现高效的关联数据过滤

### 10.2 双向关系示例

| 关联表 | sys_user 视角 | sys_role 视角 |
|--------|--------------|---------------|
| `sys_user_role_rela` | "角色关联"：按角色过滤用户 | "用户关联"：按用户过滤角色 |
| `sys_user_group_rela` | "部门关联"：按部门过滤用户 | "用户关联"：按用户过滤部门 |

### 10.3 优势

- **双向可用**：一个关联表注解，两端实体都可使用
- **类型安全**：只有定义了关联关系的实体才能使用关联过滤
- **配置简单**：前端选择关联关系后自动填充字段配置
- **易于扩展**：新增关联关系只需在关联表DO上添加注解
- **性能优化**：EXISTS子查询在有索引时性能优异