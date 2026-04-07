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

### 4.1 路由国际化规范（重要）

**路由 `meta.title` 必须使用 i18n key，禁止硬编码文本：**

```typescript
// ✅ 正确 - 使用 i18n key
{
  path: 'user',
  name: 'SystemUser',
  meta: { title: 'menu.user', icon: 'User' }
}

// ❌ 错误 - 硬编码中文
{
  path: 'user',
  name: 'SystemUser',
  meta: { title: '用户管理', icon: 'User' }
}
```

**i18n key 命名规范：**
- 使用 `menu.` 前缀
- 使用 camelCase（如 `userList`、`onlineUser`）
- 与数据库 `menu_en_name` 字段对应（CamelCase 转 camelCase）

### 4.2 侧边栏菜单国际化（重要）

菜单数据从数据库获取，国际化处理流程：

1. **数据库字段**：`menu_en_name` 存储 CamelCase 格式（如 `UserList`、`System Config`）
2. **前端转换**：`SidebarMenu.vue` 组件自动将 CamelCase 转为 camelCase
3. **i18n 匹配**：查找 `menu.{camelCase}` 对应的翻译

```
数据库 menu_en_name: 'UserList' → 转换 → 'userList' → t('menu.userList')
数据库 menu_en_name: 'System Config' → 转换 → 'systemConfig' → t('menu.systemConfig')
```

**添加新菜单时的步骤：**

1. 数据库插入菜单时，`menu_en_name` 使用 PascalCase 或空格分隔的英文
2. 在 `locales/zh-cn.ts` 和 `locales/en-us.ts` 的 `menu` 对象中添加对应翻译
3. 如果是路由页面，同时在路由 `meta.title` 中使用相同 i18n key

### 4.3 标签页国际化

标签页标题使用响应式计算属性实现国际化切换：

```vue
<script setup lang="ts">
// 使用 computed 包装，响应式响应语言切换
const translatedTabs = computed(() => {
  return props.tabs.map(tab => ({
    ...tab,
    translatedTitle: t(tab.title)  // tab.title 存储的是 i18n key
  }))
})
</script>

<template>
  <div v-for="tab in translatedTabs" :key="tab.path">
    <span>{{ tab.translatedTitle }}</span>
  </div>
</template>
```

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

## 15. 头像处理规范

### 15.1 头像存储格式

用户头像字段 `avatar` 存储的是头像样式名称（如 `lorelei`、`fun-emoji`），而非完整 URL。

### 15.2 头像解析函数

使用 `getLocalAvatarUrl` 函数将头像名称转换为实际 SVG 资源 URL：

```typescript
import { getLocalAvatarUrl } from '@/utils/avatar'

// 获取头像 URL
const avatarUrl = getLocalAvatarUrl(user.avatar)
```

### 15.3 组件中使用头像

**Layout 组件层级传递 `avatarResolver`：**

```vue
<!-- layout/index.vue - 定义 resolver -->
<script setup lang="ts">
import { getLocalAvatarUrl } from '@/utils/avatar'

const avatarResolver = (user: { avatar?: string }) => {
  return getLocalAvatarUrl(user.avatar)
}
</script>

<template>
  <MainLayout :avatar-resolver="avatarResolver" ... />
</template>

<!-- MainLayout.vue - 透传给 Header -->
<Header :avatar-resolver="avatarResolver" ... />

<!-- Header.vue - 透传给 UserDropdown -->
<UserDropdown :avatar-resolver="avatarResolver" ... />

<!-- UserDropdown.vue - 使用 resolver 解析头像 -->
<el-avatar :src="resolveAvatar" />
```

**关键点：**
- `avatarResolver` 函数在顶层定义，逐层透传
- 使用 computed 属性确保响应式
- 头像不存在时自动回退到默认头像

## 16. 布局组件开发规范

### 16.1 组件层级结构

```
MainLayout.vue           # 主布局容器
├── Sidebar/             # 侧边栏
│   └── SidebarMenu.vue  # 菜单项（递归组件）
├── Header/              # 头部
│   ├── ThemeToggle.vue  # 主题切换
│   ├── LanguageSwitch.vue # 语言切换
│   ├── FullscreenToggle.vue # 全屏切换
│   └── UserDropdown/    # 用户下拉菜单
└── TabsView/            # 标签页视图
```

### 16.2 布局组件设计原则

1. **Props 驱动**：所有配置通过 props 传入，组件内部不直接访问 store
2. **Slot 扩展**：每个区域提供 slot 支持自定义内容
3. **事件透传**：用户操作通过 emit 传递给父组件处理
4. **状态解耦**：使用 composable 管理状态，组件只负责渲染

### 16.3 新增布局配置步骤

添加新的配置项时，需要同步修改：

1. `MainLayout.vue` - 添加 props 定义和透传
2. `Header.vue` 或 `Sidebar.vue` - 接收并使用 props
3. `layout/index.vue` - 传入配置值
4. 更新类型定义 `Props` 接口

## 17. 命名约定补充

### 17.1 数据库字段与前端映射

| 数据库字段 | 前端使用 | 说明 |
|-----------|---------|------|
| `menu_en_name` | i18n key | PascalCase → camelCase 转换后作为 `menu.` 前缀的 key |
| `avatar` | 头像名称 | 通过 `getLocalAvatarUrl()` 转换为 URL |

### 17.2 国际化 Key 命名规范

| 类型 | 前缀 | 示例 |
|------|------|------|
| 菜单 | `menu.` | `menu.userList` |
| 通用 | `common.` | `common.search` |
| 用户 | `user.` | `user.loginName` |
| 验证 | `validation.` | `validation.required` |
| 消息 | `message.` | `message.saveSuccess` |
| 头部 | `header.` | `header.profile` |
| 标签页 | `tabs.` | `tabs.refresh` |
| 偏好设置 | `preferences.` | `preferences.theme` |
| 头像 | `avatar.` | `avatar.clickToChange` |

## 18. BlinkDialog 弹窗组件规范

### 18.1 基本用法

使用 `BlinkDialog` 替代直接使用 `el-dialog`，统一弹窗风格：

```vue
<template>
  <BlinkDialog
    v-model="visible"
    title="新增用户"
    @confirm="handleSubmit"
  >
    <el-form>
      <!-- 表单内容 -->
    </el-form>
  </BlinkDialog>
</template>
```

### 18.2 Props 配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| modelValue | boolean | - | 控制显示，支持 v-model |
| title | string | '' | 弹窗标题 |
| width | string \| number | '500px' | 弹窗宽度 |
| loading | boolean | false | 内容区域 loading |
| confirmLoading | boolean | false | 确认按钮 loading |
| showFooter | boolean | true | 是否显示底部 |
| showCancel | boolean | true | 是否显示取消按钮 |
| showConfirm | boolean | true | 是否显示确认按钮 |
| cancelText | string | '取消' | 取消按钮文本 |
| confirmText | string | '确定' | 确认按钮文本 |
| confirmType | string | 'primary' | 确认按钮类型 |
| closeOnClickModal | boolean | false | 点击遮罩关闭 |
| beforeClose | function | - | 关闭前回调 |

### 18.3 自定义底部

使用 `#footer` 插槽自定义底部内容：

```vue
<BlinkDialog v-model="visible" title="分配角色">
  <RoleTransfer v-model="selectedRoles" />

  <template #footer>
    <el-button @click="visible = false">取消</el-button>
    <el-button type="primary" :loading="submitting" @click="handleSubmit">
      确定分配
    </el-button>
  </template>
</BlinkDialog>
```

### 18.4 关闭确认

使用 `beforeClose` 实现关闭确认：

```vue
<script setup lang="ts">
const hasChanged = ref(false)

const handleBeforeClose = (done: () => void) => {
  if (hasChanged.value) {
    ElMessageBox.confirm('有未保存的更改，确定关闭吗？', '提示', {
      type: 'warning'
    }).then(() => {
      done()
    }).catch(() => {})
  } else {
    done()
  }
}
</script>

<template>
  <BlinkDialog v-model="visible" :before-close="handleBeforeClose">
    <!-- 内容 -->
  </BlinkDialog>
</template>
```

### 18.5 Events

| 事件 | 说明 |
|------|------|
| confirm | 点击确认按钮 |
| cancel | 点击取消按钮 |
| open | 弹窗打开 |
| opened | 弹窗打开动画结束 |
| close | 弹窗关闭 |
| closed | 弹窗关闭动画结束 |

## 19. BlinkTable 表格组件规范

### 19.1 基本用法

使用 `BlinkTable` 替代直接使用 `el-table`，统一表格风格：

```vue
<template>
  <BlinkTable
    :data="userList"
    :columns="columns"
    :loading="loading"
    :selectable="true"
    v-model:selected-keys="selectedIds"
    @selection-change="handleSelectionChange"
  />
</template>
```

### 19.2 列配置（配置模式）

```typescript
const columns: TableColumn[] = [
  { prop: 'loginName', label: '登录名', minWidth: 120 },
  { prop: 'username', label: '用户名', minWidth: 120 },
  {
    prop: 'sex',
    label: '性别',
    type: 'tag',
    tagOptions: [
      { label: '男', value: 1, type: 'primary' },
      { label: '女', value: 2, type: 'danger' },
    ]
  },
  { prop: 'createTime', label: '创建时间', type: 'datetime', width: 160 },
]
```

### 19.3 操作列配置

```typescript
const operations: TableOperation[] = [
  { label: '编辑', type: 'primary', onClick: handleEdit },
  {
    label: '删除',
    type: 'danger',
    onClick: handleDelete,
    visible: (row) => row.roleId !== 1  // 超级管理员不显示删除按钮
  },
]
```

### 19.4 插槽模式（复杂场景）

使用 `BlinkTableColumn` 子组件自定义列内容，适用于需要自定义渲染的场景：

```vue
<template>
  <BlinkTable :data="userList" :loading="loading">
    <BlinkTableColumn prop="loginName" label="登录名" />
    <BlinkTableColumn prop="avatar" label="头像">
      <template #default="{ row }">
        <el-avatar :src="getAvatarUrl(row.avatar)" />
      </template>
    </BlinkTableColumn>
    <BlinkTableColumn prop="status" label="状态">
      <template #default="{ row }">
        <el-tag :type="getStatusType(row.status)">
          {{ getStatusLabel(row.status) }}
        </el-tag>
      </template>
    </BlinkTableColumn>
  </BlinkTable>
</template>
```

> **注意**: `BlinkTableColumn` 仅支持 `type="index" | "selection" | "expand"`。标签、图片等特殊列类型需通过自定义插槽实现。

### 19.5 选择功能

```vue
<template>
  <BlinkTable
    :data="userList"
    :selectable="true"
    v-model:selected-keys="selectedIds"
    :check-selectable="checkSelectable"
    @selection-change="handleSelectionChange"
  />
</template>

<script setup lang="ts">
// 判断行是否可选
const checkSelectable = (row: UserInfo) => {
  return row.superFlag !== 1  // 超级管理员不可选
}
</script>
```

### 19.6 Props 配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| data | T[] | - | 表格数据 |
| columns | TableColumn[] | - | 列配置（配置模式） |
| rowKey | string \| function | 'id' | 行唯一标识 |
| selectable | boolean | false | 是否可选 |
| selectedKeys | (string \| number)[] | - | 选中行 keys，支持 v-model |
| selectType | 'checkbox' \| 'radio' | 'checkbox' | 选择类型 |
| checkSelectable | function | - | 判断行是否可选 |
| loading | boolean | false | 加载状态 |
| emptyText | string | '暂无数据' | 空数据提示 |
| height | string \| number | - | 表格高度 |
| maxHeight | string \| number | - | 最大高度 |
| stripe | boolean | true | 斑马纹 |
| border | boolean | true | 边框 |
| showOverflowTooltip | boolean | true | 溢出 tooltip |
| operations | TableOperation[] | - | 操作列配置 |
| operationWidth | number | 200 | 操作列宽度 |
| operationFixed | string \| boolean | 'right' | 操作列固定 |

### 19.7 Events

| 事件 | 说明 |
|------|------|
| selection-change | 选择变化 |
| select | 选中某一行 |
| select-all | 全选 |
| sort-change | 排序变化 |
| row-click | 点击行 |
| row-dblclick | 双击行 |

### 19.8 Expose 方法

```vue
<script setup lang="ts">
const tableRef = ref()

// 清空选择
const clearSelection = () => {
  tableRef.value?.clearSelection()
}

// 切换行选择状态
const toggleRowSelection = (row) => {
  tableRef.value?.toggleRowSelection(row)
}

// 全选/取消全选
const toggleAllSelection = () => {
  tableRef.value?.toggleAllSelection()
}

// 清空排序
const clearSort = () => {
  tableRef.value?.clearSort()
}

// 重新布局
const doLayout = () => {
  tableRef.value?.doLayout()
}
</script>

<template>
  <BlinkTable ref="tableRef" :data="userList" :selectable="true" />
</template>
```