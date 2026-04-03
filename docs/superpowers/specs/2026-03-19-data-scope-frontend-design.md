# 数据权限前端页面设计文档

## 一、需求概述

基于后端已实现的数据权限管理API，完善前端数据权限管理页面，支持数据过滤规则的增删改查及规则配置的可视化编辑。

### 核心需求

1. **表单驱动配置**：规则配置使用动态表单，根据规则类型显示不同的配置字段
2. **实体类下拉选择**：从已注册实体列表选择，无需手动输入完整类路径
3. **单页表单**：所有配置在一个页面中，规则配置区域动态变化
4. **穿梭框选择字段**：字段过滤规则使用穿梭框进行字段选择
anz
## 二、技术方案

### 2.1 目录结构

```
views/system/dataScope/
├── index.vue                      # 主页面（列表+搜索+分页）
└── components/
    ├── DataFilterFormDialog.vue   # 新增/编辑弹窗
    ├── FieldFilterConfig.vue      # 字段过滤配置
    ├── CreatorFilterConfig.vue    # 创建人过滤配置
    ├── DeptFilterConfig.vue       # 部门过滤配置
    ├── DateRangeConfig.vue        # 时间范围配置
    ├── StatusFilterConfig.vue     # 状态过滤配置
    └── CustomSqlConfig.vue        # 自定义SQL配置
```

### 2.2 API层设计

```typescript
// api/dataScope.ts

// ==================== 数据类型定义 ====================

/**
 * 数据过滤规则信息
 * 字段名与后端 DataFilterVO 保持一致
 */
interface DataFilterInfo {
  dataFilterId: number
  dataFilterName: string
  dataFilterEnName: string
  entityClass: string
  tableName: string
  ruleType: string          // 规则类型枚举值: FIELD_FILTER, CREATOR_FILTER 等
  ruleConfig: string        // JSON字符串，根据ruleType解析为对应配置对象
  status: number            // 状态: 0=启用, 1=禁用
  remark: string
  createBy: string
  createTime: string
}

/**
 * 已注册实体信息（需后端新增接口返回）
 */
interface EntityInfo {
  entityClass: string       // 完整类路径，如 com.blink.entity.com.blink.base.SysUserDO
  entityName: string        // 简短类名，如 SysUserDO
  tableName: string         // 关联表名，如 sys_user
}

/**
 * 实体字段信息（与后端 EntityFieldVO 一致）
 */
interface EntityFieldVO {
  fieldName: string         // Java属性名，如 userName
  columnName: string        // 数据库列名，如 user_name
  fieldType: string         // 字段类型，如 String, Integer
}

/**
 * 实体字段响应（与后端 EntityFieldsRsp 一致）
 */
interface EntityFieldsRsp {
  fields: EntityFieldVO[]
}

/**
 * 查询参数
 */
interface QueryDataFilterParams {
  pageNum?: number
  pageSize?: number
  dataFilterName?: string
  entityClass?: string
  ruleType?: string
  status?: number
}

/**
 * 新增参数
 */
interface AddDataFilterParams {
  dataFilterName: string
  dataFilterEnName?: string
  entityClass: string
  tableName: string
  ruleType: string
  ruleConfig: string
  remark?: string
}

/**
 * 更新参数（注意: entityClass 和 tableName 不可修改）
 */
interface UpdateDataFilterParams {
  dataFilterId: number
  dataFilterName: string
  dataFilterEnName?: string
  ruleConfig: string
  status?: number
  remark?: string
}

// ==================== API函数定义 ====================

/**
 * 分页查询数据过滤规则列表
 */
getDataFilterList(params: QueryDataFilterParams): Promise<PageResult<DataFilterInfo>>

/**
 * 新增数据过滤规则
 */
addDataFilter(params: AddDataFilterParams): Promise<void>

/**
 * 更新数据过滤规则
 */
updateDataFilter(params: UpdateDataFilterParams): Promise<void>

/**
 * 删除数据过滤规则
 * 注意: 参数需包裹在 body 中
 */
deleteDataFilter(dataFilterId: number): Promise<void>
// 实际调用: request.post('/sysDataFilter/deleteDataFilter', { body: { dataFilterId } })

/**
 * 获取规则详情
 * 注意: 返回分页结构，实际数据在 rows[0] 中
 */
getDataFilterDetail(dataFilterId: number): Promise<PageResult<DataFilterInfo>>
// 实际调用: const res = await getDataFilterDetail(id); const detail = res.rows[0]

/**
 * 获取已注册实体列表（需后端新增接口）
 */
getEntityList(): Promise<EntityInfo[]>

/**
 * 获取实体字段列表
 * 注意: 字段列表在 response.fields 中
 */
getEntityFields(entityClass: string): Promise<EntityFieldsRsp>
// 实际调用: const { fields } = await getEntityFields(entityClass)

/**
 * 刷新缓存
 */
refreshCache(): Promise<void>
```

## 三、页面设计

### 3.1 主页面 (index.vue)

**重要约定：**
- **状态值约定**: `status: 0 = 启用, 1 = 禁用`（与后端保持一致）
- **数据加载**: 使用 `useTransition` composable 实现平滑过渡，避免闪屏

**搜索区域：**

| 字段 | 组件 | 说明 |
|------|------|------|
| 规则名称 | Input | 模糊搜索，字段名 `dataFilterName` |
| 实体类 | Select | 下拉选择已注册实体 |
| 规则类型 | Select | 6种类型选择 |
| 状态 | Select | 启用/禁用 |

**表格列：**

| 列名 | 字段 | 说明 |
|------|------|------|
| ID | dataFilterId | - |
| 规则名称 | dataFilterName | - |
| 实体类 | entityClass | 显示简短类名，tooltip显示全路径 |
| 表名 | tableName | - |
| 规则类型 | ruleType | Tag显示，显示对应中文名称 |
| 状态 | status | Switch切换，0=启用(绿色), 1=禁用(灰色) |
| 创建时间 | createTime | - |
| 操作 | - | 编辑、删除按钮 |

**工具栏按钮：**
- 新增按钮（绿色）
- 刷新缓存按钮（灰色，放在新增按钮右侧）

### 3.2 表单弹窗 (DataFilterFormDialog.vue)

**弹窗配置：**
- 宽度: `800px`
- 位置: 居中显示

**布局结构：**

```
┌─────────────────────────────────────────┐
│ 新增/编辑数据权限规则                      │
├─────────────────────────────────────────┤
│ 基础信息                                  │
│ ┌─────────────────┬─────────────────┐   │
│ │ 规则名称 *       │ 规则英文名       │   │
│ │ [输入框]         │ [输入框]         │   │
│ ├─────────────────┼─────────────────┤   │
│ │ 实体类 *         │ 表名            │   │
│ │ [下拉选择]       │ [只读]          │   │
│ ├─────────────────┼─────────────────┤   │
│ │ 规则类型 *       │ 状态            │   │
│ │ [下拉选择]       │ [开关]          │   │
│ ├─────────────────┴─────────────────┤   │
│ │ 备注                               │   │
│ │ [文本域]                           │   │
│ └─────────────────────────────────────┘   │
│                                          │
│ 规则配置                                  │
│ ┌─────────────────────────────────────┐  │
│ │ [根据规则类型动态显示配置组件]         │  │
│ └─────────────────────────────────────┘  │
├─────────────────────────────────────────┤
│                    [取消] [确定]          │
└─────────────────────────────────────────┘
```

**交互逻辑：**

1. 选择实体类后，自动填充表名并加载字段列表
2. 选择规则类型后，显示对应的规则配置组件
3. 编辑模式下，加载已有数据并回填
4. **切换规则类型时，清空当前规则配置**

**编辑模式限制：**
- `entityClass`（实体类）和 `tableName`（表名）在编辑模式下**禁用/只读**，不可修改
- 后端 `UpdateDataFilterReq` 不包含这两个字段，修改无效
- 可编辑字段: `dataFilterName`, `dataFilterEnName`, `ruleConfig`, `status`, `remark`

## 四、规则配置组件设计

### 4.1 字段过滤配置 (FieldFilterConfig.vue)

**实现方式：** 使用 Element Plus `ElTransfer` 穿梭框组件

**ElTransfer 配置：**
```typescript
const transferProps = {
  key: 'fieldName',                        // 唯一标识字段
  label: 'columnName',                     // 显示标签
  titles: [t('dataScope.availableFields'), t('dataScope.selectedFields')],
  filterable: true,                        // 启用搜索
  filterPlaceholder: t('common.search')
}
```

**UI组件：**

```
┌─────────────────────────────────────────┐
│ 匹配模式: ○ 排除模式  ● 包含模式          │
├─────────────────────────────────────────┤
│ 可选字段          │    已选字段          │
│ ┌───────────────┐ │ ┌───────────────┐  │
│ │ □ userId      │ │ │ ■ userName    │  │
│ │ ■ userName    │ │ │ ■ nickName    │  │
│ │ ■ nickName    │ │ │               │  │
│ │ □ password    │ │ │               │  │
│ │ □ email       │ │ │               │  │
│ └───────────────┘ │ └───────────────┘  │
│    [添加 →]       │      [← 移除]       │
└─────────────────────────────────────────┘
```

**Props:**
- `modelValue: string` - ruleConfig JSON字符串
- `fields: EntityFieldVO[]` - 可选字段列表

**Emit:**
- `update:modelValue` - 更新配置

**生成JSON示例：**
```json
{
  "excludeFields": null,
  "includeFields": ["userName", "nickName"]
}
```

### 4.2 创建人过滤配置 (CreatorFilterConfig.vue)

**UI组件：**

```
┌─────────────────────────────────────────┐
│ 匹配字段: [create_by        ▼]          │
│ 匹配类型: [当前用户          ▼]          │
├─────────────────────────────────────────┤
│ (matchType=SPECIFIED_USER时显示)        │
│ 选择用户: [搜索选择用户...]              │
├─────────────────────────────────────────┤
│ (matchType=ROLE_USER时显示)             │
│ 选择角色: [下拉选择角色...]              │
└─────────────────────────────────────────┘
```

**匹配类型选项：**
- `CURRENT_USER` - 当前用户
- `SPECIFIED_USER` - 指定用户
- `ROLE_USER` - 指定角色的用户

**生成JSON示例：**
```json
{
  "field": "create_by",
  "matchType": "CURRENT_USER",
  "userIds": null,
  "roleIds": null
}
```

### 4.3 部门过滤配置 (DeptFilterConfig.vue)

**UI组件：**

```
┌─────────────────────────────────────────┐
│ 匹配字段: [group_id         ▼]          │
│ 匹配类型: [指定部门          ▼]          │
├─────────────────────────────────────────┤
│ (matchType=DEPT_LIST/DEPT_AND_CHILDREN) │
│ 选择部门:                                │
│ ┌───────────────────────────────────┐   │
│ │ □ 总公司                          │   │
│ │   ■ 研发部                        │   │
│ │   □ 产品部                        │   │
│ │ □ 分公司                          │   │
│ └───────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

**匹配类型选项：**
- `CURRENT_DEPT` - 当前部门
- `DEPT_LIST` - 指定部门
- `DEPT_AND_CHILDREN` - 部门及下级

**生成JSON示例：**
```json
{
  "field": "group_id",
  "matchType": "DEPT_LIST",
  "deptIds": [1, 2, 3]
}
```

### 4.4 时间范围配置 (DateRangeConfig.vue)

**UI组件：**

```
┌─────────────────────────────────────────┐
│ 匹配字段: [create_time      ▼]          │
│ 范围类型: ○ 相对时间  ● 绝对时间          │
├─────────────────────────────────────────┤
│ (rangeType=RELATIVE时显示)              │
│ 时间数值: [-7    ] 单位: [天 ▼]          │
├─────────────────────────────────────────┤
│ (rangeType=ABSOLUTE时显示)              │
│ 时间范围: [2024-01-01] 至 [2024-12-31]  │
└─────────────────────────────────────────┘
```

**范围类型：**
- `RELATIVE` - 相对时间
- `ABSOLUTE` - 绝对时间

**相对时间单位：**
- `DAY` - 天
- `WEEK` - 周
- `MONTH` - 月
- `YEAR` - 年

**生成JSON示例：**
```json
{
  "field": "create_time",
  "rangeType": "RELATIVE",
  "relativeValue": -7,
  "relativeUnit": "DAY",
  "startTime": null,
  "endTime": null
}
```

### 4.5 状态过滤配置 (StatusFilterConfig.vue)

**UI组件：**

```
┌─────────────────────────────────────────┐
│ 匹配字段: [status           ▼]          │
│ 允许值:                                  │
│ ┌─────────────────────────────────────┐ │
│ │ [启用 ×] [正常 ×] [+ 添加]           │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

**允许值配置：**
- 支持手动输入值
- 支持从字典选择（可选）
- 使用Tag显示已选值

**生成JSON示例：**
```json
{
  "field": "status",
  "allowedValues": [0, 1]
}
```

### 4.6 自定义SQL配置 (CustomSqlConfig.vue)

**UI组件：**

```
┌─────────────────────────────────────────┐
│ ⚠️ 安全提示: SQL片段将直接拼接到WHERE条件 │
├─────────────────────────────────────────┤
│ ┌─────────────────────────────────────┐ │
│ │ -- 常用模板 --                       │ │
│ │ [最近7天] [本月] [状态为启用]        │ │
│ └─────────────────────────────────────┘ │
│ ┌─────────────────────────────────────┐ │
│ │ status = 1                          │ │
│ │ AND create_time > DATE_SUB(         │ │
│ │   NOW(), INTERVAL 7 DAY             │ │
│ │ )                                   │ │
│ │                                     │ │
│ └─────────────────────────────────────┘ │
│                        [语法高亮编辑器]  │
└─────────────────────────────────────────┘
```

**安全校验：**
- 前端禁止输入：DELETE、DROP、TRUNCATE、ALTER、CREATE
- 后端进行完整SQL注入校验

**生成JSON示例：**
```json
{
  "sqlFragment": "status = 1 AND create_time > DATE_SUB(NOW(), INTERVAL 7 DAY)"
}
```

## 五、国际化设计

### 5.1 中文翻译 (zh-cn.ts)

```typescript
dataScope: {
  // 页面标题
  title: '数据权限',
  list: '数据权限列表',
  addTitle: '新增数据权限规则',
  editTitle: '编辑数据权限规则',

  // 基础字段
  filterName: '规则名称',
  filterEnName: '规则英文名',
  entityClass: '实体类',
  tableName: '表名',
  ruleType: '规则类型',
  ruleConfig: '规则配置',
  status: '状态',
  remark: '备注',

  // 规则类型
  fieldFilter: '字段过滤',
  creatorFilter: '创建人过滤',
  deptFilter: '部门过滤',
  dateRangeFilter: '时间范围过滤',
  statusFilter: '状态过滤',
  customSql: '自定义SQL',

  // 字段过滤配置
  excludeMode: '排除模式',
  includeMode: '包含模式',
  availableFields: '可选字段',
  selectedFields: '已选字段',
  noFieldsSelected: '请选择字段',

  // 创建人过滤配置
  matchField: '匹配字段',
  matchType: '匹配类型',
  currentUser: '当前用户',
  specifiedUser: '指定用户',
  roleUser: '指定角色的用户',
  selectUser: '选择用户',
  selectRole: '选择角色',

  // 部门过滤配置
  currentDept: '当前部门',
  specifiedDept: '指定部门',
  deptAndChildren: '部门及下级',
  selectDept: '选择部门',

  // 时间范围配置
  rangeType: '范围类型',
  relativeTime: '相对时间',
  absoluteTime: '绝对时间',
  timeValue: '时间数值',
  timeUnit: '单位',
  day: '天',
  week: '周',
  month: '月',
  year: '年',
  startTime: '开始时间',
  endTime: '结束时间',

  // 状态过滤配置
  allowedValues: '允许值',
  addValue: '添加值',
  inputPlaceholder: '输入值后回车',

  // 自定义SQL配置
  sqlFragment: 'SQL片段',
  securityWarning: 'SQL片段将直接拼接到WHERE条件，请确保安全性',
  commonTemplates: '常用模板',
  last7Days: '最近7天',
  thisMonth: '本月',
  statusEnabled: '状态为启用',

  // 操作
  deleteConfirm: '确定要删除该数据权限规则吗？',
  refreshCache: '刷新缓存',
  refreshCacheSuccess: '缓存刷新成功',

  // 验证
  nameRequired: '请输入规则名称',
  entityRequired: '请选择实体类',
  ruleTypeRequired: '请选择规则类型',
  ruleConfigRequired: '请配置规则',
}
```

### 5.2 英文翻译 (en-us.ts)

```typescript
dataScope: {
  title: 'Data Permission',
  list: 'Data Permission List',
  addTitle: 'Add Data Filter Rule',
  editTitle: 'Edit Data Filter Rule',

  filterName: 'Rule Name',
  filterEnName: 'Rule English Name',
  entityClass: 'Entity Class',
  tableName: 'Table Name',
  ruleType: 'Rule Type',
  ruleConfig: 'Rule Config',
  status: 'Status',
  remark: 'Remark',

  fieldFilter: 'Field Filter',
  creatorFilter: 'Creator Filter',
  deptFilter: 'Department Filter',
  dateRangeFilter: 'Date Range Filter',
  statusFilter: 'Status Filter',
  customSql: 'Custom SQL',

  // ... 其他翻译
}
```

## 六、错误处理

### 6.1 表单验证规则

```typescript
const rules: FormRules = {
  dataFilterName: [
    { required: true, message: t('dataScope.nameRequired'), trigger: 'blur' },
    { max: 64, message: '最大64个字符', trigger: 'blur' }
  ],
  entityClass: [
    { required: true, message: t('dataScope.entityRequired'), trigger: 'change' }
  ],
  ruleType: [
    { required: true, message: t('dataScope.ruleTypeRequired'), trigger: 'change' }
  ],
  ruleConfig: [
    { required: true, message: t('dataScope.ruleConfigRequired'), trigger: 'change' }
  ]
}
```

### 6.2 API错误处理

| 错误场景 | 处理方式 |
|---------|---------|
| 实体类未注册 | 显示警告提示，但不阻止保存 |
| SQL片段校验失败 | 显示后端返回的具体错误信息 |
| 规则名称重复 | 提示用户修改名称 |
| 网络错误 | 显示通用网络错误提示 |

### 6.3 操作确认

| 操作 | 确认方式 |
|------|---------|
| 删除 | 二次确认弹窗 |
| 状态切换 | 无确认，直接切换 |
| 表单提交 | 无确认，验证通过后直接提交 |

## 七、依赖关系

### 7.1 后端接口依赖

需要后端提供以下接口（部分可能需要新增）：

| 接口 | 状态 | 说明 |
|------|------|------|
| `/sysDataFilter/queryDataFilterList` | ✅ 已有 | 分页查询 |
| `/sysDataFilter/addDataFilter` | ✅ 已有 | 新增规则 |
| `/sysDataFilter/updateDataFilter` | ✅ 已有 | 更新规则 |
| `/sysDataFilter/deleteDataFilter` | ✅ 已有 | 删除规则，参数包裹在body中 |
| `/sysDataFilter/getDataFilterDetail` | ✅ 已有 | 获取详情 |
| `/sysDataFilter/getEntityFields` | ✅ 已有 | 获取实体字段 |
| `/sysDataFilter/refreshCache` | ✅ 已有 | 刷新缓存 |
| `/sysDataFilter/getEntityList` | ⚠️ **需新增** | 获取已注册实体列表，**核心功能依赖此接口** |

**实体列表接口设计建议：**
```java
@PostMapping("/getEntityList")
public ResponseDTO<List<EntityInfo>> getEntityList(@RequestBody RequestDTO<EmptyBody> reqDto) {
    // 返回所有标注了 @DataScopeEntity 的实体类信息
    // 包含: entityClass, entityName, tableName
}
```

**前端临时方案：** 若后端接口未就绪，可先使用手动输入实体类全路径的方式。

### 7.2 前端组件依赖

| 组件 | 用途 |
|------|------|
| ElTransfer | 字段穿梭框 |
| ElTreeSelect | 部门树选择 |
| ElCascader | 级联选择 |
| CodeMirror | SQL编辑器（可选） |

## 八、实现优先级

### 8.1 现有代码修正

现有占位代码 `/views/system/dataScope/index.vue` 需要修正：

| 问题 | 原代码 | 修正为 |
|------|--------|--------|
| 接口字段名 | `filterName` | `dataFilterName` |
| 规则类型 | `ruleType: number` | `ruleType: string`（枚举值） |
| 状态显示逻辑 | `status === 1 ? 'enabled' : 'disabled'` | `status === 0 ? 'enabled' : 'disabled'` |
| 规则类型映射 | 使用数字键 | 使用字符串枚举键 |

**修正后的规则类型映射：**
```typescript
const ruleTypeMap: Record<string, string> = {
  'FIELD_FILTER': '字段过滤',
  'CREATOR_FILTER': '创建人过滤',
  'DEPT_FILTER': '部门过滤',
  'DATE_RANGE_FILTER': '时间范围过滤',
  'STATUS_FILTER': '状态过滤',
  'CUSTOM_SQL': '自定义SQL',
}
```

### 8.2 实现优先级

1. **P0 - 核心功能**
   - 主页面列表、搜索、分页
   - 新增/编辑弹窗基础结构
   - API层对接

2. **P1 - 规则配置组件**
   - 字段过滤配置
   - 创建人过滤配置
   - 部门过滤配置

3. **P2 - 其他规则配置**
   - 时间范围配置
   - 状态过滤配置
   - 自定义SQL配置

4. **P3 - 增强功能**
   - SQL编辑器语法高亮
   - 常用模板快捷插入
   - 国际化完善