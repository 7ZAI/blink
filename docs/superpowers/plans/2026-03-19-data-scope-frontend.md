# Data Scope Frontend Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the data scope (data permission) frontend page with full CRUD functionality and 6 rule type configurations.

**Architecture:** Single-page form dialog pattern with dynamic rule config components. API layer follows existing request/response patterns with PageResult<T>. Uses ElTransfer for field selection, tree selects for department selection, and smooth data loading transitions.

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vue I18n, Pinia

---

## File Structure

```
src/
├── api/
│   └── dataScope.ts                 # API layer - interfaces and functions
├── views/system/dataScope/
│   ├── index.vue                    # Main page (list, search, pagination)
│   └── components/
│       ├── DataFilterFormDialog.vue # Form dialog for add/edit
│       ├── FieldFilterConfig.vue    # Field filter config (ElTransfer)
│       ├── CreatorFilterConfig.vue  # Creator filter config
│       ├── DeptFilterConfig.vue     # Department filter config
│       ├── DateRangeConfig.vue      # Date range config
│       ├── StatusFilterConfig.vue   # Status filter config
│       └── CustomSqlConfig.vue      # Custom SQL config
└── locales/
    ├── zh-cn.ts                     # Add dataScope translations
    └── en-us.ts                     # Add dataScope translations
```

---

## Task 1: Create API Layer

**Files:**
- Create: `src/api/dataScope.ts`

- [ ] **Step 1: Create dataScope.ts with interfaces and API functions**

```typescript
import request from '@/utils/request'

// ==================== 数据类型定义 ====================

/**
 * 数据过滤规则信息
 */
export interface DataFilterInfo {
  dataFilterId: number
  dataFilterName: string
  dataFilterEnName: string
  entityClass: string
  tableName: string
  ruleType: string
  ruleConfig: string
  status: number
  remark: string
  createBy: string
  createTime: string
}

/**
 * 已注册实体信息
 */
export interface EntityInfo {
  entityClass: string
  entityName: string
  tableName: string
}

/**
 * 实体字段信息
 */
export interface EntityFieldVO {
  fieldName: string
  columnName: string
  fieldType: string
}

/**
 * 实体字段响应
 */
export interface EntityFieldsRsp {
  fields: EntityFieldVO[]
}

/**
 * 分页结果
 */
export interface PageResult<T> {
  pageNum: number
  pageSize: number
  total: number
  pages: number
  orderBy: string
  rows: T[]
}

/**
 * 查询参数
 */
export interface QueryDataFilterParams {
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
export interface AddDataFilterParams {
  dataFilterName: string
  dataFilterEnName?: string
  entityClass: string
  tableName: string
  ruleType: string
  ruleConfig: string
  remark?: string
}

/**
 * 更新参数
 */
export interface UpdateDataFilterParams {
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
export const getDataFilterList = (params: QueryDataFilterParams): Promise<PageResult<DataFilterInfo>> => {
  return request.post('/sysDataFilter/queryDataFilterList', { body: params }) as Promise<PageResult<DataFilterInfo>>
}

/**
 * 新增数据过滤规则
 */
export const addDataFilter = (params: AddDataFilterParams): Promise<void> => {
  return request.post('/sysDataFilter/addDataFilter', { body: params }) as Promise<void>
}

/**
 * 更新数据过滤规则
 */
export const updateDataFilter = (params: UpdateDataFilterParams): Promise<void> => {
  return request.post('/sysDataFilter/updateDataFilter', { body: params }) as Promise<void>
}

/**
 * 删除数据过滤规则
 */
export const deleteDataFilter = (dataFilterId: number): Promise<void> => {
  return request.post('/sysDataFilter/deleteDataFilter', { body: { dataFilterId } }) as Promise<void>
}

/**
 * 获取规则详情
 */
export const getDataFilterDetail = (dataFilterId: number): Promise<PageResult<DataFilterInfo>> => {
  return request.post('/sysDataFilter/getDataFilterDetail', { body: { dataFilterId } }) as Promise<PageResult<DataFilterInfo>>
}

/**
 * 获取已注册实体列表
 */
export const getEntityList = (): Promise<EntityInfo[]> => {
  return request.post('/sysDataFilter/getEntityList', {}) as Promise<EntityInfo[]>
}

/**
 * 获取实体字段列表
 */
export const getEntityFields = (entityClass: string): Promise<EntityFieldsRsp> => {
  return request.post('/sysDataFilter/getEntityFields', { body: { entityClass } }) as Promise<EntityFieldsRsp>
}

/**
 * 刷新缓存
 */
export const refreshCache = (): Promise<void> => {
  return request.post('/sysDataFilter/refreshCache', {}) as Promise<void>
}
```

- [ ] **Step 2: Commit API layer**

```bash
git add src/api/dataScope.ts
git commit -m "feat(dataScope): add API layer with interfaces and functions"
```

---

## Task 2: Add i18n Translations

**Files:**
- Modify: `src/locales/zh-cn.ts`
- Modify: `src/locales/en-us.ts`

- [ ] **Step 1: Add Chinese translations to zh-cn.ts**

Find the `dataScope` section and replace with:

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
    basicInfo: '基础信息',
    refreshCache: '刷新缓存',
    refreshCacheSuccess: '缓存刷新成功',

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
    matchMode: '匹配模式',

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

    // 验证
    nameRequired: '请输入规则名称',
    entityRequired: '请选择实体类',
    ruleTypeRequired: '请选择规则类型',
    ruleConfigRequired: '请配置规则',

    // 规则类型枚举值
    FIELD_FILTER: '字段过滤',
    CREATOR_FILTER: '创建人过滤',
    DEPT_FILTER: '部门过滤',
    DATE_RANGE_FILTER: '时间范围过滤',
    STATUS_FILTER: '状态过滤',
    CUSTOM_SQL: '自定义SQL',
  },
```

- [ ] **Step 2: Add English translations to en-us.ts**

Find the `dataScope` section and replace with:

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
    basicInfo: 'Basic Info',
    refreshCache: 'Refresh Cache',
    refreshCacheSuccess: 'Cache refreshed successfully',

    fieldFilter: 'Field Filter',
    creatorFilter: 'Creator Filter',
    deptFilter: 'Department Filter',
    dateRangeFilter: 'Date Range Filter',
    statusFilter: 'Status Filter',
    customSql: 'Custom SQL',

    excludeMode: 'Exclude Mode',
    includeMode: 'Include Mode',
    availableFields: 'Available Fields',
    selectedFields: 'Selected Fields',
    noFieldsSelected: 'Please select fields',
    matchMode: 'Match Mode',

    matchField: 'Match Field',
    matchType: 'Match Type',
    currentUser: 'Current User',
    specifiedUser: 'Specified User',
    roleUser: 'Role Users',
    selectUser: 'Select User',
    selectRole: 'Select Role',

    currentDept: 'Current Dept',
    specifiedDept: 'Specified Dept',
    deptAndChildren: 'Dept and Children',
    selectDept: 'Select Department',

    rangeType: 'Range Type',
    relativeTime: 'Relative Time',
    absoluteTime: 'Absolute Time',
    timeValue: 'Time Value',
    timeUnit: 'Unit',
    day: 'Day',
    week: 'Week',
    month: 'Month',
    year: 'Year',
    startTime: 'Start Time',
    endTime: 'End Time',

    allowedValues: 'Allowed Values',
    addValue: 'Add Value',
    inputPlaceholder: 'Press enter to add',

    sqlFragment: 'SQL Fragment',
    securityWarning: 'SQL fragment will be directly concatenated to WHERE clause, please ensure security',
    commonTemplates: 'Common Templates',
    last7Days: 'Last 7 Days',
    thisMonth: 'This Month',
    statusEnabled: 'Status Enabled',

    deleteConfirm: 'Are you sure you want to delete this data filter rule?',

    nameRequired: 'Please enter rule name',
    entityRequired: 'Please select entity class',
    ruleTypeRequired: 'Please select rule type',
    ruleConfigRequired: 'Please configure the rule',

    FIELD_FILTER: 'Field Filter',
    CREATOR_FILTER: 'Creator Filter',
    DEPT_FILTER: 'Department Filter',
    DATE_RANGE_FILTER: 'Date Range Filter',
    STATUS_FILTER: 'Status Filter',
    CUSTOM_SQL: 'Custom SQL',
  },
```

- [ ] **Step 3: Commit i18n changes**

```bash
git add src/locales/zh-cn.ts src/locales/en-us.ts
git commit -m "feat(dataScope): add i18n translations for data scope module"
```

---

## Task 3: Create Field Filter Config Component

**Files:**
- Create: `src/views/system/dataScope/components/FieldFilterConfig.vue`

- [ ] **Step 1: Create FieldFilterConfig.vue**

```vue
<template>
  <div class="field-filter-config">
    <el-form-item :label="t('dataScope.matchMode')">
      <el-radio-group v-model="matchMode" @change="handleMatchModeChange">
        <el-radio value="include">{{ t('dataScope.includeMode') }}</el-radio>
        <el-radio value="exclude">{{ t('dataScope.excludeMode') }}</el-radio>
      </el-radio-group>
    </el-form-item>

    <el-form-item :label="t('dataScope.ruleConfig')">
      <el-transfer
        v-model="selectedFields"
        :data="transferData"
        :titles="[t('dataScope.availableFields'), t('dataScope.selectedFields')]"
        :props="{
          key: 'fieldName',
          label: 'columnName'
        }"
        filterable
        :filter-placeholder="t('common.pleaseInput')"
        @change="handleFieldChange"
      />
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { EntityFieldVO } from '@/api/dataScope'

const { t } = useI18n()

interface Props {
  modelValue: string
  fields: EntityFieldVO[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

// 解析配置
interface FieldFilterConfig {
  excludeFields: string[] | null
  includeFields: string[] | null
}

const matchMode = ref<'include' | 'exclude'>('include')
const selectedFields = ref<string[]>([])

// Transfer 数据源
const transferData = computed(() => props.fields)

// 初始化配置
const initConfig = () => {
  if (!props.modelValue) {
    matchMode.value = 'include'
    selectedFields.value = []
    return
  }

  try {
    const config: FieldFilterConfig = JSON.parse(props.modelValue)
    if (config.includeFields && config.includeFields.length > 0) {
      matchMode.value = 'include'
      selectedFields.value = config.includeFields
    } else if (config.excludeFields && config.excludeFields.length > 0) {
      matchMode.value = 'exclude'
      selectedFields.value = config.excludeFields
    }
  } catch {
    matchMode.value = 'include'
    selectedFields.value = []
  }
}

// 监听 modelValue 变化
watch(() => props.modelValue, initConfig, { immediate: true })

// 匹配模式变更
const handleMatchModeChange = () => {
  updateConfig()
}

// 字段变更
const handleFieldChange = () => {
  updateConfig()
}

// 更新配置
const updateConfig = () => {
  const config: FieldFilterConfig = {
    excludeFields: matchMode.value === 'exclude' ? selectedFields.value : null,
    includeFields: matchMode.value === 'include' ? selectedFields.value : null
  }
  emit('update:modelValue', JSON.stringify(config))
}
</script>

<style scoped lang="scss">
.field-filter-config {
  :deep(.el-transfer) {
    display: flex;
    justify-content: center;
  }

  :deep(.el-transfer-panel) {
    width: 280px;
  }
}
</style>
```

- [ ] **Step 2: Commit FieldFilterConfig component**

```bash
git add src/views/system/dataScope/components/FieldFilterConfig.vue
git commit -m "feat(dataScope): add FieldFilterConfig component with ElTransfer"
```

---

## Task 4: Create Creator Filter Config Component

**Files:**
- Create: `src/views/system/dataScope/components/CreatorFilterConfig.vue`

- [ ] **Step 1: Create CreatorFilterConfig.vue**

```vue
<template>
  <div class="creator-filter-config">
    <el-form-item :label="t('dataScope.matchField')">
      <el-select v-model="config.field" :placeholder="t('common.pleaseSelect')" @change="updateConfig">
        <el-option
          v-for="field in fields"
          :key="field.fieldName"
          :label="field.columnName"
          :value="field.fieldName"
        />
      </el-select>
    </el-form-item>

    <el-form-item :label="t('dataScope.matchType')">
      <el-select v-model="config.matchType" :placeholder="t('common.pleaseSelect')" @change="updateConfig">
        <el-option :label="t('dataScope.currentUser')" value="CURRENT_USER" />
        <el-option :label="t('dataScope.specifiedUser')" value="SPECIFIED_USER" />
        <el-option :label="t('dataScope.roleUser')" value="ROLE_USER" />
      </el-select>
    </el-form-item>

    <!-- 指定用户选择 -->
    <el-form-item v-if="config.matchType === 'SPECIFIED_USER'" :label="t('dataScope.selectUser')">
      <el-select
        v-model="config.userIds"
        multiple
        filterable
        :placeholder="t('common.pleaseSelect')"
        @change="updateConfig"
      >
        <!-- TODO: 对接用户列表API -->
        <el-option label="用户1" :value="1" />
        <el-option label="用户2" :value="2" />
      </el-select>
    </el-form-item>

    <!-- 角色用户选择 -->
    <el-form-item v-if="config.matchType === 'ROLE_USER'" :label="t('dataScope.selectRole')">
      <el-select
        v-model="config.roleIds"
        multiple
        filterable
        :placeholder="t('common.pleaseSelect')"
        @change="updateConfig"
      >
        <!-- TODO: 对接角色列表API -->
        <el-option label="管理员" :value="1" />
        <el-option label="普通用户" :value="2" />
      </el-select>
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { EntityFieldVO } from '@/api/dataScope'

const { t } = useI18n()

interface Props {
  modelValue: string
  fields: EntityFieldVO[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

interface CreatorFilterConfig {
  field: string
  matchType: string
  userIds: number[] | null
  roleIds: number[] | null
}

const config = reactive<CreatorFilterConfig>({
  field: '',
  matchType: 'CURRENT_USER',
  userIds: null,
  roleIds: null
})

// 初始化配置
const initConfig = () => {
  if (!props.modelValue) {
    config.field = ''
    config.matchType = 'CURRENT_USER'
    config.userIds = null
    config.roleIds = null
    return
  }

  try {
    const parsed: CreatorFilterConfig = JSON.parse(props.modelValue)
    Object.assign(config, parsed)
  } catch {
    // 使用默认值
  }
}

watch(() => props.modelValue, initConfig, { immediate: true })

const updateConfig = () => {
  const result: CreatorFilterConfig = {
    field: config.field,
    matchType: config.matchType,
    userIds: config.matchType === 'SPECIFIED_USER' ? config.userIds : null,
    roleIds: config.matchType === 'ROLE_USER' ? config.roleIds : null
  }
  emit('update:modelValue', JSON.stringify(result))
}
</script>
```

- [ ] **Step 2: Commit CreatorFilterConfig component**

```bash
git add src/views/system/dataScope/components/CreatorFilterConfig.vue
git commit -m "feat(dataScope): add CreatorFilterConfig component"
```

---

## Task 5: Create Dept Filter Config Component

**Files:**
- Create: `src/views/system/dataScope/components/DeptFilterConfig.vue`

- [ ] **Step 1: Create DeptFilterConfig.vue**

```vue
<template>
  <div class="dept-filter-config">
    <el-form-item :label="t('dataScope.matchField')">
      <el-select v-model="config.field" :placeholder="t('common.pleaseSelect')" @change="updateConfig">
        <el-option
          v-for="field in fields"
          :key="field.fieldName"
          :label="field.columnName"
          :value="field.fieldName"
        />
      </el-select>
    </el-form-item>

    <el-form-item :label="t('dataScope.matchType')">
      <el-select v-model="config.matchType" :placeholder="t('common.pleaseSelect')" @change="updateConfig">
        <el-option :label="t('dataScope.currentDept')" value="CURRENT_DEPT" />
        <el-option :label="t('dataScope.specifiedDept')" value="DEPT_LIST" />
        <el-option :label="t('dataScope.deptAndChildren')" value="DEPT_AND_CHILDREN" />
      </el-select>
    </el-form-item>

    <!-- 部门选择 -->
    <el-form-item
      v-if="config.matchType === 'DEPT_LIST' || config.matchType === 'DEPT_AND_CHILDREN'"
      :label="t('dataScope.selectDept')"
    >
      <el-tree-select
        v-model="config.deptIds"
        :data="deptTree"
        multiple
        check-strictly
        filterable
        :placeholder="t('common.pleaseSelect')"
        @change="updateConfig"
      />
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import type { EntityFieldVO } from '@/api/dataScope'

const { t } = useI18n()

interface Props {
  modelValue: string
  fields: EntityFieldVO[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

interface DeptFilterConfig {
  field: string
  matchType: string
  deptIds: number[] | null
}

const config = reactive<DeptFilterConfig>({
  field: '',
  matchType: 'CURRENT_DEPT',
  deptIds: null
})

// 部门树数据
interface DeptTreeNode {
  value: number
  label: string
  children?: DeptTreeNode[]
}

const deptTree = ref<DeptTreeNode[]>([])

// 加载部门树
const loadDeptTree = async () => {
  // TODO: 对接部门树API
  deptTree.value = [
    { value: 1, label: '总公司', children: [
      { value: 2, label: '研发部' },
      { value: 3, label: '产品部' }
    ]}
  ]
}

// 初始化配置
const initConfig = () => {
  if (!props.modelValue) {
    config.field = ''
    config.matchType = 'CURRENT_DEPT'
    config.deptIds = null
    return
  }

  try {
    const parsed: DeptFilterConfig = JSON.parse(props.modelValue)
    Object.assign(config, parsed)
  } catch {
    // 使用默认值
  }
}

watch(() => props.modelValue, initConfig, { immediate: true })

const updateConfig = () => {
  const result: DeptFilterConfig = {
    field: config.field,
    matchType: config.matchType,
    deptIds: ['DEPT_LIST', 'DEPT_AND_CHILDREN'].includes(config.matchType) ? config.deptIds : null
  }
  emit('update:modelValue', JSON.stringify(result))
}

onMounted(() => {
  loadDeptTree()
})
</script>
```

- [ ] **Step 2: Commit DeptFilterConfig component**

```bash
git add src/views/system/dataScope/components/DeptFilterConfig.vue
git commit -m "feat(dataScope): add DeptFilterConfig component with tree select"
```

---

## Task 6: Create Date Range Config Component

**Files:**
- Create: `src/views/system/dataScope/components/DateRangeConfig.vue`

- [ ] **Step 1: Create DateRangeConfig.vue**

```vue
<template>
  <div class="date-range-config">
    <el-form-item :label="t('dataScope.matchField')">
      <el-select v-model="config.field" :placeholder="t('common.pleaseSelect')" @change="updateConfig">
        <el-option
          v-for="field in fields"
          :key="field.fieldName"
          :label="field.columnName"
          :value="field.fieldName"
        />
      </el-select>
    </el-form-item>

    <el-form-item :label="t('dataScope.rangeType')">
      <el-radio-group v-model="config.rangeType" @change="updateConfig">
        <el-radio value="RELATIVE">{{ t('dataScope.relativeTime') }}</el-radio>
        <el-radio value="ABSOLUTE">{{ t('dataScope.absoluteTime') }}</el-radio>
      </el-radio-group>
    </el-form-item>

    <!-- 相对时间 -->
    <template v-if="config.rangeType === 'RELATIVE'">
      <el-form-item :label="t('dataScope.timeValue')">
        <div class="time-input-group">
          <el-input-number v-model="config.relativeValue" @change="updateConfig" />
          <el-select v-model="config.relativeUnit" :placeholder="t('common.pleaseSelect')" @change="updateConfig">
            <el-option :label="t('dataScope.day')" value="DAY" />
            <el-option :label="t('dataScope.week')" value="WEEK" />
            <el-option :label="t('dataScope.month')" value="MONTH" />
            <el-option :label="t('dataScope.year')" value="YEAR" />
          </el-select>
        </div>
      </el-form-item>
    </template>

    <!-- 绝对时间 -->
    <template v-if="config.rangeType === 'ABSOLUTE'">
      <el-form-item :label="t('dataScope.startTime')">
        <el-date-picker
          v-model="config.startTime"
          type="datetime"
          :placeholder="t('common.pleaseSelect')"
          @change="updateConfig"
        />
      </el-form-item>
      <el-form-item :label="t('dataScope.endTime')">
        <el-date-picker
          v-model="config.endTime"
          type="datetime"
          :placeholder="t('common.pleaseSelect')"
          @change="updateConfig"
        />
      </el-form-item>
    </template>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { EntityFieldVO } from '@/api/dataScope'

const { t } = useI18n()

interface Props {
  modelValue: string
  fields: EntityFieldVO[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

interface DateRangeConfig {
  field: string
  rangeType: string
  relativeValue: number | null
  relativeUnit: string
  startTime: string | null
  endTime: string | null
}

const config = reactive<DateRangeConfig>({
  field: '',
  rangeType: 'RELATIVE',
  relativeValue: -7,
  relativeUnit: 'DAY',
  startTime: null,
  endTime: null
})

// 初始化配置
const initConfig = () => {
  if (!props.modelValue) {
    config.field = ''
    config.rangeType = 'RELATIVE'
    config.relativeValue = -7
    config.relativeUnit = 'DAY'
    config.startTime = null
    config.endTime = null
    return
  }

  try {
    const parsed: DateRangeConfig = JSON.parse(props.modelValue)
    Object.assign(config, parsed)
  } catch {
    // 使用默认值
  }
}

watch(() => props.modelValue, initConfig, { immediate: true })

const updateConfig = () => {
  const result: DateRangeConfig = {
    field: config.field,
    rangeType: config.rangeType,
    relativeValue: config.rangeType === 'RELATIVE' ? config.relativeValue : null,
    relativeUnit: config.rangeType === 'RELATIVE' ? config.relativeUnit : 'DAY',
    startTime: config.rangeType === 'ABSOLUTE' ? config.startTime : null,
    endTime: config.rangeType === 'ABSOLUTE' ? config.endTime : null
  }
  emit('update:modelValue', JSON.stringify(result))
}
</script>

<style scoped lang="scss">
.time-input-group {
  display: flex;
  gap: 12px;

  .el-input-number {
    width: 120px;
  }

  .el-select {
    width: 100px;
  }
}
</style>
```

- [ ] **Step 2: Commit DateRangeConfig component**

```bash
git add src/views/system/dataScope/components/DateRangeConfig.vue
git commit -m "feat(dataScope): add DateRangeConfig component with relative/absolute time"
```

---

## Task 7: Create Status Filter Config Component

**Files:**
- Create: `src/views/system/dataScope/components/StatusFilterConfig.vue`

- [ ] **Step 1: Create StatusFilterConfig.vue**

```vue
<template>
  <div class="status-filter-config">
    <el-form-item :label="t('dataScope.matchField')">
      <el-select v-model="config.field" :placeholder="t('common.pleaseSelect')" @change="updateConfig">
        <el-option
          v-for="field in fields"
          :key="field.fieldName"
          :label="field.columnName"
          :value="field.fieldName"
        />
      </el-select>
    </el-form-item>

    <el-form-item :label="t('dataScope.allowedValues')">
      <div class="values-container">
        <el-tag
          v-for="(value, index) in displayValues"
          :key="index"
          closable
          type="primary"
          @close="removeValue(index)"
        >
          {{ value }}
        </el-tag>
        <el-input
          v-model="inputValue"
          class="input-new-value"
          :placeholder="t('dataScope.inputPlaceholder')"
          @keyup.enter="addValue"
        />
      </div>
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { EntityFieldVO } from '@/api/dataScope'

const { t } = useI18n()

interface Props {
  modelValue: string
  fields: EntityFieldVO[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

interface StatusFilterConfig {
  field: string
  allowedValues: (string | number)[]
}

const config = reactive<StatusFilterConfig>({
  field: '',
  allowedValues: []
})

const inputValue = ref('')

// 显示的值列表
const displayValues = computed(() => config.allowedValues)

// 初始化配置
const initConfig = () => {
  if (!props.modelValue) {
    config.field = ''
    config.allowedValues = []
    return
  }

  try {
    const parsed: StatusFilterConfig = JSON.parse(props.modelValue)
    Object.assign(config, parsed)
  } catch {
    // 使用默认值
  }
}

watch(() => props.modelValue, initConfig, { immediate: true })

// 添加值
const addValue = () => {
  const value = inputValue.value.trim()
  if (value && !config.allowedValues.includes(value)) {
    // 尝试转换为数字
    const numValue = Number(value)
    config.allowedValues.push(isNaN(numValue) ? value : numValue)
    updateConfig()
  }
  inputValue.value = ''
}

// 移除值
const removeValue = (index: number) => {
  config.allowedValues.splice(index, 1)
  updateConfig()
}

const updateConfig = () => {
  emit('update:modelValue', JSON.stringify({
    field: config.field,
    allowedValues: config.allowedValues
  }))
}
</script>

<style scoped lang="scss">
.values-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;

  .el-tag {
    margin: 0;
  }

  .input-new-value {
    width: 120px;
  }
}
</style>
```

- [ ] **Step 2: Commit StatusFilterConfig component**

```bash
git add src/views/system/dataScope/components/StatusFilterConfig.vue
git commit -m "feat(dataScope): add StatusFilterConfig component with tag input"
```

---

## Task 8: Create Custom SQL Config Component

**Files:**
- Create: `src/views/system/dataScope/components/CustomSqlConfig.vue`

- [ ] **Step 1: Create CustomSqlConfig.vue**

```vue
<template>
  <div class="custom-sql-config">
    <el-alert
      :title="t('dataScope.securityWarning')"
      type="warning"
      :closable="false"
      show-icon
      class="security-warning"
    />

    <el-form-item :label="t('dataScope.commonTemplates')">
      <el-button-group>
        <el-button size="small" @click="insertTemplate('last7days')">
          {{ t('dataScope.last7Days') }}
        </el-button>
        <el-button size="small" @click="insertTemplate('thismonth')">
          {{ t('dataScope.thisMonth') }}
        </el-button>
        <el-button size="small" @click="insertTemplate('statusenabled')">
          {{ t('dataScope.statusEnabled') }}
        </el-button>
      </el-button-group>
    </el-form-item>

    <el-form-item :label="t('dataScope.sqlFragment')">
      <el-input
        v-model="sqlFragment"
        type="textarea"
        :rows="6"
        :placeholder="t('common.pleaseInput')"
        @input="updateConfig"
      />
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

interface Props {
  modelValue: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const sqlFragment = ref('')

// 初始化配置
const initConfig = () => {
  if (!props.modelValue) {
    sqlFragment.value = ''
    return
  }

  try {
    const parsed = JSON.parse(props.modelValue)
    sqlFragment.value = parsed.sqlFragment || ''
  } catch {
    sqlFragment.value = ''
  }
}

watch(() => props.modelValue, initConfig, { immediate: true })

// 插入模板
const insertTemplate = (type: string) => {
  const templates: Record<string, string> = {
    last7days: "create_time > DATE_SUB(NOW(), INTERVAL 7 DAY)",
    thismonth: "DATE_FORMAT(create_time, '%Y-%m') = DATE_FORMAT(NOW(), '%Y-%m')",
    statusenabled: "status = 0"
  }

  const template = templates[type]
  if (template) {
    sqlFragment.value = sqlFragment.value
      ? `${sqlFragment.value} AND ${template}`
      : template
    updateConfig()
  }
}

const updateConfig = () => {
  emit('update:modelValue', JSON.stringify({
    sqlFragment: sqlFragment.value
  }))
}
</script>

<style scoped lang="scss">
.custom-sql-config {
  .security-warning {
    margin-bottom: 16px;
  }
}
</style>
```

- [ ] **Step 2: Commit CustomSqlConfig component**

```bash
git add src/views/system/dataScope/components/CustomSqlConfig.vue
git commit -m "feat(dataScope): add CustomSqlConfig component with templates"
```

---

## Task 9: Create Form Dialog Component

**Files:**
- Create: `src/views/system/dataScope/components/DataFilterFormDialog.vue`

- [ ] **Step 1: Create DataFilterFormDialog.vue**

```vue
<template>
  <el-dialog
    v-model="visible"
    :title="type === 'add' ? t('dataScope.addTitle') : t('dataScope.editTitle')"
    width="800px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="120px"
      class="data-filter-form"
    >
      <el-divider content-position="left">{{ t('dataScope.basicInfo') }}</el-divider>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="t('dataScope.filterName')" prop="dataFilterName">
            <el-input v-model.trim="formData.dataFilterName" :placeholder="t('common.pleaseInput')" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('dataScope.filterEnName')">
            <el-input v-model.trim="formData.dataFilterEnName" :placeholder="t('common.pleaseInput')" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="t('dataScope.entityClass')" prop="entityClass">
            <el-select
              v-model="formData.entityClass"
              :placeholder="t('common.pleaseSelect')"
              :disabled="type === 'edit'"
              filterable
              @change="handleEntityChange"
            >
              <el-option
                v-for="entity in entityList"
                :key="entity.entityClass"
                :label="entity.entityName"
                :value="entity.entityClass"
              >
                <span>{{ entity.entityName }}</span>
                <span class="entity-class-path">{{ entity.entityClass }}</span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('dataScope.tableName')">
            <el-input v-model="formData.tableName" disabled :placeholder="t('common.pleaseInput')" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="t('dataScope.ruleType')" prop="ruleType">
            <el-select
              v-model="formData.ruleType"
              :placeholder="t('common.pleaseSelect')"
              @change="handleRuleTypeChange"
            >
              <el-option
                v-for="option in ruleTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('dataScope.status')">
            <el-switch
              v-model="formData.status"
              :active-value="0"
              :inactive-value="1"
              :active-text="t('common.enabled')"
              :inactive-text="t('common.disabled')"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item :label="t('dataScope.remark')">
        <el-input v-model.trim="formData.remark" type="textarea" :rows="2" :placeholder="t('common.pleaseInput')" />
      </el-form-item>

      <el-divider content-position="left">{{ t('dataScope.ruleConfig') }}</el-divider>

      <div v-if="formData.ruleType" class="rule-config-area">
        <FieldFilterConfig
          v-if="formData.ruleType === 'FIELD_FILTER'"
          v-model="formData.ruleConfig"
          :fields="entityFields"
        />
        <CreatorFilterConfig
          v-else-if="formData.ruleType === 'CREATOR_FILTER'"
          v-model="formData.ruleConfig"
          :fields="entityFields"
        />
        <DeptFilterConfig
          v-else-if="formData.ruleType === 'DEPT_FILTER'"
          v-model="formData.ruleConfig"
          :fields="entityFields"
        />
        <DateRangeConfig
          v-else-if="formData.ruleType === 'DATE_RANGE_FILTER'"
          v-model="formData.ruleConfig"
          :fields="entityFields"
        />
        <StatusFilterConfig
          v-else-if="formData.ruleType === 'STATUS_FILTER'"
          v-model="formData.ruleConfig"
          :fields="entityFields"
        />
        <CustomSqlConfig
          v-else-if="formData.ruleType === 'CUSTOM_SQL'"
          v-model="formData.ruleConfig"
        />
      </div>
      <el-empty v-else :description="t('dataScope.ruleTypeRequired')" />
    </el-form>

    <template #footer>
      <el-button @click="handleClose">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  getEntityList,
  getEntityFields,
  addDataFilter,
  updateDataFilter,
  getDataFilterDetail,
  type DataFilterInfo,
  type EntityInfo,
  type EntityFieldVO
} from '@/api/dataScope'
import FieldFilterConfig from './FieldFilterConfig.vue'
import CreatorFilterConfig from './CreatorFilterConfig.vue'
import DeptFilterConfig from './DeptFilterConfig.vue'
import DateRangeConfig from './DateRangeConfig.vue'
import StatusFilterConfig from './StatusFilterConfig.vue'
import CustomSqlConfig from './CustomSqlConfig.vue'

const { t } = useI18n()

interface Props {
  modelValue: boolean
  type: 'add' | 'edit'
  data: DataFilterInfo | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'success': []
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const formRef = ref<FormInstance>()
const submitting = ref(false)
const entityList = ref<EntityInfo[]>([])
const entityFields = ref<EntityFieldVO[]>([])

// 表单数据
const formData = reactive({
  dataFilterId: 0,
  dataFilterName: '',
  dataFilterEnName: '',
  entityClass: '',
  tableName: '',
  ruleType: '',
  ruleConfig: '',
  status: 0,
  remark: ''
})

// 规则类型选项
const ruleTypeOptions = computed(() => [
  { value: 'FIELD_FILTER', label: t('dataScope.fieldFilter') },
  { value: 'CREATOR_FILTER', label: t('dataScope.creatorFilter') },
  { value: 'DEPT_FILTER', label: t('dataScope.deptFilter') },
  { value: 'DATE_RANGE_FILTER', label: t('dataScope.dateRangeFilter') },
  { value: 'STATUS_FILTER', label: t('dataScope.statusFilter') },
  { value: 'CUSTOM_SQL', label: t('dataScope.customSql') }
])

// 表单验证规则
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
  ]
}

// 加载实体列表
const loadEntityList = async () => {
  try {
    entityList.value = await getEntityList()
  } catch {
    // 静默失败
  }
}

// 实体类变更
const handleEntityChange = async (entityClass: string) => {
  const entity = entityList.value.find(e => e.entityClass === entityClass)
  if (entity) {
    formData.tableName = entity.tableName
  }

  // 加载字段列表
  try {
    const res = await getEntityFields(entityClass)
    entityFields.value = res.fields || []
  } catch {
    entityFields.value = []
  }
}

// 规则类型变更 - 清空规则配置
const handleRuleTypeChange = () => {
  formData.ruleConfig = ''
}

// 加载详情
const loadDetail = async () => {
  if (props.type !== 'edit' || !props.data?.dataFilterId) return

  try {
    const res = await getDataFilterDetail(props.data.dataFilterId)
    const detail = res.rows?.[0]
    if (detail) {
      Object.assign(formData, {
        dataFilterId: detail.dataFilterId,
        dataFilterName: detail.dataFilterName,
        dataFilterEnName: detail.dataFilterEnName || '',
        entityClass: detail.entityClass,
        tableName: detail.tableName,
        ruleType: detail.ruleType,
        ruleConfig: detail.ruleConfig || '',
        status: detail.status,
        remark: detail.remark || ''
      })

      // 加载字段列表
      const fieldsRes = await getEntityFields(detail.entityClass)
      entityFields.value = fieldsRes.fields || []
    }
  } catch {
    ElMessage.error('加载详情失败')
  }
}

// 监听弹窗打开
watch(visible, (val) => {
  if (val) {
    if (props.type === 'add') {
      resetForm()
    } else {
      loadDetail()
    }
  }
})

// 重置表单
const resetForm = () => {
  formData.dataFilterId = 0
  formData.dataFilterName = ''
  formData.dataFilterEnName = ''
  formData.entityClass = ''
  formData.tableName = ''
  formData.ruleType = ''
  formData.ruleConfig = ''
  formData.status = 0
  formData.remark = ''
  entityFields.value = []
  formRef.value?.resetFields()
}

// 关闭弹窗
const handleClose = () => {
  visible.value = false
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      if (props.type === 'add') {
        await addDataFilter({
          dataFilterName: formData.dataFilterName,
          dataFilterEnName: formData.dataFilterEnName || undefined,
          entityClass: formData.entityClass,
          tableName: formData.tableName,
          ruleType: formData.ruleType,
          ruleConfig: formData.ruleConfig,
          remark: formData.remark || undefined
        })
        ElMessage.success(t('message.success'))
      } else {
        await updateDataFilter({
          dataFilterId: formData.dataFilterId,
          dataFilterName: formData.dataFilterName,
          dataFilterEnName: formData.dataFilterEnName || undefined,
          ruleConfig: formData.ruleConfig,
          status: formData.status,
          remark: formData.remark || undefined
        })
        ElMessage.success(t('message.success'))
      }

      emit('success')
      handleClose()
    } finally {
      submitting.value = false
    }
  })
}

onMounted(() => {
  loadEntityList()
})
</script>

<style scoped lang="scss">
.data-filter-form {
  .entity-class-path {
    margin-left: 8px;
    color: var(--text-color-secondary);
    font-size: 12px;
  }

  .rule-config-area {
    padding: 16px;
    background: var(--bg-color-page);
    border-radius: 4px;
  }
}
</style>
```

- [ ] **Step 2: Commit DataFilterFormDialog component**

```bash
git add src/views/system/dataScope/components/DataFilterFormDialog.vue
git commit -m "feat(dataScope): add DataFilterFormDialog with dynamic rule config"
```

---

## Task 10: Implement Main Page

**Files:**
- Modify: `src/views/system/dataScope/index.vue`

- [ ] **Step 1: Rewrite index.vue with full implementation**

```vue
<template>
  <div class="data-scope-management">
    <el-card class="search-card" shadow="never">
      <div class="search-buttons">
        <el-button type="info" @click="handleSearch">
          <el-icon><Search /></el-icon>{{ t('common.search') }}
        </el-button>
        <el-button @click="handleReset">
          <el-icon><Refresh /></el-icon>{{ t('common.reset') }}
        </el-button>
      </div>
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('dataScope.filterName')">
          <el-input v-model.trim="searchForm.dataFilterName" :placeholder="t('common.pleaseInput')" clearable />
        </el-form-item>
        <el-form-item :label="t('dataScope.entityClass')">
          <el-select v-model="searchForm.entityClass" :placeholder="t('common.pleaseSelect')" clearable filterable>
            <el-option
              v-for="entity in entityList"
              :key="entity.entityClass"
              :label="entity.entityName"
              :value="entity.entityClass"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('dataScope.ruleType')">
          <el-select v-model="searchForm.ruleType" :placeholder="t('common.pleaseSelect')" clearable>
            <el-option
              v-for="option in ruleTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select v-model="searchForm.status" :placeholder="t('common.pleaseSelect')" clearable>
            <el-option :label="t('common.enabled')" :value="0" />
            <el-option :label="t('common.disabled')" :value="1" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <div class="header-left">
            <span class="title">{{ t('dataScope.title') }}</span>
            <el-button type="success" @click="handleAdd">
              <el-icon><Plus /></el-icon>{{ t('common.add') }}
            </el-button>
            <el-button @click="handleRefreshCache">
              <el-icon><Refresh /></el-icon>{{ t('dataScope.refreshCache') }}
            </el-button>
          </div>
        </div>
      </template>

      <div
        class="table-wrapper data-transition-wrapper"
        :class="[transitionClass, { 'data-loaded': !loading && dataFilterList.length > 0 }]"
      >
        <el-table v-loading="loading" :data="dataFilterList" stripe border>
          <el-table-column prop="dataFilterId" label="ID" width="80" align="center" />
          <el-table-column prop="dataFilterName" :label="t('dataScope.filterName')" min-width="120" />
          <el-table-column prop="entityClass" :label="t('dataScope.entityClass')" min-width="150">
            <template #default="{ row }">
              <el-tooltip :content="row.entityClass" placement="top">
                <span class="entity-name">{{ getEntityShortName(row.entityClass) }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="tableName" :label="t('dataScope.tableName')" min-width="120" />
          <el-table-column prop="ruleType" :label="t('dataScope.ruleType')" width="140" align="center">
            <template #default="{ row }">
              <el-tag>{{ t(`dataScope.${row.ruleType}`) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" :label="t('common.status')" width="100" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status"
                :active-value="0"
                :inactive-value="1"
                @change="handleStatusChange(row, $event)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="createTime" :label="t('common.createTime')" width="180" align="center" />
          <el-table-column :label="t('common.operation')" width="150" fixed="right">
            <template #default="{ row }">
              <div class="operation-buttons">
                <el-button type="primary" link size="small" @click="handleEdit(row)">
                  <el-icon><Edit /></el-icon>{{ t('common.edit') }}
                </el-button>
                <el-button type="danger" link size="small" @click="handleDelete(row)">
                  <el-icon><Delete /></el-icon>{{ t('common.delete') }}
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchDataFilterList"
        @current-change="fetchDataFilterList"
      />
    </el-card>

    <DataFilterFormDialog
      v-model="dialogVisible"
      :type="dialogType"
      :data="currentRow"
      @success="fetchDataFilterList"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  getDataFilterList,
  deleteDataFilter,
  updateDataFilter,
  refreshCache,
  getEntityList,
  type DataFilterInfo,
  type EntityInfo
} from '@/api/dataScope'
import { useDataTransition } from '@/composables/useDataTransition'
import DataFilterFormDialog from './components/DataFilterFormDialog.vue'

defineOptions({
  name: 'SystemDataScope',
})

const { t } = useI18n()
const { transitionClass, startTransition, completeTransition } = useDataTransition()

const loading = ref(false)
const dataFilterList = ref<DataFilterInfo[]>([])
const entityList = ref<EntityInfo[]>([])

const searchForm = reactive({
  dataFilterName: '',
  entityClass: '',
  ruleType: '',
  status: undefined as number | undefined
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const dialogVisible = ref(false)
const dialogType = ref<'add' | 'edit'>('add')
const currentRow = ref<DataFilterInfo | null>(null)

// 规则类型选项
const ruleTypeOptions = computed(() => [
  { value: 'FIELD_FILTER', label: t('dataScope.fieldFilter') },
  { value: 'CREATOR_FILTER', label: t('dataScope.creatorFilter') },
  { value: 'DEPT_FILTER', label: t('dataScope.deptFilter') },
  { value: 'DATE_RANGE_FILTER', label: t('dataScope.dateRangeFilter') },
  { value: 'STATUS_FILTER', label: t('dataScope.statusFilter') },
  { value: 'CUSTOM_SQL', label: t('dataScope.customSql') }
])

// 获取实体简短名称
const getEntityShortName = (entityClass: string) => {
  const parts = entityClass.split('.')
  return parts[parts.length - 1]
}

// 加载实体列表
const loadEntityList = async () => {
  try {
    entityList.value = await getEntityList()
  } catch {
    // 静默失败
  }
}

// 加载数据过滤规则列表
const fetchDataFilterList = async () => {
  startTransition()
  loading.value = true
  try {
    const res = await getDataFilterList({
      ...pagination,
      ...searchForm
    })
    dataFilterList.value = res.rows || []
    pagination.total = res.total || 0
  } catch {
    dataFilterList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
    completeTransition()
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  fetchDataFilterList()
}

// 重置
const handleReset = () => {
  searchForm.dataFilterName = ''
  searchForm.entityClass = ''
  searchForm.ruleType = ''
  searchForm.status = undefined
  pagination.pageNum = 1
  fetchDataFilterList()
}

// 新增
const handleAdd = () => {
  dialogType.value = 'add'
  currentRow.value = null
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: DataFilterInfo) => {
  dialogType.value = 'edit'
  currentRow.value = row
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row: DataFilterInfo) => {
  try {
    await ElMessageBox.confirm(t('dataScope.deleteConfirm'), t('message.tips'), {
      type: 'warning',
    })
    await deleteDataFilter(row.dataFilterId)
    ElMessage.success(t('message.deleteSuccess'))
    fetchDataFilterList()
  } catch {
    // 取消删除
  }
}

// 状态切换
const handleStatusChange = async (row: DataFilterInfo, status: number) => {
  try {
    await updateDataFilter({
      dataFilterId: row.dataFilterId,
      dataFilterName: row.dataFilterName,
      ruleConfig: row.ruleConfig,
      status
    })
    ElMessage.success(t('message.success'))
    fetchDataFilterList()
  } catch {
    // 失败后刷新列表恢复原状态
    fetchDataFilterList()
  }
}

// 刷新缓存
const handleRefreshCache = async () => {
  try {
    await refreshCache()
    ElMessage.success(t('dataScope.refreshCacheSuccess'))
  } catch {
    // 静默失败
  }
}

onMounted(() => {
  loadEntityList()
  fetchDataFilterList()
})
</script>

<style scoped lang="scss">
.data-scope-management {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;

  .search-card {
    flex-shrink: 0;

    :deep(.el-card__body) {
      padding: 12px 16px;
    }

    .search-buttons {
      margin-bottom: 12px;
    }

    .search-form {
      display: flex;
      flex-wrap: nowrap;
      align-items: center;

      :deep(.el-form-item) {
        margin-bottom: 0;
        margin-right: 16px;

        &:last-child {
          margin-right: 0;
        }
      }

      :deep(.el-input),
      :deep(.el-select) {
        width: auto;
        min-width: 140px;
        max-width: 200px;
      }
    }
  }

  .table-card {
    flex: 1;
    display: flex;
    flex-direction: column;

    :deep(.el-card__header) {
      padding: 0;
    }

    :deep(.el-card__body) {
      flex: 1;
      display: flex;
      flex-direction: column;
      padding: 16px;
    }

    .table-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid var(--border-color-light);

      .header-left {
        display: flex;
        align-items: center;
        gap: 12px;

        .title {
          font-weight: 500;
          color: var(--text-color-primary);
        }
      }
    }

    .table-wrapper {
      flex: 1;
    }

    .data-transition-wrapper {
      opacity: 0;
      transform: translateY(10px);
      transition: opacity 0.3s ease, transform 0.3s ease;

      &.data-enter,
      &.data-loaded {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .entity-name {
      cursor: pointer;
    }

    .el-pagination {
      margin-top: 16px;
      justify-content: flex-end;
    }
  }

  .operation-buttons {
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    gap: 0;

    .el-button {
      padding: 4px 6px;
      margin: 0;
    }
  }
}
</style>
```

- [ ] **Step 2: Commit main page implementation**

```bash
git add src/views/system/dataScope/index.vue
git commit -m "feat(dataScope): implement main page with CRUD functionality"
```

---

## Task 11: Final Testing and Integration

- [ ] **Step 1: Run frontend development server**

```bash
cd blink-base/blink-base-web && npm run dev
```

- [ ] **Step 2: Verify all features work correctly**

Test checklist:
- [ ] Search with different filters
- [ ] Add new data filter rule with each rule type
- [ ] Edit existing rule
- [ ] Delete rule with confirmation
- [ ] Status toggle switch
- [ ] Refresh cache button
- [ ] Pagination
- [ ] i18n language switching

- [ ] **Step 3: Final commit**

```bash
git add -A
git commit -m "feat(dataScope): complete data scope frontend implementation"
```

---

## Notes

1. **Entity List API**: The `getEntityList` API may not exist on backend yet. A temporary workaround is to use manual input or mock data.

2. **User/Role/Dept Selection**: The CreatorFilterConfig and DeptFilterConfig components have placeholder data for user/role/department selection. These need to be connected to actual APIs when available.

3. **Status Convention**: Status 0 = enabled (green), 1 = disabled (gray). This is consistent with backend.

4. **Edit Mode Restrictions**: entityClass and tableName are read-only in edit mode, matching backend UpdateDataFilterReq structure.