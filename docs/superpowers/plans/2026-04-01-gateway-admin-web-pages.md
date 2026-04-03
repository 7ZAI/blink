# gateway-admin-web 页面实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 gateway-admin-web 新增权限管理、操作日志、设置和字典管理页面

**Architecture:** 直接复制 blink-base-web 的实现，保持代码和后端API完全一致

**Tech Stack:** Vue 3 + TypeScript + Element Plus + Pinia

---

## 文件结构

```
blink-gateway/gateway-admin-web/src/
├── api/
│   ├── permission.ts        # 新增
│   ├── operation-log.ts     # 新增
│   └── dict.ts              # 新增
├── composables/
│   ├── usePermission.ts     # 新增
│   └── useDataTransition.ts # 新增
├── components/
│   └── AuthButton.vue       # 新增
├── stores/
│   ├── theme.ts             # 更新：替换为blink-base-web版本
│   └── user.ts              # 更新：添加isSuperAdmin
├── views/
│   ├── settings/
│   │   ├── index.vue
│   │   └── components/
│   │       └── ThemeEditor.vue
│   └── system/
│       ├── permission/
│       │   ├── index.vue
│       │   └── components/
│       │       ├── PermissionFormDialog.vue
│       │       └── PermissionSelectDialog.vue
│       ├── operation-log/
│       │   ├── index.vue
│       │   └── components/
│       │       └── OperationLogDetailDialog.vue
│       └── dict/
│           ├── type/
│           │   ├── index.vue
│           │   └── components/
│           │       └── DictTypeFormDialog.vue
│           └── data/
│               ├── index.vue
│               └── components/
│                   └── DictDataFormDialog.vue
├── config/
│   └── themes.ts            # 新增
├── locales/
│   ├── zh-cn.ts             # 更新
│   └── en-us.ts             # 更新
└── router/
    └── index.ts             # 更新
```

---

### Task 1: 添加 isSuperAdmin 到 user store

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/stores/user.ts`

- [ ] **Step 1: 添加 isSuperAdmin 计算属性**

在 `useUserStore` 中添加 `isSuperAdmin` 计算属性：

```typescript
// 在 menuTree 计算属性后添加
const isSuperAdmin = computed(() => {
  return roles.value.includes('superAdmin') ||
         userInfo.value?.superFlag === 1 ||
         String(userInfo.value?.superFlag) === '1'
})
```

- [ ] **Step 2: 在 return 中导出 isSuperAdmin**

```typescript
return {
  // ... 现有导出
  isSuperAdmin,
}
```

- [ ] **Step 3: 提交**

```bash
git add blink-gateway/gateway-admin-web/src/stores/user.ts
git commit -m "feat(gateway-admin-web): add isSuperAdmin to user store"
```

---

### Task 2: 复制 themes.ts 配置文件

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/config/themes.ts`

- [ ] **Step 1: 复制 themes.ts 文件**

复制 `blink-base/blink-base-web/src/config/themes.ts` 到 `blink-gateway/gateway-admin-web/src/config/themes.ts`

- [ ] **Step 2: 提交**

```bash
git add blink-gateway/gateway-admin-web/src/config/themes.ts
git commit -m "feat(gateway-admin-web): add themes config"
```

---

### Task 3: 替换 theme store

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/stores/theme.ts`

- [ ] **Step 1: 替换为 blink-base-web 的 theme store**

复制 `blink-base/blink-base-web/src/stores/theme.ts` 的完整内容替换 `blink-gateway/gateway-admin-web/src/stores/theme.ts`

- [ ] **Step 2: 提交**

```bash
git add blink-gateway/gateway-admin-web/src/stores/theme.ts
git commit -m "feat(gateway-admin-web): upgrade theme store with full theme support"
```

---

### Task 4: 添加 usePermission composable

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/composables/usePermission.ts`

- [ ] **Step 1: 复制 usePermission.ts**

复制 `blink-base/blink-base-web/src/composables/usePermission.ts` 到 `blink-gateway/gateway-admin-web/src/composables/usePermission.ts`

- [ ] **Step 2: 提交**

```bash
git add blink-gateway/gateway-admin-web/src/composables/usePermission.ts
git commit -m "feat(gateway-admin-web): add usePermission composable"
```

---

### Task 5: 添加 useDataTransition composable

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/composables/useDataTransition.ts`

- [ ] **Step 1: 复制 useDataTransition.ts**

复制 `blink-base/blink-base-web/src/composables/useDataTransition.ts` 到 `blink-gateway/gateway-admin-web/src/composables/useDataTransition.ts`

- [ ] **Step 2: 提交**

```bash
git add blink-gateway/gateway-admin-web/src/composables/useDataTransition.ts
git commit -m "feat(gateway-admin-web): add useDataTransition composable"
```

---

### Task 6: 添加 AuthButton 组件

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/components/AuthButton.vue`

- [ ] **Step 1: 复制 AuthButton.vue**

复制 `blink-base/blink-base-web/src/components/AuthButton.vue` 到 `blink-gateway/gateway-admin-web/src/components/AuthButton.vue`

- [ ] **Step 2: 提交**

```bash
git add blink-gateway/gateway-admin-web/src/components/AuthButton.vue
git commit -m "feat(gateway-admin-web): add AuthButton component"
```

---

### Task 7: 添加 permission API

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/api/permission.ts`

- [ ] **Step 1: 复制 permission.ts**

复制 `blink-base/blink-base-web/src/api/permission.ts` 到 `blink-gateway/gateway-admin-web/src/api/permission.ts`

- [ ] **Step 2: 提交**

```bash
git add blink-gateway/gateway-admin-web/src/api/permission.ts
git commit -m "feat(gateway-admin-web): add permission API"
```

---

### Task 8: 添加 operation-log API

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/api/operation-log.ts`

- [ ] **Step 1: 复制 operation-log.ts**

复制 `blink-base/blink-base-web/src/api/operation-log.ts` 到 `blink-gateway/gateway-admin-web/src/api/operation-log.ts`

- [ ] **Step 2: 提交**

```bash
git add blink-gateway/gateway-admin-web/src/api/operation-log.ts
git commit -m "feat(gateway-admin-web): add operation-log API"
```

---

### Task 9: 添加 dict API

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/api/dict.ts`

- [ ] **Step 1: 复制 dict.ts**

复制 `blink-base/blink-base-web/src/api/dict.ts` 到 `blink-gateway/gateway-admin-web/src/api/dict.ts`

- [ ] **Step 2: 提交**

```bash
git add blink-gateway/gateway-admin-web/src/api/dict.ts
git commit -m "feat(gateway-admin-web): add dict API"
```

---

### Task 10: 更新中文语言包

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/locales/zh-cn.ts`

- [ ] **Step 1: 在 common 对象后添加 message 对象**

在 `common` 对象后添加：

```typescript
  message: {
    tips: '提示',
    operationSuccess: '操作成功',
    saveSuccess: '保存成功',
    deleteSuccess: '删除成功',
    confirmDelete: '确定删除吗？',
  },
```

- [ ] **Step 2: 在 menu 对象中添加新菜单项**

更新 `menu` 对象：

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
    menu: '菜单管理',
    permission: '权限管理',
    operationLog: '操作日志',
    dict: '字典管理',
    dictType: '字典类型',
    dictData: '字典数据',
    settings: '系统设置',
  },
```

- [ ] **Step 3: 添加 system.permission 对象**

在 `system.menu` 对象后添加：

```typescript
    permission: {
      title: '权限管理',
      acName: '权限名称',
      acEnName: '权限英文名',
      acIdentity: '权限标识',
      acType: '权限类型',
      acTypeApi: '接口权限',
      acTypeData: '数据权限',
      typeApi: '接口权限',
      typeData: '数据权限',
      url: '权限地址',
      dataFilterId: '数据过滤规则',
      dataFilterRule: '选择数据过滤规则',
      createTime: '创建时间',
      createBy: '创建人',
      addPermission: '新增权限',
      editPermission: '编辑权限',
      deleteConfirm: '确定要删除该权限吗？',
      relatedMenus: '关联菜单',
    },
```

- [ ] **Step 4: 添加 system.operationLog 对象**

在 `system.permission` 对象后添加：

```typescript
    operationLog: {
      title: '操作日志',
      list: '操作日志列表',
      detailTitle: '操作日志详情',
      keywordPlaceholder: '请输入关键词搜索描述或URL',
      operator: '操作人',
      logType: '日志类型',
      executeStatus: '执行状态',
      operationTime: '操作时间',
      logId: '日志ID',
      userId: '用户ID',
      description: '操作描述',
      requestUrl: '请求URL',
      requestMethod: '请求方法',
      executeTimeMs: '执行时长',
      ipAddress: 'IP地址',
      userAgent: '浏览器UA',
      requestParams: '请求参数',
      responseData: '响应数据',
      errorMsg: '错误信息',
      LOGIN: '登入日志',
      SYSTEM: '系统日志',
      OPERATION: '操作日志',
    },
```

- [ ] **Step 5: 添加 settings 对象**

在 `system` 对象后添加：

```typescript
  settings: {
    title: '系统设置',
    basicSettings: '基本设置',
    sidebarSettings: '侧边栏设置',
    theme: '主题',
    lightTheme: '浅色主题',
    darkTheme: '深色主题',
    language: '语言',
    sidebarWidth: '侧边栏宽度',
    chinese: '简体中文',
    to: '至',
    startDate: '开始日期',
    endDate: '结束日期',
    themeSettings: '主题设置',
    presetThemes: '预设主题',
    colorSettings: '颜色设置',
    fontSettings: '字体设置',
    animationSettings: '动画设置',
    enableAnimations: '启用动画效果',
    animationsEnabled: '全局动画已开启',
    animationsDisabled: '全局动画已关闭',
    primaryColor: '主题色',
    successColor: '成功色',
    warningColor: '警告色',
    dangerColor: '危险色',
    infoColor: '信息色',
    fontFamily: '字体',
    baseFontSize: '基础字号',
    largeFontSize: '大字号',
    smallFontSize: '小字号',
    saveAsPreset: '保存为预设',
    presetName: '预设名称',
    applyTheme: '应用主题',
    deletePreset: '删除预设',
    resetToDefault: '恢复默认',
    exportTheme: '导出主题',
    importTheme: '导入主题',
    customPresets: '自定义预设',
    maxPresetsReached: '最多保存 {max} 个自定义预设',
  },
```

- [ ] **Step 6: 添加 dict 对象**

在 `settings` 对象后添加：

```typescript
  dict: {
    typeTitle: '字典类型',
    dataTitle: '字典数据',
    dictName: '字典名称',
    dictType: '字典类型',
    dictLabel: '字典标签',
    dictValue: '字典键值',
    dictSort: '字典排序',
    locale: '语言标识',
    listClass: '标签样式',
    cssClass: 'CSS类名',
    isDefault: '是否默认',
    statusEnable: '启用',
    statusDisable: '禁用',
    selectDictType: '请选择字典类型',
    selectDictTypeHint: '请从左侧选择字典类型查看数据',
    addDictType: '新增字典类型',
    editDictType: '编辑字典类型',
    addDictData: '新增字典数据',
    editDictData: '编辑字典数据',
    deleteTypeConfirm: '确定要删除该字典类型吗？',
    deleteDataConfirm: '确定要删除该字典数据吗？',
    dictTypeFormat: '字典类型只能包含小写字母、数字和下划线，且必须以字母开头',
    dataItems: '项数据',
  },
```

- [ ] **Step 7: 提交**

```bash
git add blink-gateway/gateway-admin-web/src/locales/zh-cn.ts
git commit -m "feat(gateway-admin-web): add i18n translations for permission, operationLog, settings, dict"
```

---

### Task 11: 更新英文语言包

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/locales/en-us.ts`

- [ ] **Step 1: 在 common 对象后添加 message 对象**

在 `common` 对象后添加：

```typescript
  message: {
    tips: 'Tips',
    operationSuccess: 'Operation successful',
    saveSuccess: 'Saved successfully',
    deleteSuccess: 'Deleted successfully',
    confirmDelete: 'Are you sure to delete?',
  },
```

- [ ] **Step 2: 在 menu 对象中添加新菜单项**

更新 `menu` 对象：

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
    menu: 'Menu',
    permission: 'Permission',
    operationLog: 'Operation Log',
    dict: 'Dictionary',
    dictType: 'Dict Type',
    dictData: 'Dict Data',
    settings: 'Settings',
  },
```

- [ ] **Step 3: 添加 system.permission 对象**

在 `system.menu` 对象后添加：

```typescript
    permission: {
      title: 'Permission Management',
      acName: 'Permission Name',
      acEnName: 'English Name',
      acIdentity: 'Permission Identity',
      acType: 'Permission Type',
      acTypeApi: 'API Permission',
      acTypeData: 'Data Permission',
      typeApi: 'API Permission',
      typeData: 'Data Permission',
      url: 'Permission URL',
      dataFilterId: 'Data Filter Rule',
      dataFilterRule: 'Select Data Filter Rule',
      createTime: 'Create Time',
      createBy: 'Created By',
      addPermission: 'Add Permission',
      editPermission: 'Edit Permission',
      deleteConfirm: 'Are you sure to delete this permission?',
      relatedMenus: 'Related Menus',
    },
```

- [ ] **Step 4: 添加 system.operationLog 对象**

在 `system.permission` 对象后添加：

```typescript
    operationLog: {
      title: 'Operation Log',
      list: 'Operation Log List',
      detailTitle: 'Operation Log Detail',
      keywordPlaceholder: 'Enter keyword to search description or URL',
      operator: 'Operator',
      logType: 'Log Type',
      executeStatus: 'Execute Status',
      operationTime: 'Operation Time',
      logId: 'Log ID',
      userId: 'User ID',
      description: 'Description',
      requestUrl: 'Request URL',
      requestMethod: 'Request Method',
      executeTimeMs: 'Execute Time(ms)',
      ipAddress: 'IP Address',
      userAgent: 'User Agent',
      requestParams: 'Request Params',
      responseData: 'Response Data',
      errorMsg: 'Error Message',
      LOGIN: 'Login Log',
      SYSTEM: 'System Log',
      OPERATION: 'Operation Log',
    },
```

- [ ] **Step 5: 添加 settings 对象**

在 `system` 对象后添加：

```typescript
  settings: {
    title: 'Settings',
    basicSettings: 'Basic Settings',
    sidebarSettings: 'Sidebar Settings',
    theme: 'Theme',
    lightTheme: 'Light Theme',
    darkTheme: 'Dark Theme',
    language: 'Language',
    sidebarWidth: 'Sidebar Width',
    chinese: 'Chinese',
    to: 'to',
    startDate: 'Start Date',
    endDate: 'End Date',
    themeSettings: 'Theme Settings',
    presetThemes: 'Preset Themes',
    colorSettings: 'Color Settings',
    fontSettings: 'Font Settings',
    animationSettings: 'Animation Settings',
    enableAnimations: 'Enable Animations',
    animationsEnabled: 'Animations enabled',
    animationsDisabled: 'Animations disabled',
    primaryColor: 'Primary Color',
    successColor: 'Success Color',
    warningColor: 'Warning Color',
    dangerColor: 'Danger Color',
    infoColor: 'Info Color',
    fontFamily: 'Font Family',
    baseFontSize: 'Base Font Size',
    largeFontSize: 'Large Font Size',
    smallFontSize: 'Small Font Size',
    saveAsPreset: 'Save as Preset',
    presetName: 'Preset Name',
    applyTheme: 'Apply Theme',
    deletePreset: 'Delete Preset',
    resetToDefault: 'Reset to Default',
    exportTheme: 'Export Theme',
    importTheme: 'Import Theme',
    customPresets: 'Custom Presets',
    maxPresetsReached: 'Maximum {max} custom presets allowed',
  },
```

- [ ] **Step 6: 添加 dict 对象**

在 `settings` 对象后添加：

```typescript
  dict: {
    typeTitle: 'Dictionary Type',
    dataTitle: 'Dictionary Data',
    dictName: 'Dict Name',
    dictType: 'Dict Type',
    dictLabel: 'Dict Label',
    dictValue: 'Dict Value',
    dictSort: 'Dict Sort',
    locale: 'Locale',
    listClass: 'List Class',
    cssClass: 'CSS Class',
    isDefault: 'Is Default',
    statusEnable: 'Enable',
    statusDisable: 'Disable',
    selectDictType: 'Select Dict Type',
    selectDictTypeHint: 'Please select a dict type from left panel',
    addDictType: 'Add Dict Type',
    editDictType: 'Edit Dict Type',
    addDictData: 'Add Dict Data',
    editDictData: 'Edit Dict Data',
    deleteTypeConfirm: 'Are you sure to delete this dict type?',
    deleteDataConfirm: 'Are you sure to delete this dict data?',
    dictTypeFormat: 'Dict type can only contain lowercase letters, numbers and underscores, and must start with a letter',
    dataItems: 'data items',
  },
```

- [ ] **Step 7: 提交**

```bash
git add blink-gateway/gateway-admin-web/src/locales/en-us.ts
git commit -m "feat(gateway-admin-web): add English i18n translations"
```

---

### Task 12: 添加权限管理页面

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/views/system/permission/index.vue`
- Create: `blink-gateway/gateway-admin-web/src/views/system/permission/components/PermissionFormDialog.vue`
- Create: `blink-gateway/gateway-admin-web/src/views/system/permission/components/PermissionSelectDialog.vue`

- [ ] **Step 1: 创建目录结构**

```bash
mkdir -p blink-gateway/gateway-admin-web/src/views/system/permission/components
```

- [ ] **Step 2: 复制 index.vue**

复制 `blink-base/blink-base-web/src/views/system/permission/index.vue` 到 `blink-gateway/gateway-admin-web/src/views/system/permission/index.vue`

- [ ] **Step 3: 复制 PermissionFormDialog.vue**

复制 `blink-base/blink-base-web/src/views/system/permission/components/PermissionFormDialog.vue` 到目标位置

- [ ] **Step 4: 复制 PermissionSelectDialog.vue**

复制 `blink-base/blink-base-web/src/views/system/permission/components/PermissionSelectDialog.vue` 到目标位置

- [ ] **Step 5: 提交**

```bash
git add blink-gateway/gateway-admin-web/src/views/system/permission/
git commit -m "feat(gateway-admin-web): add permission management page"
```

---

### Task 13: 添加操作日志页面

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/views/system/operation-log/index.vue`
- Create: `blink-gateway/gateway-admin-web/src/views/system/operation-log/components/OperationLogDetailDialog.vue`

- [ ] **Step 1: 创建目录结构**

```bash
mkdir -p blink-gateway/gateway-admin-web/src/views/system/operation-log/components
```

- [ ] **Step 2: 复制 index.vue**

复制 `blink-base/blink-base-web/src/views/system/operation-log/index.vue` 到目标位置

- [ ] **Step 3: 复制 OperationLogDetailDialog.vue**

复制 `blink-base/blink-base-web/src/views/system/operation-log/components/OperationLogDetailDialog.vue` 到目标位置

- [ ] **Step 4: 提交**

```bash
git add blink-gateway/gateway-admin-web/src/views/system/operation-log/
git commit -m "feat(gateway-admin-web): add operation log page"
```

---

### Task 14: 添加字典类型页面

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/views/system/dict/type/index.vue`
- Create: `blink-gateway/gateway-admin-web/src/views/system/dict/type/components/DictTypeFormDialog.vue`

- [ ] **Step 1: 创建目录结构**

```bash
mkdir -p blink-gateway/gateway-admin-web/src/views/system/dict/type/components
```

- [ ] **Step 2: 复制 index.vue**

复制 `blink-base/blink-base-web/src/views/system/dict/type/index.vue` 到目标位置

- [ ] **Step 3: 复制 DictTypeFormDialog.vue**

复制 `blink-base/blink-base-web/src/views/system/dict/type/components/DictTypeFormDialog.vue` 到目标位置

- [ ] **Step 4: 提交**

```bash
git add blink-gateway/gateway-admin-web/src/views/system/dict/type/
git commit -m "feat(gateway-admin-web): add dict type page"
```

---

### Task 15: 添加字典数据页面

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/views/system/dict/data/index.vue`
- Create: `blink-gateway/gateway-admin-web/src/views/system/dict/data/components/DictDataFormDialog.vue`

- [ ] **Step 1: 创建目录结构**

```bash
mkdir -p blink-gateway/gateway-admin-web/src/views/system/dict/data/components
```

- [ ] **Step 2: 复制 index.vue**

复制 `blink-base/blink-base-web/src/views/system/dict/data/index.vue` 到目标位置

- [ ] **Step 3: 复制 DictDataFormDialog.vue**

复制 `blink-base/blink-base-web/src/views/system/dict/data/components/DictDataFormDialog.vue` 到目标位置

- [ ] **Step 4: 提交**

```bash
git add blink-gateway/gateway-admin-web/src/views/system/dict/data/
git commit -m "feat(gateway-admin-web): add dict data page"
```

---

### Task 16: 添加设置页面

**Files:**
- Create: `blink-gateway/gateway-admin-web/src/views/settings/index.vue`
- Create: `blink-gateway/gateway-admin-web/src/views/settings/components/ThemeEditor.vue`

- [ ] **Step 1: 创建目录结构**

```bash
mkdir -p blink-gateway/gateway-admin-web/src/views/settings/components
```

- [ ] **Step 2: 复制 index.vue**

复制 `blink-base/blink-base-web/src/views/settings/index.vue` 到目标位置

- [ ] **Step 3: 复制 ThemeEditor.vue**

复制 `blink-base/blink-base-web/src/views/settings/components/ThemeEditor.vue` 到目标位置

- [ ] **Step 4: 提交**

```bash
git add blink-gateway/gateway-admin-web/src/views/settings/
git commit -m "feat(gateway-admin-web): add settings page"
```

---

### Task 17: 更新路由配置

**Files:**
- Modify: `blink-gateway/gateway-admin-web/src/router/index.ts`

- [ ] **Step 1: 在 system children 中添加新路由**

在 `system` 的 `children` 数组中，在 `menu` 路由后添加：

```typescript
          {
            path: 'permission',
            name: 'SystemPermission',
            component: () => import('@/views/system/permission/index.vue'),
            meta: { title: 'system.permission.title' }
          },
          {
            path: 'operation-log',
            name: 'SystemOperationLog',
            component: () => import('@/views/system/operation-log/index.vue'),
            meta: { title: 'system.operationLog.title' }
          },
          {
            path: 'dict',
            redirect: '/system/dict/type',
            meta: { title: 'system.dict.title' },
            children: [
              {
                path: 'type',
                name: 'SystemDictType',
                component: () => import('@/views/system/dict/type/index.vue'),
                meta: { title: 'system.dict.typeTitle' }
              },
              {
                path: 'data',
                name: 'SystemDictData',
                component: () => import('@/views/system/dict/data/index.vue'),
                meta: { title: 'system.dict.dataTitle' }
              }
            ]
          }
```

- [ ] **Step 2: 在根路由 children 中添加 settings 路由**

在 `monitor` 路由后添加：

```typescript
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/index.vue'),
        meta: { title: 'settings.title' }
      },
```

- [ ] **Step 3: 提交**

```bash
git add blink-gateway/gateway-admin-web/src/router/index.ts
git commit -m "feat(gateway-admin-web): add routes for permission, operation-log, settings, dict pages"
```

---

### Task 18: 验证构建

- [ ] **Step 1: 运行构建验证**

```bash
cd blink-gateway/gateway-admin-web && npm run build
```

Expected: 构建成功，无 TypeScript 错误

- [ ] **Step 2: 如果有错误，修复并提交**

如果构建失败，根据错误信息修复代码，然后提交修复。

---

## 完成检查清单

- [ ] 所有 API 文件已创建
- [ ] 所有 composable 已创建
- [ ] AuthButton 组件已创建
- [ ] user store 已添加 isSuperAdmin
- [ ] theme store 已替换为完整版本
- [ ] themes.ts 配置已创建
- [ ] 中英文语言包已更新
- [ ] 所有页面组件已创建
- [ ] 路由配置已更新
- [ ] 项目构建成功