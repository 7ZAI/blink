# Gateway Admin Web 侧边栏实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 gateway-admin-web 添加可折叠侧边栏，支持后端动态菜单

**Architecture:** 使用 Element Plus ElMenu 组件 + 复用现有 userStore.menus 和 appStore.sidebarCollapsed。侧边栏作为 MainLayout 的子组件，桌面端固定显示，移动端使用 el-drawer 抽屉式显示。

**Tech Stack:** Vue 3 + TypeScript + Element Plus + Pinia + SCSS

---

## File Structure

```
blink-gateway/gateway-admin-web/src/
├── layouts/
│   ├── MainLayout.vue              # 修改：添加侧边栏布局
│   └── components/
│       └── Sidebar/
│           ├── index.vue            # 新建：侧边栏主组件
│           └── SidebarItem.vue      # 新建：递归菜单项组件
├── styles/
│   ├── variables.scss               # 修改：添加侧边栏 CSS 变量
│   └── sidebar.scss                 # 新建：侧边栏样式
└── locales/
    ├── zh-cn.ts                     # 修改：添加菜单 i18n
    └── en-us.ts                     # 修改：添加菜单 i18n
```

---

## Chunk 1: 样式基础

### Task 1: 添加侧边栏 CSS 变量

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/styles/variables.scss`

- [ ] **Step 1: 在 `:root` 中添加侧边栏变量**

在 `variables.scss` 的 `:root` 块中，在 `// Navigation` 注释块之后添加：

```scss
  // Sidebar
  --sidebar-width: 220px;
  --sidebar-collapse-width: 64px;
  --sidebar-bg: var(--card-bg);
  --sidebar-border-color: var(--border-color);
  --sidebar-item-hover-bg: rgba(0, 0, 0, 0.04);
  --sidebar-item-active-bg: rgba(59, 130, 246, 0.1);
  --sidebar-item-active-color: var(--primary-color);
```

- [ ] **Step 2: 在 `html[data-theme='dark']` 中添加暗黑模式变量**

在 `variables.scss` 的 `html[data-theme='dark']` 块中，在 `--nav-item-hover-bg` 之后添加：

```scss
  --sidebar-bg: var(--card-bg);
  --sidebar-border-color: var(--border-color);
  --sidebar-item-hover-bg: rgba(255, 255, 255, 0.08);
  --sidebar-item-active-bg: rgba(59, 130, 246, 0.2);
  --sidebar-item-active-color: var(--primary-color);
```

- [ ] **Step 3: 验证变量添加正确**

运行以下命令确认变量已添加：

```bash
grep -A 7 "// Sidebar" blink-gateway/gateway-admin-web/src/styles/variables.scss
```

预期输出应包含 `--sidebar-width` 等变量。

- [ ] **Step 4: 提交更改**

```bash
git add blink-gateway/gateway-admin-web/src/styles/variables.scss
git commit -m "style: add sidebar CSS variables for theming

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 2: 创建侧边栏样式文件

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/styles/sidebar.scss`

- [ ] **Step 1: 创建 sidebar.scss 文件**

```scss
@use './variables.scss' as *;

// ============================================
// Sidebar Layout
// ============================================

.sidebar {
  position: fixed;
  top: var(--nav-height);
  left: 0;
  bottom: 0;
  width: var(--sidebar-width);
  background-color: var(--sidebar-bg);
  border-right: 1px solid var(--sidebar-border-color);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-duration) ease;
  z-index: 90;
  overflow: hidden;

  &.is-collapse {
    width: var(--sidebar-collapse-width);

    .sidebar-header {
      justify-content: center;
      padding: 12px;
    }

    .el-menu {
      width: var(--sidebar-collapse-width);
    }
  }
}

// ============================================
// Sidebar Header (Collapse Button)
// ============================================

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 12px 16px;
  border-bottom: 1px solid var(--sidebar-border-color);
  min-height: 48px;
  flex-shrink: 0;

  .collapse-btn {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    border-radius: var(--radius-md);
    color: var(--text-color-secondary);
    transition: all 0.2s ease;

    &:hover {
      background-color: var(--sidebar-item-hover-bg);
      color: var(--text-color-primary);
    }
  }
}

// ============================================
// Sidebar Menu Styles
// ============================================

.sidebar {
  .el-scrollbar {
    flex: 1;
    overflow: hidden;
  }

  .el-menu {
    border-right: none;
    background-color: transparent;
    width: 100%;

    // Menu item
    .el-menu-item {
      height: 48px;
      line-height: 48px;
      color: var(--text-color-regular);
      transition: all 0.2s ease;

      &:hover {
        background-color: var(--sidebar-item-hover-bg);
      }

      &.is-active {
        color: var(--sidebar-item-active-color);
        background-color: var(--sidebar-item-active-bg);
      }

      .el-icon {
        margin-right: 8px;
        font-size: 18px;
      }
    }

    // Sub menu
    .el-sub-menu {
      .el-sub-menu__title {
        height: 48px;
        line-height: 48px;
        color: var(--text-color-regular);

        &:hover {
          background-color: var(--sidebar-item-hover-bg);
        }

        .el-icon {
          margin-right: 8px;
          font-size: 18px;
        }
      }

      &.is-active {
        > .el-sub-menu__title {
          color: var(--sidebar-item-active-color);
        }
      }

      .el-menu {
        .el-menu-item {
          padding-left: 48px !important;
        }
      }
    }
  }

  // Collapsed state menu styles
  &.is-collapse {
    .el-menu {
      .el-menu-item,
      .el-sub-menu__title {
        padding: 0 !important;
        justify-content: center;

        .el-icon {
          margin-right: 0;
        }

        span {
          display: none;
        }
      }
    }
  }
}

// ============================================
// Main Content with Sidebar
// ============================================

.main-content {
  margin-left: var(--sidebar-width);
  transition: margin-left var(--transition-duration) ease;
}

.sidebar.is-collapse ~ .main-content,
.main-content.sidebar-collapsed {
  margin-left: var(--sidebar-collapse-width);
}

// ============================================
// Mobile Drawer Sidebar
// ============================================

.mobile-sidebar-drawer {
  .el-drawer__body {
    padding: 0;
    background-color: var(--sidebar-bg);
  }

  .sidebar {
    position: relative;
    top: 0;
    width: 100%;
    border-right: none;
  }
}

// ============================================
// Responsive Styles
// ============================================

@media (max-width: 768px) {
  .sidebar {
    display: none;
  }

  .main-content {
    margin-left: 0 !important;
  }
}
```

- [ ] **Step 2: 在 index.scss 中导入 sidebar.scss**

在 `blink-gateway/gateway-admin-web/src/styles/index.scss` 末尾添加：

```scss
@use './sidebar.scss';
```

- [ ] **Step 3: 提交更改**

```bash
git add blink-gateway/gateway-admin-web/src/styles/sidebar.scss
git add blink-gateway/gateway-admin-web/src/styles/index.scss
git commit -m "style: add sidebar styles with responsive support

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Chunk 2: 侧边栏组件

### Task 3: 创建 SidebarItem.vue 递归菜单项组件

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/layouts/components/Sidebar/SidebarItem.vue`

- [ ] **Step 1: 创建 Sidebar 组件目录**

```bash
mkdir -p blink-gateway/gateway-admin-web/src/layouts/components/Sidebar
```

- [ ] **Step 2: 创建 SidebarItem.vue 文件**

> **关于图标说明**：项目已在 `main.ts` 中全局注册所有 Element Plus 图标（见 `app.component(key, component)`）。因此 `MenuVO.icon` 字段应返回 Element Plus 图标组件名称（如 "Odometer"、"Connection"、"Guide" 等），`<component :is="item.icon" />` 即可正确渲染图标。

```vue
<template>
  <!-- 目录类型：有子菜单 -->
  <el-sub-menu
    v-if="item.menuType === 1 && hasVisibleChildren"
    :index="String(item.menuId)"
    :popper-class="isCollapse ? 'sidebar-popper' : ''"
  >
    <template #title>
      <el-icon v-if="item.icon">
        <component :is="item.icon" />
      </el-icon>
      <span>{{ menuTitle }}</span>
    </template>
    <SidebarItem
      v-for="child in visibleChildren"
      :key="child.menuId"
      :item="child"
    />
  </el-sub-menu>

  <!-- 菜单类型：直接跳转 -->
  <el-menu-item
    v-else-if="item.menuType === 2"
    :index="item.path"
  >
    <el-icon v-if="item.icon">
      <component :is="item.icon" />
    </el-icon>
    <template #title>
      <span>{{ menuTitle }}</span>
    </template>
  </el-menu-item>

  <!-- 按钮类型：不显示 -->
</template>

<script setup lang="ts">
import { computed, inject, type ComputedRef } from 'vue'
import { useI18n } from 'vue-i18n'
import type { MenuVO } from '@/api/auth'

defineOptions({ name: 'SidebarItem' })

const props = defineProps<{
  item: MenuVO
}>()

const { t } = useI18n()

// 从父组件注入折叠状态（父组件提供的是 ComputedRef<boolean>）
const isCollapseRef = inject<ComputedRef<boolean>>('isCollapse')
const isCollapse = computed(() => isCollapseRef?.value ?? false)

// 菜单标题：优先使用 i18n，回退到 menuName
const menuTitle = computed(() => {
  const key = props.item.menuEnName?.toLowerCase() || ''
  return t(`menu.${key}`, props.item.menuName)
})

// 过滤可见的子菜单（排除按钮类型和不可见菜单）
const visibleChildren = computed(() => {
  if (!props.item.children) return []
  return props.item.children.filter(
    child => child.menuType !== 3 && child.visible !== 0
  )
})

// 是否有可见的子菜单
const hasVisibleChildren = computed(() => visibleChildren.value.length > 0)
</script>
```

- [ ] **Step 3: 提交更改**

```bash
git add blink-gateway/gateway-admin-web/src/layouts/components/Sidebar/SidebarItem.vue
git commit -m "feat: add SidebarItem recursive menu component

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 4: 创建 Sidebar/index.vue 侧边栏主组件

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/layouts/components/Sidebar/index.vue`

- [ ] **Step 1: 创建 Sidebar/index.vue 文件**

```vue
<template>
  <div class="sidebar" :class="{ 'is-collapse': appStore.sidebarCollapsed }">
    <!-- 折叠按钮 -->
    <div class="sidebar-header">
      <el-icon class="collapse-btn" @click="appStore.toggleSidebar()">
        <Fold v-if="!appStore.sidebarCollapsed" />
        <Expand v-else />
      </el-icon>
    </div>

    <!-- 菜单列表 -->
    <el-scrollbar>
      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        :unique-opened="true"
        router
      >
        <SidebarItem
          v-for="menu in userStore.menus"
          :key="menu.menuId"
          :item="menu"
        />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import { computed, provide } from 'vue'
import { useRoute } from 'vue-router'
import { Fold, Expand } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import SidebarItem from './SidebarItem.vue'

defineOptions({ name: 'Sidebar' })

const route = useRoute()
const appStore = useAppStore()
const userStore = useUserStore()

// 当前激活的菜单
const activeMenu = computed(() => route.path)

// 向子组件提供折叠状态
provide('isCollapse', computed(() => appStore.sidebarCollapsed))
</script>
```

- [ ] **Step 2: 提交更改**

```bash
git add blink-gateway/gateway-admin-web/src/layouts/components/Sidebar/index.vue
git commit -m "feat: add Sidebar main component with collapse support

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Chunk 3: 布局集成

### Task 5: 修改 MainLayout.vue 集成侧边栏

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/layouts/MainLayout.vue`

- [ ] **Step 1: 修改 MainLayout.vue 模板部分**

将整个 `<template>` 替换为：

```vue
<template>
  <div class="main-layout">
    <!-- Top Navigation -->
    <header class="top-nav">
      <div class="top-nav-left">
        <div class="nav-logo">
          <div class="logo-icon">
            <el-icon><Guide /></el-icon>
          </div>
          <span>Gateway Admin</span>
        </div>
      </div>
      <div class="top-nav-right">
        <!-- Mobile Menu Button -->
        <button class="mobile-menu-btn" @click="mobileDrawerVisible = true">
          <el-icon><Expand /></el-icon>
        </button>

        <!-- Fullscreen Toggle -->
        <button class="nav-action" :title="t('common.fullscreen')" @click="toggleFullscreen">
          <el-icon><FullScreen /></el-icon>
        </button>

        <!-- Theme Toggle -->
        <button class="nav-action" :title="themeStore.theme === 'light' ? t('common.darkMode') : t('common.lightMode')" @click="themeStore.toggleTheme">
          <el-icon v-if="themeStore.theme === 'light'"><Moon /></el-icon>
          <el-icon v-else><Sunny /></el-icon>
        </button>

        <!-- Language Switch -->
        <el-dropdown @command="handleLanguageChange">
          <button class="nav-action">
            {{ appStore.language === 'zh-cn' ? '中文' : 'EN' }}
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="zh-cn">中文</el-dropdown-item>
              <el-dropdown-item command="en-us">English</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- Desktop Sidebar -->
    <Sidebar />

    <!-- Mobile Drawer Sidebar -->
    <el-drawer
      v-model="mobileDrawerVisible"
      direction="ltr"
      :with-header="false"
      size="220px"
      class="mobile-sidebar-drawer"
    >
      <Sidebar />
    </el-drawer>

    <!-- Main Content -->
    <main
      class="main-content"
      :class="{ 'sidebar-collapsed': appStore.sidebarCollapsed }"
    >
      <router-view />
    </main>
  </div>
</template>
```

- [ ] **Step 2: 修改 script 部分**

将 `<script setup lang="ts">` 部分替换为：

```vue
<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useFullscreen } from '@vueuse/core'
import { Guide, Expand, FullScreen, Moon, Sunny } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { useThemeStore } from '@/stores/theme'
import Sidebar from './components/Sidebar/index.vue'

defineOptions({ name: 'MainLayout' })

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const appStore = useAppStore()
const themeStore = useThemeStore()
const { toggle: toggleFullscreen } = useFullscreen()

const mobileDrawerVisible = ref(false)

// 路由变化时关闭移动端抽屉
watch(() => route.path, () => {
  mobileDrawerVisible.value = false
})

const handleLanguageChange = (lang: string) => {
  appStore.setLanguage(lang)
  locale.value = lang
}
</script>
```

- [ ] **Step 3: 移除旧的顶部导航菜单样式（可选）**

由于 `.nav-menu` 已移除，可以保留相关样式作为备用，不影响功能。

- [ ] **Step 4: 提交更改**

```bash
git add blink-gateway/gateway-admin-web/src/layouts/MainLayout.vue
git commit -m "feat: integrate sidebar into MainLayout with mobile drawer

- Remove top navigation menu
- Add fixed sidebar for desktop
- Add drawer sidebar for mobile
- Adjust main content margin

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Chunk 4: 国际化和数据加载

### Task 6: 添加菜单 i18n 翻译

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/locales/zh-cn.ts`
- Modify: `blink-gateway/gateway-admin-web/src/locales/en-us.ts`

- [ ] **Step 1: 在 zh-cn.ts 中添加 menu 翻译**

在 `zh-cn.ts` 中，在 `common` 对象之后添加 `menu` 对象：

```typescript
  menu: {
    dashboard: '仪表盘',
    channel: '渠道管理',
    route: '路由管理',
    config: '配置管理',
    monitor: '监控中心',
    system: '系统管理',
    user: '用户管理',
    role: '角色管理',
    menu: '菜单管理'
  },
```

- [ ] **Step 2: 在 en-us.ts 中添加 menu 翻译**

在 `en-us.ts` 中，在 `common` 对象之后添加 `menu` 对象：

```typescript
  menu: {
    dashboard: 'Dashboard',
    channel: 'Channel',
    route: 'Route',
    config: 'Config',
    monitor: 'Monitor',
    system: 'System',
    user: 'User',
    role: 'Role',
    menu: 'Menu'
  },
```

- [ ] **Step 3: 提交更改**

```bash
git add blink-gateway/gateway-admin-web/src/locales/zh-cn.ts
git add blink-gateway/gateway-admin-web/src/locales/en-us.ts
git commit -m "feat: add menu i18n translations

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 7: 验证登录后加载菜单数据

**Files:**
- Verify: `blink-gateway/gateway-admin-web/src/views/login/index.vue`

> **注意**：登录页面已实现菜单加载功能（见 `login/index.vue` 第 134-135 行）。此任务为验证步骤。

- [ ] **Step 1: 验证登录逻辑包含菜单加载**

确认 `login/index.vue` 中的 `handleLogin` 函数包含以下代码：

```typescript
// Fetch user info and menus
await userStore.fetchUserInfo()
await userStore.fetchMenus()
```

如果已存在，则无需修改。

- [ ] **Step 2: 确认无需修改**

如验证通过，跳过此任务的修改步骤。

---

## Verification

### Manual Testing Checklist

完成所有任务后，进行以下手动测试：

- [ ] **桌面端测试**
  - 启动开发服务器：`npm run dev`
  - 登录系统
  - 确认侧边栏正确显示
  - 测试折叠/展开功能
  - 测试菜单导航跳转
  - 测试当前路由高亮
  - 测试暗黑模式切换

- [ ] **移动端测试**
  - 缩小浏览器窗口至 768px 以下
  - 确认侧边栏隐藏
  - 点击汉堡菜单打开 Drawer
  - 测试菜单导航
  - 确认点击菜单后 Drawer 关闭

- [ ] **多级菜单测试**
  - 展开有子菜单的目录
  - 确认子菜单正常显示
  - 测试子菜单导航

---

## Final Commit

- [ ] **最终提交**

```bash
git add -A
git commit -m "feat: add collapsible sidebar with dynamic menu support

- Add sidebar CSS variables for theming
- Create Sidebar and SidebarItem components
- Integrate sidebar into MainLayout
- Add mobile drawer support
- Add menu i18n translations
- Fetch menus after login

Closes: gateway-admin-web sidebar implementation

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```