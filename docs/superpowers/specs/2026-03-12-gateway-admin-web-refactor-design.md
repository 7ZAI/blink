# Gateway Admin Web Refactoring Design Specification

**Date:** 2026-03-12
**Author:** Claude
**Status:** Draft

## Overview

Refactor `blink-gateway/gateway-admin-web` frontend project to modern UI standards with a clean, progressive design suitable for a monitoring management system. The refactoring preserves all existing functionality while adding login page and common frontend features.

## Design Decisions

### 1. Visual Style: Dual Theme System

**Choice:** Full light/dark theme support with smooth CSS transitions.

**Rationale:**
- Follows existing `blink-base-web` patterns for consistency across the Blink ecosystem
- Users can switch based on preference or environment
- CSS variables enable easy theming throughout the application

**Implementation:**
- CSS custom properties for all colors
- `data-theme="light"` or `data-theme="dark"` on `<html>` element
- Theme persisted in `localStorage`
- Instant switch without page reload

### 2. Layout Structure: Top Navigation

**Choice:** Horizontal navigation bar at the top of the page.

**Rationale:**
- More vertical space for monitoring dashboards and data tables
- Clean, simple interface suitable for the limited menu items (5 pages)
- Responsive design with hamburger menu on mobile
- Modern web application pattern

**Menu Items:**
- Dashboard
- Channel Management
- Route Management
- Config Management
- Monitor Center

### 3. Login Page: Split Screen

**Choice:** Two-column layout with branded panel on left, login form on right.

**Rationale:**
- Modern, visually impactful design
- Branded left panel can show system name, description, and branding
- Form-focused right panel for clean user input
- Responsive: stacks vertically on mobile

**Components:**
- System logo and title
- Username input field
- Password input field
- Remember me checkbox
- Login button
- Theme toggle
- Language switch

## Features

### Common Features

| Feature | Description | Implementation Notes |
|---------|-------------|---------------------|
| Fullscreen Toggle | Button to enter/exit fullscreen mode for better focus | Uses `@vueuse/core` useFullscreen |
| Breadcrumb Navigation | Show current page location in hierarchy | Static breadcrumbs based on route |
| Notification Center | Bell icon with dropdown for system alerts | **Simplified:** Shows recent activity log entries from `/monitor/getStatistics` |
| Global Search | Quick search with Ctrl+K keyboard shortcut | **In-memory search:** Searches loaded channels, routes, configs by name/ID |
| User Profile Dropdown | Avatar with profile, settings, logout options | Settings links to theme/language toggles |
| Loading Skeletons | Shimmer loading states instead of spinners | Custom Vue component with CSS animation |
| Error Pages | Custom 404 and 500 pages with navigation | Static pages with illustration |
| Offline Indicator | Banner when backend connection is lost | Axios interceptor-based detection |

### Dashboard Features

| Feature | Description | Data Source |
|---------|-------------|-------------|
| Real-time Metrics Cards | Live stats with auto-refresh: requests, success rate, latency | `/monitor/getStatistics` |
| Charts & Visualizations | Line, bar, pie charts for traffic and performance data | Aggregated from `/monitor/getGatewayMetrics` |
| Instance Health Grid | Visual grid showing all gateway instances with status | `/monitor/getGatewayInstances` |
| Recent Activity Log | Live feed of events: config changes, errors, deployments | **Placeholder** - UI only, no backend yet |
| Quick Action Buttons | Shortcuts: Refresh routes, Sync config, Clear cache | Calls existing APIs |
| System Status Banner | Top banner showing overall health status | Computed from instance health |

## Pages

### New Pages

1. **Login Page** (`/login`)
   - Split screen layout
   - Username/password authentication
   - Remember me option
   - Theme and language switchers

2. **404 Error Page** (`/:pathMatch(.*)*`)
   - Friendly not-found message
   - Link back to dashboard

3. **500 Error Page**
   - Server error message
   - Retry button
   - Link back to dashboard

### Refactored Pages

1. **Dashboard** (`/dashboard`)
   - Real-time metrics cards (requests, success rate, latency, active instances)
   - Charts section (traffic trends, status distribution)
   - Instance health grid with status indicators
   - Quick actions (refresh routes, sync config)
   - Activity log placeholder

2. **Channel Management** (`/channel`)
   - **Existing functionality to preserve:**
     - Channel list with pagination
     - Search by channelId, channelName, status
     - Add/Edit channel dialog
     - Toggle encryption switch inline
     - Operations dropdown: refreshChannelKey, refreshSystemKey, issueToken, delete
     - Issue token result dialog
   - **Updates:** Loading skeletons, updated UI components

3. **Route Management** (`/route`)
   - **Existing functionality to preserve:**
     - Route list with pagination
     - Search by routeId, routeName
     - Add/Edit route dialog with predicates/filters configuration
     - Delete route
     - Sync status indicator
   - **Updates:** Loading skeletons, updated UI components

4. **Config Management** (`/config`)
   - **Existing functionality to preserve:**
     - Gateway config form (signature, replay, encryption, rate limit toggles)
     - IP whitelist/blacklist management
     - Save config
   - **Updates:** Loading skeletons, updated UI components

5. **Monitor Center** (`/monitor`)
   - **Existing functionality to preserve:**
     - Instance list with health status
     - Instance details view
     - Metrics display
   - **Updates:** Loading skeletons, updated UI components

## Architecture

### Directory Structure

```
gateway-admin-web/
├── src/
│   ├── api/                    # API layer
│   │   ├── auth.ts             # Authentication API (new)
│   │   ├── channel.ts          # Channel API (existing)
│   │   ├── config.ts           # Config API (existing)
│   │   ├── monitor.ts          # Monitor API (existing)
│   │   └── route.ts            # Route API (existing)
│   ├── components/             # Shared components (new)
│   │   ├── Breadcrumb/
│   │   ├── GlobalSearch/
│   │   ├── LoadingSkeleton/
│   │   ├── NotificationCenter/
│   │   ├── OfflineIndicator/
│   │   └── StatusBanner/
│   ├── composables/            # Vue composables (new)
│   │   ├── useFullscreen.ts
│   │   ├── useOffline.ts
│   │   └── useSearch.ts
│   ├── layouts/                # Layout components (new)
│   │   └── MainLayout.vue
│   ├── locales/                # i18n files
│   │   ├── index.ts
│   │   ├── zh-cn.ts
│   │   └── en-us.ts
│   ├── router/                 # Vue Router
│   │   └── index.ts
│   ├── stores/                 # Pinia stores
│   │   ├── app.ts
│   │   ├── theme.ts
│   │   ├── user.ts             # (new)
│   │   └── notification.ts     # (new)
│   ├── styles/                 # Global styles
│   │   ├── index.scss
│   │   └── variables.scss
│   ├── types/                  # TypeScript types
│   │   └── index.ts
│   ├── views/                  # Page components
│   │   ├── login/              # (new)
│   │   ├── error/              # (new)
│   │   ├── dashboard/
│   │   ├── channel/
│   │   ├── route/
│   │   ├── config/
│   │   └── monitor/
│   ├── App.vue
│   └── main.ts
├── public/
├── index.html
├── package.json
├── vite.config.ts
└── tsconfig.json
```

### Component Design

#### MainLayout.vue
```
┌─────────────────────────────────────────────────────────┐
│ Logo  │ Dashboard │ Channel │ Route │ Config │ Monitor │ User │
├─────────────────────────────────────────────────────────┤
│                                                         │
│                    Page Content                         │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### LoginPage.vue
```
┌──────────────────┬──────────────────────────┐
│                  │                          │
│    Gateway       │   Welcome back           │
│    Admin         │   ┌──────────────────┐   │
│                  │   │ Username         │   │
│    Monitor &     │   ├──────────────────┤   │
│    Manage Your   │   │ Password         │   │
│    API Gateway   │   ├──────────────────┤   │
│                  │   │ [Login Button]   │   │
│                  │   └──────────────────┘   │
└──────────────────┴──────────────────────────┘
```

### State Management

**Pinia Stores:**

1. **app.ts** - Application state (sidebar, language)
2. **theme.ts** - Theme state (light/dark)
3. **user.ts** - User authentication state (new)
4. **notification.ts** - Notification state (new)

### API Layer

All APIs follow the project's POST-only convention:

```typescript
// Example: auth.ts
export const login = (params: LoginParams): Promise<LoginResult> => {
  return request.post('/login', { body: params })
}
```

### Routing

```typescript
const routes = [
  { path: '/login', component: Login, meta: { public: true } },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: Dashboard },
      { path: 'channel', component: Channel },
      { path: 'route', component: Route },
      { path: 'config', component: Config },
      { path: 'monitor', component: Monitor }
    ]
  },
  { path: '/404', component: NotFound },
  { path: '/500', component: ServerError },
  { path: '/:pathMatch(.*)*', redirect: '/404' }
]
```

### Authentication Flow

**Scope Note:** Authentication requires backend `/login` API implementation. For this frontend refactoring iteration:
- Frontend login page and auth flow will be fully implemented
- Backend login API integration will use a **mock** for development/testing
- Real backend authentication integration is a **separate task** after this frontend work

**Happy Path:**
1. User navigates to protected route
2. Router guard checks for token in localStorage
3. If no token, redirect to `/login`
4. User enters credentials
5. On success, store token and redirect to dashboard
6. Token included in all API requests via axios interceptor

**Error Handling:**
- **Login Failure:** Display error message from server (e.g., "Invalid credentials"), allow retry
- **Network Error:** Show "Unable to connect to server" message with retry button
- **Token Expiration:** Axios interceptor catches 401 response, clear token, redirect to login
- **Logout:** Clear token from localStorage, reset user store, redirect to login page

**Login API:**
```typescript
// Request
interface LoginReq {
  username: string
  password: string
  rememberMe?: boolean
}

// Response
interface LoginRsp {
  token: string
  expiresIn: number
  userInfo: {
    userId: string
    username: string
    avatar?: string
  }
}
```

### Dashboard Data Sources

**Using Existing Backend APIs:**

| Feature | API Endpoint | Refresh Method | Interval |
|---------|--------------|----------------|----------|
| Real-time Metrics | `/monitor/getStatistics` | Polling | 5 seconds |
| Instance Health | `/monitor/getGatewayInstances` | Polling | 10 seconds |
| Gateway Metrics | `/monitor/getGatewayMetrics` | Polling | 10 seconds |

**Requires New Backend Implementation:**

| Feature | Proposed Endpoint | Notes |
|---------|-------------------|-------|
| Activity Log | `/monitor/getActivityLog` | **Out of scope** for this iteration - placeholder in UI |
| System Status Banner | Computed from `/monitor/getStatistics` | Frontend calculates from healthy/total instances |

**Charts Data:** Charts will visualize data from existing endpoints:
- Traffic trends: Aggregated from `/monitor/getGatewayMetrics` history (stored in frontend state)
- Status distribution: Computed from instance health data
- No new backend API required for basic charts

**Offline Detection:**
- Uses axios response interceptor to detect network failures
- Shows banner when any API call fails with network error
- Auto-hides banner when API calls succeed again
- Visual indicator: Red banner at top of page with "Connection lost - Retrying..."

## Styling Guidelines

### CSS Variables

```scss
:root {
  // Colors
  --primary-color: #3b82f6;
  --success-color: #10b981;
  --warning-color: #f59e0b;
  --danger-color: #ef4444;

  // Light theme
  --bg-color: #f8fafc;
  --card-bg: #ffffff;
  --text-color: #1e293b;
  --text-color-secondary: #64748b;
  --border-color: #e2e8f0;
}

html[data-theme='dark'] {
  --bg-color: #0f172a;
  --card-bg: #1e293b;
  --text-color: #f1f5f9;
  --text-color-secondary: #94a3b8;
  --border-color: #334155;
}
```

### Animation

- Theme transition: `transition: background-color 0.3s, color 0.3s`
- Page enter: fade + translateY
- Table row enter: staggered fade

## Dependencies

### Existing
- Vue 3.4+
- Vue Router 4.3+
- Pinia 2.1+
- Element Plus 2.6+
- Vue I18n 9.10+
- Axios 1.6+
- Sass 1.71+

### New
- **ECharts** - For charts and visualizations
- **@vueuse/core** - For composables (fullscreen, etc.)

## Code Standards

Follow `CLAUDE.md` guidelines:
- POST-only API requests
- RequestDTO/ResponseDTO wrappers
- i18n for all user-visible text
- CSS variables for theming
- Component naming: PascalCase
- File naming: kebab-case for views, PascalCase for components

## Migration Strategy

### Implementation Units

**Unit 1: Theme System + Layout Foundation**
- CSS variables and theme switching
- MainLayout component with top navigation
- Responsive design structure

**Unit 2: Authentication + Login Page**
- User store with mock authentication
- Login page with split-screen design
- Router guards and auth flow

**Unit 3: Common Features**
- Fullscreen toggle, breadcrumbs
- Notification center (simplified)
- Global search (in-memory)
- Loading skeletons
- Offline indicator
- Error pages

**Unit 4: Dashboard Enhancement**
- Metrics cards with auto-refresh
- Charts integration (ECharts)
- Instance health grid
- Quick actions

**Unit 5: Page Refactoring**
- Channel page UI update
- Route page UI update
- Config page UI update
- Monitor page UI update

### Execution Order

1. Create new layout and theme system
2. Add authentication store and login page
3. Update router with auth guards
4. Refactor each page incrementally
5. Add new features (notifications, search, etc.)
6. Add dashboard charts and visualizations
7. Add error pages
8. Test all functionality

## Success Criteria

- [ ] All existing functionality works
- [ ] Login page with authentication
- [ ] Light/dark theme switching
- [ ] Responsive design (mobile-friendly)
- [ ] All 8 common features implemented
- [ ] All 6 dashboard features implemented
- [ ] Error pages (404, 500) working
- [ ] i18n (Chinese/English) complete
- [ ] No console errors
- [ ] Build succeeds without warnings