# Gateway Admin Web 侧边栏设计文档

## 概述

为 gateway-admin-web 前端项目添加可折叠侧边栏，支持后端动态菜单。

## 需求

1. **请求代理**：已配置 `/gateway-admin` 代理到 `http://127.0.0.1:8003`，无需修改
2. **侧边栏**：添加可折叠侧边栏，支持后端动态菜单

## 技术方案

采用 Element Plus ElMenu + 现有 Pinia Store。

## 现有资源复用

### 已有接口和类型

- `api/auth.ts` 中已有 `MenuVO` 接口和 `getUserMenus()` API
- `stores/user.ts` 中已有 `menus` ref 和 `fetchMenus()` action
- `stores/app.ts` 中已有 `sidebarCollapsed` state 和 `toggleSidebar()` action

**复用策略**：直接使用现有资源，无需创建新的 menu store。

## 架构设计

### 文件结构

```
blink-gateway/gateway-admin-web/src/
├── layouts/
│   ├── MainLayout.vue          # 修改：添加侧边栏布局
│   └── components/
│       └── Sidebar/
│           ├── index.vue        # 侧边栏主组件
│           └── SidebarItem.vue  # 菜单项组件（递归渲染子菜单）
├── styles/
│   ├── variables.scss           # 修改：添加侧边栏变量
│   └── sidebar.scss             # 新增：侧边栏样式
└── locales/
    ├── zh-cn.ts                 # 修改：添加菜单 i18n
    └── en-us.ts                 # 修改：添加菜单 i18n
```

### 布局结构

```
┌────────────────────────────────────────────┐
│  Top Header (Logo + 主题/语言/用户信息)      │
├──────────┬─────────────────────────────────┤
│          │                                 │
│ Sidebar  │        Main Content             │
│ (可折叠)  │        (router-view)            │
│          │                                 │
└──────────┴─────────────────────────────────┘
```

## 组件设计

### MainLayout.vue 修改

- 移除顶部导航菜单（nav-menu），只保留 Logo、主题切换、语言切换、用户信息
- 添加移动端汉堡菜单按钮（触发侧边栏 Drawer）
- 主内容区添加 `margin-left` 适配侧边栏宽度

### Sidebar/index.vue

侧边栏主组件：

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
```

### SidebarItem.vue

递归组件，负责渲染菜单项：

- 使用 `<el-sub-menu>` 渲染目录类型（menuType === 1）
- 使用 `<el-menu-item>` 渲染菜单类型（menuType === 2）
- 忽略按钮类型（menuType === 3）
- 图标动态渲染（支持 Element Plus 图标组件名）
- 活跃菜单根据当前路由高亮

### 移动端实现

使用 `el-drawer` 组件：

```vue
<!-- 移动端 Drawer 侧边栏 -->
<el-drawer
  v-model="mobileDrawerVisible"
  direction="ltr"
  :with-header="false"
  size="220px"
>
  <Sidebar @select="mobileDrawerVisible = false" />
</el-drawer>
```

- 触发条件：屏幕宽度 ≤ 768px
- 触发方式：点击顶部汉堡菜单按钮
- 点击菜单项后自动关闭 Drawer

## 数据模型

### MenuVO（已存在于 api/auth.ts）

```typescript
interface MenuVO {
  menuId: number
  menuName: string           // 菜单名称
  menuEnName: string         // 英文名（用于 i18n）
  parentId: number           // 父级ID
  menuLevel: number          // 层级
  menuType: number           // 类型: 1-目录 2-菜单 3-按钮
  path: string               // 路由地址
  component: string          // 组件路径
  perms: string              // 权限标识
  icon: string               // 图标
  orderNum: number           // 排序
  visible: number            // 是否可见
  status: number             // 状态
  createTime: string
  children?: MenuVO[]        // 子菜单
}
```

## 数据流程

```
1. 用户登录成功 (login/index.vue)
   ↓
2. 调用 userStore.fetchMenus() 获取用户菜单
   ↓
3. 菜单存储在 userStore.menus
   ↓
4. Sidebar 组件从 userStore.menus 读取并渲染
   ↓
5. 折叠状态通过 appStore.sidebarCollapsed 控制
```

## 样式设计

### CSS 变量（添加到 variables.scss）

```scss
:root {
  // Sidebar
  --sidebar-width: 220px;
  --sidebar-collapse-width: 64px;
  --sidebar-bg: var(--card-bg);
  --sidebar-border-color: var(--border-color);
  --sidebar-item-hover-bg: rgba(0, 0, 0, 0.04);
  --sidebar-item-active-bg: rgba(59, 130, 246, 0.1);
  --sidebar-item-active-color: var(--primary-color);
}

html[data-theme='dark'] {
  --sidebar-bg: var(--card-bg);
  --sidebar-border-color: var(--border-color);
  --sidebar-item-hover-bg: rgba(255, 255, 255, 0.08);
  --sidebar-item-active-bg: rgba(59, 130, 246, 0.2);
  --sidebar-item-active-color: var(--primary-color);
}
```

### 尺寸

- 展开宽度：`220px`
- 折叠宽度：`64px`
- 过渡动画：`0.3s ease`

### 响应式设计

| 断点 | 行为 |
|------|------|
| 桌面端 (>768px) | 侧边栏固定在左侧，支持折叠 |
| 移动端 (≤768px) | 隐藏固定侧边栏，显示汉堡菜单，点击打开 Drawer |

```scss
@media (max-width: 768px) {
  .sidebar {
    display: none; // 移动端隐藏固定侧边栏
  }

  .main-content {
    margin-left: 0 !important; // 移动端无左边距
  }
}
```

## 国际化

### i18n 策略

菜单名称使用动态翻译：

```typescript
// 使用 menuEnName 作为 key，menuName 作为回退
const menuTitle = computed(() => {
  return t(`menu.${item.menuEnName}`, item.menuName)
})
```

### 添加菜单翻译键

需要在 locales 文件中添加：

```typescript
// zh-cn.ts
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
}

// en-us.ts
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
}
```

## 实现步骤

1. 添加侧边栏 CSS 变量到 `variables.scss`
2. 创建 `src/styles/sidebar.scss` 侧边栏样式文件
3. 创建 `src/layouts/components/Sidebar/index.vue` 侧边栏主组件
4. 创建 `src/layouts/components/Sidebar/SidebarItem.vue` 菜单项组件
5. 修改 `MainLayout.vue` 集成侧边栏（桌面端固定侧边栏 + 移动端 Drawer）
6. 在登录成功后调用 `userStore.fetchMenus()`
7. 添加菜单 i18n 翻译键

## 验收标准

- [ ] 侧边栏正常显示从后端获取的菜单
- [ ] 折叠/展开功能正常，状态由 appStore 管理
- [ ] 菜单导航正确跳转，当前路由高亮
- [ ] 支持暗黑模式，样式正确切换
- [ ] 移动端显示汉堡菜单，点击打开 Drawer
- [ ] 多级菜单正常展开/收起
- [ ] 菜单名称支持 i18n