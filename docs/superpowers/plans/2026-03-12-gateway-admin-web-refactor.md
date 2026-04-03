# Gateway Admin Web Refactoring Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the gateway-admin-web frontend to modern UI with dual theme support, top navigation, authentication flow, and enhanced monitoring features while preserving all existing functionality.

**Architecture:** Vue 3 Composition API with Pinia state management. Replace sidebar layout with horizontal top navigation. Add authentication layer with mock login API. Integrate ECharts for dashboard visualizations. Use CSS variables for dual theme (light/dark) system.

**Tech Stack:** Vue 3.4+, TypeScript, Pinia 2.1+, Vue Router 4.3+, Element Plus 2.6+, Vue I18n 9.10+, Axios 1.6+, ECharts 5.x, @vueuse/core, Sass 1.71+

---

## File Structure

### New Files to Create

```
gateway-admin-web/src/
├── api/auth.ts                    # Authentication API (mock)
├── components/
│   ├── Breadcrumb/index.vue       # Breadcrumb navigation
│   ├── GlobalSearch/index.vue     # Global search modal (Ctrl+K)
│   ├── LoadingSkeleton/index.vue  # Shimmer loading skeleton
│   ├── NotificationCenter/index.vue # Notification dropdown
│   ├── OfflineIndicator/index.vue # Offline banner
│   └── StatusBanner/index.vue     # System status banner
├── composables/
│   ├── useFullscreen.ts           # Fullscreen toggle
│   ├── useOffline.ts              # Offline detection
│   └── useSearch.ts               # Global search
├── layouts/MainLayout.vue         # New top navigation layout
├── stores/
│   ├── user.ts                    # User authentication state
│   └── notification.ts            # Notification state
└── views/
    ├── login/index.vue            # Login page (split screen)
    └── error/
        ├── 404.vue                # 404 error page
        └── 500.vue                # 500 error page
```

### Files to Modify

```
gateway-admin-web/src/
├── router/index.ts                # Add auth guards, login/error routes
├── stores/app.ts                  # Add fullscreen state
├── styles/variables.scss          # Add new CSS variables
├── styles/index.scss              # Add new layout styles
├── locales/zh-cn.ts               # Add new i18n keys
├── locales/en-us.ts               # Add new i18n keys
├── utils/request.ts               # Add offline detection, 401 handling
├── views/dashboard/index.vue      # Add charts, metrics, quick actions
├── views/channel/index.vue        # Update UI, add loading skeletons
├── views/route/index.vue          # Update UI, add loading skeletons
├── views/config/index.vue         # Update UI, add loading skeletons
├── views/monitor/index.vue        # Update UI, add loading skeletons
└── views/layout/index.vue         # DELETE (replaced by MainLayout.vue)
```

---

## Chunk 1: Theme System + Layout Foundation

### Task 1: Install New Dependencies

**Files:**
- Modify: `blink-gateway/gateway-admin-web/package.json`

- [ ] **Step 1: Add ECharts and VueUse dependencies to package.json**

Add to dependencies:
```json
"@vueuse/core": "^10.9.0",
"echarts": "^5.5.0",
"vue-echarts": "^6.6.9"
```

- [ ] **Step 2: Install dependencies**

Run: `cd blink-gateway/gateway-admin-web && npm install`
Expected: Dependencies installed successfully

- [ ] **Step 3: Commit**

```bash
git add package.json package-lock.json
git commit -m "feat(gateway-admin-web): add echarts and vueuse dependencies"
```

---

### Task 2: Update CSS Variables

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/styles/variables.scss`

- [ ] **Step 1: Replace variables.scss with enhanced CSS variables**

Key changes:
- Update primary color to `#3b82f6`
- Add `--bg-color-page`, `--text-color-primary/regular/secondary`
- Add `--nav-bg`, `--nav-height`, `--nav-item-color`, `--nav-item-active-color`
- Add shadow variables: `--shadow-sm/md/lg`
- Keep existing dark theme overrides

- [ ] **Step 2: Commit**

```bash
git add src/styles/variables.scss
git commit -m "feat(gateway-admin-web): update CSS variables for top navigation layout"
```

---

### Task 3: Update Global Styles

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/styles/index.scss`

- [ ] **Step 1: Replace index.scss with new layout styles**

Key additions:
- `.main-layout` - flex column container
- `.top-nav` - fixed navigation bar
- `.nav-logo`, `.nav-menu`, `.nav-item` - navigation elements
- `.main-content` - content area with top margin
- Responsive styles for mobile (hamburger menu)
- `.offline-banner` - offline indicator styles

- [ ] **Step 2: Commit**

```bash
git add src/styles/index.scss
git commit -m "feat(gateway-admin-web): add top navigation layout styles"
```

---

### Task 4: Create MainLayout Component

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/layouts/MainLayout.vue`

- [ ] **Step 1: Create MainLayout.vue with top navigation**

Template structure:
```vue
<template>
  <div class="main-layout">
    <!-- Top Navigation -->
    <header class="top-nav">
      <div class="top-nav-left">
        <div class="nav-logo">
          <div class="logo-icon"><el-icon><Guide /></el-icon></div>
          <span>Gateway Admin</span>
        </div>
        <nav class="nav-menu">
          <router-link v-for="item in navItems" :to="item.path" class="nav-item">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ t(item.title) }}</span>
          </router-link>
        </nav>
      </div>
      <div class="top-nav-right">
        <GlobalSearch />
        <NotificationCenter />
        <el-button class="nav-action" @click="toggleFullscreen">
          <el-icon><FullScreen /></el-icon>
        </el-button>
        <ThemeToggle />
        <LanguageSwitch />
        <UserDropdown />
      </div>
    </header>
    <!-- Main Content -->
    <main class="main-content">
      <Breadcrumb />
      <OfflineIndicator />
      <router-view />
    </main>
  </div>
</template>
```

Script setup:
- Import stores (app, theme, user)
- Define navItems array with path, title, icon
- Use `useFullscreen` from @vueuse/core
- Handle mobile menu toggle

- [ ] **Step 2: Commit**

```bash
git add src/layouts/MainLayout.vue
git commit -m "feat(gateway-admin-web): create MainLayout with top navigation"
```

---

### Task 5: Update Router Configuration

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/router/index.ts`

- [ ] **Step 1: Update routes to use MainLayout**

Changes:
- Replace `@/views/layout/index.vue` with `@/layouts/MainLayout.vue`
- Add `/login` route with `meta: { public: true }`
- Add `/404` and `/500` routes
- Add catch-all redirect to 404

```typescript
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true, title: 'login.title' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: 'dashboard.title' } },
      { path: 'channel', name: 'Channel', component: () => import('@/views/channel/index.vue'), meta: { title: 'channel.title' } },
      { path: 'route', name: 'Route', component: () => import('@/views/route/index.vue'), meta: { title: 'route.title' } },
      { path: 'config', name: 'Config', component: () => import('@/views/config/index.vue'), meta: { title: 'config.title' } },
      { path: 'monitor', name: 'Monitor', component: () => import('@/views/monitor/index.vue'), meta: { title: 'monitor.title' } }
    ]
  },
  { path: '/404', name: 'NotFound', component: () => import('@/views/error/404.vue'), meta: { public: true } },
  { path: '/500', name: 'ServerError', component: () => import('@/views/error/500.vue'), meta: { public: true } },
  { path: '/:pathMatch(.*)*', redirect: '/404' }
]
```

- [ ] **Step 2: Commit**

```bash
git add src/router/index.ts
git commit -m "feat(gateway-admin-web): update router with auth routes and MainLayout"
```

---

### Task 6: Delete Old Layout

**Files:**
- Delete: `blink-gateway/gateway-admin-web/src/views/layout/index.vue`

- [ ] **Step 1: Remove old sidebar layout**

```bash
rm blink-gateway/gateway-admin-web/src/views/layout/index.vue
rmdir blink-gateway/gateway-admin-web/src/views/layout
```

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "refactor(gateway-admin-web): remove old sidebar layout"
```

---

## Chunk 2: Authentication + Login Page

### Task 7: Create User Store

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/stores/user.ts`

- [ ] **Step 1: Create user store with mock authentication**

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface UserInfo {
  userId: string
  username: string
  avatar?: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  const login = async (username: string, password: string, rememberMe: boolean): Promise<boolean> => {
    // Mock authentication - in production, call real API
    if (username === 'admin' && password === 'admin123') {
      const mockToken = 'mock-token-' + Date.now()
      token.value = mockToken
      userInfo.value = { userId: '1', username: 'admin', avatar: '' }

      if (rememberMe) {
        localStorage.setItem('token', mockToken)
      } else {
        sessionStorage.setItem('token', mockToken)
      }
      return true
    }
    return false
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    sessionStorage.removeItem('token')
  }

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  return { token, userInfo, isLoggedIn, login, logout, setToken }
})
```

- [ ] **Step 2: Commit**

```bash
git add src/stores/user.ts
git commit -m "feat(gateway-admin-web): create user store with mock authentication"
```

---

### Task 8: Create Auth API

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/api/auth.ts`

- [ ] **Step 1: Create auth API with mock implementation**

```typescript
import request from '@/utils/request'

export interface LoginReq {
  username: string
  password: string
  rememberMe?: boolean
}

export interface LoginRsp {
  token: string
  expiresIn: number
  userInfo: {
    userId: string
    username: string
    avatar?: string
  }
}

// Mock login - returns success for admin/admin123
export const login = async (params: LoginReq): Promise<LoginRsp> => {
  // In production: return request.post('/login', { body: params })

  // Mock implementation
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (params.username === 'admin' && params.password === 'admin123') {
        resolve({
          token: 'mock-token-' + Date.now(),
          expiresIn: 7200,
          userInfo: {
            userId: '1',
            username: 'admin',
            avatar: ''
          }
        })
      } else {
        reject(new Error('用户名或密码错误'))
      }
    }, 500)
  })
}

export const logout = async (): Promise<void> => {
  // In production: return request.post('/logout', { body: {} })
  return Promise.resolve()
}
```

- [ ] **Step 2: Commit**

```bash
git add src/api/auth.ts
git commit -m "feat(gateway-admin-web): create auth API with mock implementation"
```

---

### Task 9: Update Request Interceptor

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/utils/request.ts`

- [ ] **Step 1: Add offline detection and 401 handling**

Add to response interceptor:
- Detect network errors and set offline state
- Handle 401 by clearing token and redirecting to login
- Import and use notification store for offline alerts

- [ ] **Step 2: Commit**

```bash
git add src/utils/request.ts
git commit -m "feat(gateway-admin-web): add offline detection and 401 handling"
```

---

### Task 10: Create Login Page

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/views/login/index.vue`

- [ ] **Step 1: Create split-screen login page**

Template structure:
```vue
<template>
  <div class="login-container">
    <!-- Left Panel - Branding -->
    <div class="login-left">
      <div class="brand-content">
        <div class="logo-wrapper">
          <div class="logo-icon"><el-icon><Guide /></el-icon></div>
        </div>
        <h1 class="brand-title">Gateway Admin</h1>
        <p class="brand-subtitle">Monitor & Manage Your API Gateway</p>
      </div>
    </div>

    <!-- Right Panel - Login Form -->
    <div class="login-right">
      <div class="login-form-wrapper">
        <h2 class="form-title">{{ t('login.welcomeBack') }}</h2>
        <el-form ref="formRef" :model="loginForm" :rules="rules">
          <el-form-item prop="username">
            <el-input v-model="loginForm.username" :placeholder="t('login.username')" prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="loginForm.password" type="password" :placeholder="t('login.password')" prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="loginForm.rememberMe">{{ t('login.rememberMe') }}</el-checkbox>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="handleLogin" class="login-btn">
              {{ t('login.loginBtn') }}
            </el-button>
          </el-form-item>
        </el-form>
        <div class="login-footer">
          <ThemeToggle />
          <LanguageSwitch />
        </div>
      </div>
    </div>
  </div>
</template>
```

Styles:
- `.login-container` - flex row, full height
- `.login-left` - gradient background, centered branding
- `.login-right` - white/dark card, centered form
- Responsive: stack vertically on mobile

- [ ] **Step 2: Commit**

```bash
git add src/views/login/index.vue
git commit -m "feat(gateway-admin-web): create split-screen login page"
```

---

### Task 11: Update Router Guards

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/router/index.ts`

- [ ] **Step 1: Add authentication guard**

```typescript
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  const isPublic = to.meta.public

  if (isPublic) {
    next()
    return
  }

  if (!token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  next()
})
```

- [ ] **Step 2: Commit**

```bash
git add src/router/index.ts
git commit -m "feat(gateway-admin-web): add authentication router guard"
```

---

### Task 12: Add i18n Keys for Auth

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/locales/zh-cn.ts`
- Modify: `blink-gateway/gateway-admin-web/src/locales/en-us.ts`

- [ ] **Step 1: Add login translations to zh-cn.ts**

```typescript
login: {
  title: '登录',
  welcomeBack: '欢迎回来',
  username: '用户名',
  password: '密码',
  rememberMe: '记住我',
  loginBtn: '登录',
  loginSuccess: '登录成功',
  loginFailed: '登录失败',
  usernameRequired: '请输入用户名',
  passwordRequired: '请输入密码',
  logout: '退出登录',
  profile: '个人中心',
  settings: '设置'
}
```

- [ ] **Step 2: Add login translations to en-us.ts**

```typescript
login: {
  title: 'Login',
  welcomeBack: 'Welcome back',
  username: 'Username',
  password: 'Password',
  rememberMe: 'Remember me',
  loginBtn: 'Login',
  loginSuccess: 'Login successful',
  loginFailed: 'Login failed',
  usernameRequired: 'Please enter username',
  passwordRequired: 'Please enter password',
  logout: 'Logout',
  profile: 'Profile',
  settings: 'Settings'
}
```

- [ ] **Step 3: Commit**

```bash
git add src/locales/zh-cn.ts src/locales/en-us.ts
git commit -m "feat(gateway-admin-web): add login i18n translations"
```

---

## Chunk 3: Common Features

### Task 13: Create Breadcrumb Component

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/components/Breadcrumb/index.vue`

- [ ] **Step 1: Create breadcrumb navigation**

- Read current route from useRoute()
- Map route path to breadcrumb items
- Use el-breadcrumb component

- [ ] **Step 2: Commit**

```bash
git add src/components/Breadcrumb/index.vue
git commit -m "feat(gateway-admin-web): create breadcrumb component"
```

---

### Task 14: Create useFullscreen Composable

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/composables/useFullscreen.ts`

- [ ] **Step 1: Create fullscreen composable using VueUse**

```typescript
import { useFullscreen as useVueUseFullscreen } from '@vueuse/core'

export const useFullscreen = () => {
  const { isFullscreen, toggle } = useVueUseFullscreen()

  return {
    isFullscreen,
    toggleFullscreen: toggle
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/composables/useFullscreen.ts
git commit -m "feat(gateway-admin-web): create useFullscreen composable"
```

---

### Task 15: Create useOffline Composable

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/composables/useOffline.ts`

- [ ] **Step 1: Create offline detection composable**

```typescript
import { ref } from 'vue'

const isOffline = ref(false)

export const useOffline = () => {
  const setOffline = (value: boolean) => {
    isOffline.value = value
  }

  return {
    isOffline,
    setOffline
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/composables/useOffline.ts
git commit -m "feat(gateway-admin-web): create useOffline composable"
```

---

### Task 16: Create OfflineIndicator Component

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/components/OfflineIndicator/index.vue`

- [ ] **Step 1: Create offline banner component**

- Use useOffline composable
- Show banner when isOffline is true
- Red background with "Connection lost - Retrying..." message

- [ ] **Step 2: Commit**

```bash
git add src/components/OfflineIndicator/index.vue
git commit -m "feat(gateway-admin-web): create offline indicator component"
```

---

### Task 17: Create GlobalSearch Component

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/components/GlobalSearch/index.vue`
- Create: `blink-gateway/gateway-admin-web/src/composables/useSearch.ts`

- [ ] **Step 1: Create useSearch composable**

- Search across loaded channels, routes, configs by name/ID
- Return filtered results grouped by type

- [ ] **Step 2: Create GlobalSearch modal component**

- Trigger with Ctrl+K keyboard shortcut
- Search input with results dropdown
- Click result to navigate to detail page

- [ ] **Step 3: Commit**

```bash
git add src/components/GlobalSearch/index.vue src/composables/useSearch.ts
git commit -m "feat(gateway-admin-web): create global search component"
```

---

### Task 18: Create Notification Store

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/stores/notification.ts`

- [ ] **Step 1: Create notification store**

```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface NotificationItem {
  id: string
  title: string
  message: string
  type: 'info' | 'warning' | 'error' | 'success'
  time: Date
  read: boolean
}

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<NotificationItem[]>([])

  const addNotification = (notification: Omit<NotificationItem, 'id' | 'time' | 'read'>) => {
    notifications.value.unshift({
      ...notification,
      id: Date.now().toString(),
      time: new Date(),
      read: false
    })
  }

  const markAsRead = (id: string) => {
    const item = notifications.value.find(n => n.id === id)
    if (item) item.read = true
  }

  const markAllAsRead = () => {
    notifications.value.forEach(n => n.read = true)
  }

  const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

  return { notifications, addNotification, markAsRead, markAllAsRead, unreadCount }
})
```

- [ ] **Step 2: Commit**

```bash
git add src/stores/notification.ts
git commit -m "feat(gateway-admin-web): create notification store"
```

---

### Task 19: Create NotificationCenter Component

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/components/NotificationCenter/index.vue`

- [ ] **Step 1: Create notification dropdown**

- Bell icon with unread badge
- Dropdown with notification list (simplified: show recent activity from `/monitor/getStatistics`)
- Mark as read functionality

- [ ] **Step 2: Commit**

```bash
git add src/components/NotificationCenter/index.vue
git commit -m "feat(gateway-admin-web): create notification center component"
```

---

### Task 20: Create LoadingSkeleton Component

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/components/LoadingSkeleton/index.vue`

- [ ] **Step 1: Create shimmer loading skeleton**

Props: `type: 'card' | 'table' | 'list' | 'text'`, `rows: number`

- Card: shimmer rectangles
- Table: shimmer rows with columns
- List: shimmer items
- Text: shimmer lines

- [ ] **Step 2: Commit**

```bash
git add src/components/LoadingSkeleton/index.vue
git commit -m "feat(gateway-admin-web): create loading skeleton component"
```

---

### Task 21: Create StatusBanner Component

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/components/StatusBanner/index.vue`

- [ ] **Step 1: Create system status banner**

- Compute status from `/monitor/getStatistics` healthy instances
- Show: "All Systems Operational" (green) / "Degraded" (yellow) / "Down" (red)

- [ ] **Step 2: Commit**

```bash
git add src/components/StatusBanner/index.vue
git commit -m "feat(gateway-admin-web): create status banner component"
```

---

### Task 22: Create Error Pages

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/views/error/404.vue`
- Create: `blink-gateway/gateway-admin-web/src/views/error/500.vue`

- [ ] **Step 1: Create 404 page**

- Friendly "Page Not Found" message
- Illustration/icon
- Link back to dashboard

- [ ] **Step 2: Create 500 page**

- "Server Error" message
- Retry button
- Link back to dashboard

- [ ] **Step 3: Commit**

```bash
git add src/views/error/404.vue src/views/error/500.vue
git commit -m "feat(gateway-admin-web): create error pages (404, 500)"
```

---

## Chunk 4: Dashboard Enhancement

### Task 23: Create Metrics Cards with Auto-Refresh

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/views/dashboard/index.vue`

- [ ] **Step 1: Add real-time metrics cards**

Metrics to display:
- Total Requests (from `getStatistics.totalRequests`)
- Success Rate (calculated from successRequests/totalRequests)
- Avg Response Time (from `getStatistics.avgResponseTime`)
- Healthy Instances (from `getStatistics.healthyInstances/totalInstances`)

Auto-refresh every 5 seconds using setInterval

- [ ] **Step 2: Commit**

```bash
git add src/views/dashboard/index.vue
git commit -m "feat(gateway-admin-web): add real-time metrics cards to dashboard"
```

---

### Task 24: Add ECharts Integration

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/views/dashboard/index.vue`

- [ ] **Step 1: Add traffic trend line chart**

- Use vue-echarts component
- Store historical data in reactive array (last 10 data points)
- X-axis: time, Y-axis: request count

- [ ] **Step 2: Add status distribution pie chart**

- Pie chart showing healthy vs unhealthy instances
- Use data from `getGatewayInstances`

- [ ] **Step 3: Commit**

```bash
git add src/views/dashboard/index.vue
git commit -m "feat(gateway-admin-web): add charts to dashboard"
```

---

### Task 25: Add Instance Health Grid

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/views/dashboard/index.vue`

- [ ] **Step 1: Create instance health grid**

- Grid of instance cards showing:
  - Instance ID
  - IP:Port
  - Health status (green/red indicator)
  - Weight
- Click to navigate to monitor page

- [ ] **Step 2: Commit**

```bash
git add src/views/dashboard/index.vue
git commit -m "feat(gateway-admin-web): add instance health grid to dashboard"
```

---

### Task 26: Add Quick Action Buttons

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/views/dashboard/index.vue`

- [ ] **Step 1: Add quick action buttons**

Actions:
- Refresh Routes (call existing API)
- Sync Config (call existing API)
- Clear Cache (call existing API if available)

- [ ] **Step 2: Commit**

```bash
git add src/views/dashboard/index.vue
git commit -m "feat(gateway-admin-web): add quick action buttons to dashboard"
```

---

### Task 27: Add Activity Log Placeholder

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/views/dashboard/index.vue`

- [ ] **Step 1: Add activity log section (UI only)**

- Show placeholder "No recent activity" message
- Note: Backend API `/monitor/getActivityLog` not yet implemented
- Prepare UI for future integration

- [ ] **Step 2: Commit**

```bash
git add src/views/dashboard/index.vue
git commit -m "feat(gateway-admin-web): add activity log placeholder to dashboard"
```

---

## Chunk 5: Page Refactoring

### Task 28: Refactor Channel Page

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/views/channel/index.vue`

- [ ] **Step 1: Update to use new layout styles**

- Replace old sidebar-dependent styles
- Use new CSS variables
- Add loading skeleton while data loads

- [ ] **Step 2: Preserve existing functionality**

- Channel list with pagination
- Search by channelId, channelName, status
- Add/Edit channel dialog
- Toggle encryption switch inline
- Operations dropdown: refreshChannelKey, refreshSystemKey, issueToken, delete
- Issue token result dialog

- [ ] **Step 3: Commit**

```bash
git add src/views/channel/index.vue
git commit -m "refactor(gateway-admin-web): update channel page UI"
```

---

### Task 29: Refactor Route Page

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/views/route/index.vue`

- [ ] **Step 1: Update to use new layout styles**

- Replace old styles
- Use new CSS variables
- Add loading skeleton

- [ ] **Step 2: Preserve existing functionality**

- Route list with pagination
- Search by routeId, routeName
- Add/Edit route dialog with predicates/filters
- Delete route
- Sync status indicator

- [ ] **Step 3: Commit**

```bash
git add src/views/route/index.vue
git commit -m "refactor(gateway-admin-web): update route page UI"
```

---

### Task 30: Refactor Config Page

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/views/config/index.vue`

- [ ] **Step 1: Update to use new layout styles**

- Replace old styles
- Use new CSS variables
- Add loading skeleton

- [ ] **Step 2: Preserve existing functionality**

- Gateway config form (toggles for signature, replay, encryption, rate limit)
- IP whitelist/blacklist management
- Save config

- [ ] **Step 3: Commit**

```bash
git add src/views/config/index.vue
git commit -m "refactor(gateway-admin-web): update config page UI"
```

---

### Task 31: Refactor Monitor Page

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/views/monitor/index.vue`

- [ ] **Step 1: Update to use new layout styles**

- Replace old styles
- Use new CSS variables
- Add loading skeleton

- [ ] **Step 2: Preserve existing functionality**

- Instance list with health status
- Instance details view
- Metrics display

- [ ] **Step 3: Commit**

```bash
git add src/views/monitor/index.vue
git commit -m "refactor(gateway-admin-web): update monitor page UI"
```

---

### Task 32: Add Common i18n Keys

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/locales/zh-cn.ts`
- Modify: `blink-gateway/gateway-admin-web/src/locales/en-us.ts`

- [ ] **Step 1: Add common feature translations**

Keys to add:
- `common.fullscreen`, `common.exitFullscreen`
- `common.search`, `common.globalSearch`
- `common.notifications`, `common.noNotifications`
- `common.offline`, `common.reconnecting`
- `common.allSystemsOperational`, `common.degraded`, `common.systemDown`
- `error.404`, `error.404Message`, `error.500`, `error.500Message`, `error.backToDashboard`

- [ ] **Step 2: Commit**

```bash
git add src/locales/zh-cn.ts src/locales/en-us.ts
git commit -m "feat(gateway-admin-web): add common feature i18n keys"
```

---

### Task 33: Final Testing and Build

**Files:**
- All modified files

- [ ] **Step 1: Run development server**

Run: `cd blink-gateway/gateway-admin-web && npm run dev`
Expected: Dev server starts without errors

- [ ] **Step 2: Test all pages manually**

- Login page: test login with admin/admin123
- Dashboard: verify metrics, charts, instance grid
- Channel: verify list, search, add/edit, operations
- Route: verify list, search, add/edit, delete
- Config: verify form, IP lists, save
- Monitor: verify instance list, details
- Theme toggle: verify light/dark switching
- Language switch: verify zh-cn/en-us
- 404 page: navigate to invalid route

- [ ] **Step 3: Run production build**

Run: `cd blink-gateway/gateway-admin-web && npm run build`
Expected: Build succeeds without warnings

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "feat(gateway-admin-web): complete refactoring with all features"
```

---

## Success Criteria

- [ ] All existing functionality works
- [ ] Login page with mock authentication
- [ ] Light/dark theme switching
- [ ] Responsive design (mobile-friendly)
- [ ] All 8 common features implemented
- [ ] All 6 dashboard features implemented
- [ ] Error pages (404, 500) working
- [ ] i18n (Chinese/English) complete
- [ ] No console errors
- [ ] Build succeeds without warnings