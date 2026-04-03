# 前端开发规范

本文件定义 Blink 项目前端开发规范，适用 Vue 3 + TypeScript + Element Plus。

## 前端 Commands

```bash
cd blink-base/blink-base-web

# Install dependencies
npm install

# Development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

## 1. API层规范 (`src/api/*.ts`)

**接口命名约定：**
- 请求参数：`XxxParams` 或 `XxxReq`
- 响应数据：`XxxRsp` 或 `XxxResult`
- 数据实体：`XxxInfo` 或 `XxxDetail`

**公共类型导入：**
```typescript
// ✅ 正确 - 从统一类型文件导入
import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'

// ❌ 错误 - 在 API 文件中重复定义类型
export interface PageResult<T> { ... }  // 禁止重复定义
```

**API函数定义：**
```typescript
// 统一使用POST方法，参数包裹在body中
export const getUserList = (params: QueryUserParams): Promise<PageResult<UserInfo>> => {
  return request.post('/sysUser/getSysUserList', { body: params }) as Promise<PageResult<UserInfo>>
}

// 分页响应格式 - 定义在 src/types/index.ts
export interface PageResult<T> {
  pageNum: number
  pageSize: number
  total: number
  pages: number
  orderBy?: string
  rows: T[]
}
```

## 2. 页面组件规范 (`src/views/**/*.vue`)

**Script Setup 结构顺序：**
```vue
<script setup lang="ts">
// 1. 导入顺序：Vue核心 → 第三方库 → 组件 → API → 工具
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import UserFormDialog from './components/UserFormDialog.vue'
import { getUserList, type UserInfo } from '@/api/user'

// 2. defineOptions定义组件名
defineOptions({ name: 'SystemUser' })

// 3. i18n
const { t } = useI18n()

// 4. 响应式状态
const loading = ref(false)
const searchForm = reactive({ loginName: '', username: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

// 5. 数据获取方法：try-finally处理loading
const fetchUserList = async () => {
  loading.value = true
  try {
    const res = await getUserList({ ...pagination, ...searchForm })
    userList.value = res.rows || []
    pagination.total = res.total || 0
  } finally {
    loading.value = false
  }
}

// 6. 事件处理方法：handle前缀
const handleSearch = () => {
  pagination.pageNum = 1
  fetchUserList()
}

// 7. 生命周期
onMounted(() => { fetchUserList() })
</script>
```

## 3. 数据加载平滑过渡规范（重要）

**原则：** 数据加载展示（列表、树、图形等）需要**平滑出现**，不能闪屏出现。

**实现方式：** 使用 `useTransition` composable 和 CSS过渡动画：

```vue
<template>
  <div
    class="table-wrapper data-transition-wrapper"
    :class="transitionClass"
  >
    <el-table v-loading="loading" :data="dataList">
      <!-- ... -->
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { useTransition } from '@/composables/useDataTransition'

const { transitionClass, startTransition, finishTransition } = useTransition()

const fetchData = async () => {
  startTransition()
  loading.value = true
  try {
    const res = await getData()
    dataList.value = res.rows || []
  } finally {
    loading.value = false
    finishTransition()
  }
}
</script>

<style scoped lang="scss">
// 过渡动画样式已定义在全局 styles/index.scss 中
// 只需添加 data-transition-wrapper 类和 :class="transitionClass" 即可
</style>
```

## 4. 国际化规范

**所有用户可见文本必须使用 i18n：**
```vue
<template>
  <el-button>{{ t('common.search') }}</el-button>
  <el-table-column :label="t('user.loginName')" />
  <el-form-item :label="t('user.username')">
    <el-input :placeholder="t('common.pleaseInput') + t('user.username')" />
  </el-form-item>
</template>

<script setup lang="ts">
const { t } = useI18n()

// 消息提示也使用 i18n
ElMessage.success(t('message.saveSuccess'))
ElMessageBox.confirm(t('user.deleteConfirm'), t('message.tips'))
</script>
```

**语言文件位置：**
- `src/locales/zh-cn.ts` - 中文
- `src/locales/en-us.ts` - 英文

## 5. 样式规范

**CSS变量（支持暗黑模式）：**
```scss
.component-name {
  background: var(--card-bg);
  border: 1px solid var(--border-color-light);
  color: var(--text-color-primary);
}
```

**常用CSS变量：**
| 变量 | 用途 |
|------|------|
| `--card-bg` | 卡片背景 |
| `--bg-color` / `--bg-color-page` | 页面背景 |
| `--border-color-light` | 边框颜色 |
| `--text-color-primary` | 主要文本 |
| `--text-color-secondary` | 次要文本 |
| `--primary-color` | 主题色 |

**深度选择器：**
```scss
:deep(.el-card__body) {
  padding: 16px;
}
```

## 6. 组件拆分规范

**目录结构：**
```
views/system/user/
├── index.vue              # 主页面（搜索、表格、分页）
└── components/
    ├── UserFormDialog.vue # 表单弹窗
    ├── UserDetailDialog.vue # 详情弹窗
    └── AssignRoleDialog.vue # 分配角色弹窗
```

**弹窗组件通信：**
```vue
<!-- 父组件 -->
<UserFormDialog
  v-model="dialogVisible"
  :type="dialogType"
  :data="currentRow"
  @success="fetchUserList"
/>

<!-- 子组件 -->
<script setup lang="ts">
const props = defineProps<{
  modelValue: boolean
  type: 'add' | 'edit'
  data: UserInfo | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'success': []
}>()
</script>
```

## 7. 表格操作列规范

```vue
<el-table-column :label="t('common.operation')" width="280" fixed="right">
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
```

## 8. 前端命名约定

| 类型 | 约定 | 示例 |
|------|------|------|
| 接口(请求) | XxxParams / XxxReq | `QueryUserParams` |
| 接口(响应) | XxxRsp / XxxResult | `PageResult` |
| 数据实体 | XxxInfo / XxxDetail | `UserInfo` |
| 页面组件 | PascalCase | `SystemUser` |
| 方法 | handleXxx / fetchXxx | `handleSearch`, `fetchUserList` |
| 状态变量 | camelCase | `searchForm`, `loading` |

## 9. 图标使用

```vue
<!-- Element Plus 图标 -->
<el-icon><User /></el-icon>
<BlinkIcon icon="User" />

<!-- MDI 图标 -->
<BlinkIcon icon="mdi:cog" />
```

## 10. 表单验证规范

表单验证规则必须使用 i18n 消息，**使用 computed 包装以支持响应式语言切换**：

```typescript
// ✅ 正确 - 使用 computed 包装验证规则
const rules = computed<FormRules>(() => ({
  loginName: [
    { required: true, message: t('validation.required', { field: t('user.loginName') }), trigger: 'blur' },
    { min: 3, max: 20, message: t('validation.length', { min: 3, max: 20 }), trigger: 'blur' }
  ],
  phone: [
    { required: true, message: t('validation.required', { field: t('user.phone') }), trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: t('validation.phone'), trigger: 'blur' }
  ]
}))

// ❌ 错误 - 静态定义会导致语言切换后验证消息不变
const rules: FormRules = {
  loginName: [
    { required: true, message: '请输入登录名', trigger: 'blur' }  // 硬编码中文
  ]
}
```

**验证相关 i18n 键值（已添加到 locales）：**
- `validation.required` - 必填验证，支持 `{field}` 占位符
- `validation.length` - 长度验证，支持 `{min}` `{max}` 占位符
- `validation.phone` - 手机号格式验证
- `validation.email` - 邮箱格式验证
- `validation.passwordNotMatch` - 密码不匹配

## 11. 暗黑模式（Dark Mode）规范

### 11.1 实现原理

项目使用 **Tailwind CSS 的 `dark` class 模式**：
- 通过在 `<html>` 元素上添加/移除 `dark` class 切换深色模式
- 使用 `useThemeStore` 管理主题状态 (`src/stores/theme.ts`)
- 在 `main.ts` 中初始化主题并应用

```typescript
// src/stores/theme.ts
import { useThemeStore } from '@/stores/theme'
const themeStore = useThemeStore()
themeStore.setTheme('dark') // 切换到深色模式
```

### 11.2 全局 CSS 变量

项目已在 `src/styles/index.scss` 中定义了完整的 CSS 变量，组件应优先使用这些变量：

```scss
// 背景色
--bg-color          // 页面背景
--bg-color-page     // 卡片/容器背景
--card-bg           // 卡片背景

// 边框色
--border-color-base // 基础边框
--border-color-light // 浅色边框

// 文本色
--text-color-primary   // 主要文本
--text-color-regular   // 常规文本
--text-color-secondary // 次要文本
--text-color-placeholder // 占位符文本

// 输入控件
--input-bg         // 输入框背景
--input-border     // 输入框边框

// 表格
--table-header-bg      // 表头背景
--table-row-hover      // 行悬停背景
--table-border-color   // 表格边框
```

### 11.3 组件开发规范

**必须使用 CSS 变量，禁止硬编码颜色：**

```scss
// ✅ 正确 - 使用 CSS 变量
.component-name {
  background-color: var(--card-bg);
  color: var(--text-color-primary);
  border-color: var(--border-color-light);
}

// ❌ 错误 - 硬编码颜色
.component-name {
  background-color: #ffffff;
  color: #1e293b;
  border-color: #e2e8f0;
}
```

**处理 placeholder 和禁用状态的特殊颜色：**

```scss
.component-name {
  // 输入框占位符
  :deep(.el-input__placeholder),
  :deep(.el-select__placeholder) {
    color: var(--text-color-placeholder);
  }

  // 选中文本颜色
  :deep(.el-select__selected-item) {
    color: var(--text-color-primary);
  }

  // 禁用状态
  :deep(.is-disabled) {
    background-color: var(--input-disabled-bg);
  }
}
```

### 11.4 深色模式适配检查清单

新建组件或修改现有组件时，确保以下场景已适配：

| 检查项 | 说明 |
|--------|------|
| 背景色 | 使用 `var(--card-bg)`、`var(--bg-color-page)` |
| 文本色 | 使用 `var(--text-color-primary)`、`var(--text-color-regular)` |
| 占位符 | 使用 `var(--text-color-placeholder)` |
| 边框色 | 使用 `var(--border-color-light)` |
| 悬停态 | 使用 `var(--table-row-hover)` |
| 输入框 | 使用 `var(--input-bg)`、`var(--input-border)` |
| 穿梭框 | `el-transfer` 的 panel 背景和边框 |
| 树选择 | `el-tree-select` 的背景和文本颜色 |

### 11.5 测试验证

开发完成后，手动切换主题进行验证：

1. 切换到深色模式，检查所有弹窗、下拉框、表格等组件
2. 重点关注：表单输入、穿梭框、树形选择器、日期选择器
3. 确认无白屏或颜色错乱问题

## 12. 错误处理规范（重要）

### 12.1 禁止空 catch 块

**所有 catch 块必须包含错误处理逻辑，禁止留空：**

```typescript
// ✅ 正确 - 记录错误日志
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getData()
    dataList.value = res.rows || []
  } catch (error) {
    console.error('[ComponentName] Failed to fetch data:', error)
  } finally {
    loading.value = false
  }
}

// ✅ 正确 - 处理用户取消操作
const handleDelete = async (row: UserInfo) => {
  try {
    await ElMessageBox.confirm(t('user.deleteConfirm'), t('message.tips'))
    await deleteUser({ userId: row.userId })
    ElMessage.success(t('message.deleteSuccess'))
  } catch (error) {
    // 用户取消删除
    if (error !== 'cancel') {
      console.error('[UserManagement] Failed to delete user:', error)
    }
  }
}

// ❌ 错误 - 空 catch 块会隐藏错误
const fetchData = async () => {
  try {
    const res = await getData()
  } catch (error) {
    // 空 catch 块 - 禁止！
  }
}
```

### 12.2 错误日志格式

**使用统一的日志前缀格式：**

```typescript
// 格式：[组件名/模块名] 操作描述
console.error('[UserManagement] Failed to fetch user list:', error)
console.error('[DictData] Failed to delete dict data:', error)
console.error('[Workflow] Failed to start process:', error)
```

### 12.3 请求错误处理

**HTTP 错误已在 `src/utils/request.ts` 统一处理，业务代码只需关注业务逻辑：**

```typescript
// request.ts 已统一处理：
// - 网络错误 (networkError)
// - 401 未授权 (unauthorized)
// - 403 禁止访问 (forbidden)
// - 404 资源不存在 (notFound)
// - 超时 (timeout)
// - 服务器错误 (serverError)

// 业务代码只需处理特定业务异常
const handleSave = async () => {
  try {
    await saveData(form)
    ElMessage.success(t('message.saveSuccess'))
  } catch (error) {
    // request.ts 已显示错误消息，此处只需记录日志
    console.error('[ComponentName] Save failed:', error)
  }
}
```

## 13. 未使用导入清理

**定期清理未使用的导入，保持代码整洁：**

```typescript
// ❌ 错误 - 包含未使用的导入
import { getUserList, deleteUser, type UserInfo } from '@/api/user'
import { getAvatarUrl } from '@/utils/avatar'  // 未使用！
import { useUserStore } from '@/stores/user'

// ✅ 正确 - 只导入需要的模块
import { getUserList, deleteUser, type UserInfo } from '@/api/user'
import { useUserStore } from '@/stores/user'
```

## 14. 防止重复提交

**使用 `useSubmitGuard` composable 防止重复提交：**

```typescript
import { useSubmitGuard } from '@/composables/useSubmitGuard'

const { isSubmitting, submitGuard } = useSubmitGuard()

// 按钮使用 isSubmitting 控制 loading 状态
// <el-button :loading="isSubmitting" @click="handleSubmit">

const handleSubmit = async () => {
  await submitGuard(async () => {
    // 提交逻辑
    await saveData(params)
    ElMessage.success('保存成功')
  })
}
```

**参数说明：**
- `delay`: 防抖延迟时间（毫秒），默认 1000ms
- `isSubmitting`: 当前是否正在提交
- `submitGuard`: 包装提交函数的包装器

**使用场景：**
- 表单提交按钮
- 确认操作按钮
- 保存/删除等关键操作