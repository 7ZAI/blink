# Gateway Admin Frontend-Backend Integration Design

**Date:** 2026-03-13
**Author:** Claude
**Status:** Draft

---

## Overview

This document describes the design for connecting gateway-admin-web frontend with gateway-admin backend, focusing on:

1. **Auth Integration** - Replace mock authentication with real backend API
2. **User Management** - Complete CRUD operations for users
3. **Role Management** - Complete CRUD operations for roles with menu permission assignment
4. **Menu Management** - Tree-based menu management with CRUD operations

---

## Current State

| Module | Backend | Frontend API | Frontend View | Status |
|--------|---------|--------------|---------------|--------|
| Auth | ✅ Complete | ⚠️ Mock only | ✅ Login page | Needs real API |
| User | ✅ Complete | ❌ Missing | ❌ Missing | Needs implementation |
| Role | ✅ Complete | ❌ Missing | ❌ Missing | Needs implementation |
| Menu | ✅ Complete | ❌ Missing | ❌ Missing | Needs implementation |
| Channel | ✅ Complete | ✅ Connected | ✅ Working | Done |
| Route | ✅ Complete | ✅ Connected | ✅ Working | Done |
| Config | ✅ Complete | ✅ Connected | ✅ Working | Done |
| Monitor | ✅ Complete | ✅ Connected | ✅ Working | Done |

---

## Backend API Reference

### AuthController (`/auth`)

| Endpoint | Method | Request | Response | Description |
|----------|--------|---------|----------|-------------|
| `/auth/login` | POST | `LoginReq` | `LoginRsp` | User login |
| `/auth/logout` | POST | - | `EmptyBody` | User logout |
| `/auth/getUserInfo` | POST | - | `UserInfoRsp` | Get current user info |
| `/auth/getUserMenus` | POST | - | `List<MenuVO>` | Get user's menu tree |
| `/auth/modifyPassword` | POST | `ModifyPasswordReq` | `EmptyBody` | Change password |

### UserController (`/user`)

| Endpoint | Method | Request | Response | Description |
|----------|--------|---------|----------|-------------|
| `/user/getUserList` | POST | `QueryUserReq` | `QueryUserRsp` | Paginated user list |
| `/user/getUser` | POST | `DeleteUserReq` | `UserVO` | Get user detail |
| `/user/saveUser` | POST | `AddUserReq` | `EmptyBody` | Create user |
| `/user/modifyUser` | POST | `ModifyUserReq` | `EmptyBody` | Update user |
| `/user/deleteUser` | POST | `DeleteUserReq` | `EmptyBody` | Delete user |
| `/user/resetPassword` | POST | `ResetPasswordReq` | `EmptyBody` | Reset password |
| `/user/unlockUser` | POST | `UnlockUserReq` | `EmptyBody` | Unlock user |
| `/user/assignRole` | POST | `AssignRoleReq` | `EmptyBody` | Assign roles to user |

### RoleController (`/role`)

| Endpoint | Method | Request | Response | Description |
|----------|--------|---------|----------|-------------|
| `/role/getRoleList` | POST | `QueryRoleReq` | `List<RoleVO>` | Get role list |
| `/role/saveRole` | POST | `AddRoleReq` | `EmptyBody` | Create role |
| `/role/modifyRole` | POST | `ModifyRoleReq` | `EmptyBody` | Update role |
| `/role/deleteRole` | POST | `DeleteRoleReq` | `EmptyBody` | Delete role |
| `/role/assignMenu` | POST | `AssignMenuReq` | `EmptyBody` | Assign menus to role |

### MenuController (`/menu`)

| Endpoint | Method | Request | Response | Description |
|----------|--------|---------|----------|-------------|
| `/menu/getMenuTree` | POST | - | `List<MenuVO>` | Get menu tree |
| `/menu/saveMenu` | POST | `AddMenuReq` | `EmptyBody` | Create menu |
| `/menu/modifyMenu` | POST | `ModifyMenuReq` | `EmptyBody` | Update menu |
| `/menu/deleteMenu` | POST | `DeleteMenuReq` | `EmptyBody` | Delete menu |

---

## Design Details

### Layer 1: Auth Integration

#### API Changes

**File: `src/api/auth.ts`**

Replace mock implementation with real API calls:

```typescript
import request from '@/utils/request'

export interface LoginReq {
  loginName: string
  password: string
  rememberMe?: boolean
}

export interface LoginRsp {
  token: string
  userId: number
  loginName: string
  username: string
  firstLogin: number  // 0=no, 1=yes
  roleCodes: string[]
}

export interface UserInfoRsp {
  userId: number
  loginName: string
  username: string
  phone?: string
  email?: string
  status: number
  roleCodes: string[]
}

export interface MenuVO {
  menuId: number
  menuName: string
  menuEnName: string
  parentId: number
  menuLevel: number
  menuType: number  // 1=目录, 2=菜单, 3=按钮
  path: string
  component: string
  perms: string
  icon: string
  orderNum: number
  visible: number
  status: number
  createTime: string
  children?: MenuVO[]
}

export const login = (params: LoginReq): Promise<LoginRsp> => {
  return request.post('/auth/login', { body: params })
}

export const logout = (): Promise<void> => {
  return request.post('/auth/logout', { body: {} })
}

export const getUserInfo = (): Promise<UserInfoRsp> => {
  return request.post('/auth/getUserInfo', { body: {} })
}

export const getUserMenus = (): Promise<MenuVO[]> => {
  return request.post('/auth/getUserMenus', { body: {} })
}

export const modifyPassword = (params: { oldPassword: string; newPassword: string }): Promise<void> => {
  return request.post('/auth/modifyPassword', { body: params })
}
```

#### Store Changes

**File: `src/stores/user.ts`**

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo, getUserMenus, type LoginRsp, type UserInfoRsp, type MenuVO } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || sessionStorage.getItem('token') || '')
  const userInfo = ref<UserInfoRsp | null>(null)
  const menus = ref<MenuVO[]>([])

  const isLoggedIn = computed(() => !!token.value)

  const login = async (loginName: string, password: string, rememberMe: boolean): Promise<LoginRsp> => {
    const rsp = await loginApi({ loginName, password, rememberMe })
    token.value = rsp.token

    if (rememberMe) {
      localStorage.setItem('token', rsp.token)
    } else {
      sessionStorage.setItem('token', rsp.token)
    }

    return rsp
  }

  const logout = async () => {
    await logoutApi()
    token.value = ''
    userInfo.value = null
    menus.value = []
    localStorage.removeItem('token')
    sessionStorage.removeItem('token')
  }

  const fetchUserInfo = async () => {
    const rsp = await getUserInfo()
    userInfo.value = rsp
    return rsp
  }

  const fetchMenus = async () => {
    const rsp = await getUserMenus()
    menus.value = rsp
    return rsp
  }

  return {
    token,
    userInfo,
    menus,
    isLoggedIn,
    login,
    logout,
    fetchUserInfo,
    fetchMenus
  }
})
```

#### Request Interceptor Changes

**File: `src/utils/request.ts`**

Add token header and 401 handling:

```typescript
// In request interceptor
config.headers['satoken'] = token  // or appropriate header name

// In response interceptor
if (response.status === 401) {
  const userStore = useUserStore()
  userStore.logout()
  router.push('/login')
  return Promise.reject(new Error('Unauthorized'))
}
```

#### Login Page Changes

**File: `src/views/login/index.vue`**

- Call real auth API
- Handle `firstLogin` flag to prompt password change
- On success: store token, fetch user info and menus, navigate to dashboard

---

### Layer 2: User Management

#### API Layer

**File: `src/api/user.ts` (new)**

```typescript
import request from '@/utils/request'

export interface QueryUserParams {
  pageNum?: number
  pageSize?: number
  loginName?: string
  username?: string
  status?: number
}

export interface UserVO {
  userId: number
  loginName: string
  username: string
  phone?: string
  email?: string
  status: number       // 0=正常, 1=禁用
  locked: number       // 0=未锁定, 1=管理员锁定, 2=密码错误锁定
  firstLogin: number   // 0=no, 1=yes
  lastLoginTime?: string
  createTime?: string
  roleIds?: number[]
  roleNames?: string[]
}

export interface AddUserParams {
  loginName: string
  username: string
  phone?: string
  email?: string
  roleIds?: number[]
}

export interface ModifyUserParams extends AddUserParams {
  userId: number
}

export interface PageResult<T> {
  pageNum: number
  pageSize: number
  total: number
  pages: number
  rows: T[]
}

export const getUserList = (params: QueryUserParams): Promise<PageResult<UserVO>> => {
  return request.post('/user/getUserList', { body: params })
}

export const getUser = (params: { userId: number }): Promise<UserVO> => {
  return request.post('/user/getUser', { body: params })
}

export const saveUser = (params: AddUserParams): Promise<void> => {
  return request.post('/user/saveUser', { body: params })
}

export const modifyUser = (params: ModifyUserParams): Promise<void> => {
  return request.post('/user/modifyUser', { body: params })
}

export const deleteUser = (params: { userId: number }): Promise<void> => {
  return request.post('/user/deleteUser', { body: params })
}

export const resetPassword = (params: { userId: number }): Promise<void> => {
  return request.post('/user/resetPassword', { body: params })
}

export const unlockUser = (params: { userId: number }): Promise<void> => {
  return request.post('/user/unlockUser', { body: params })
}

export const assignRole = (params: { userId: number; roleIds: number[] }): Promise<void> => {
  return request.post('/user/assignRole', { body: params })
}
```

#### View Layer

**File: `src/views/system/user/index.vue` (new)**

Main user management page with:
- Search form: loginName, username, status
- Data table with columns: loginName, username, phone, email, status, locked, roleNames, createTime
- Operations: Edit, Delete, Reset Password, Unlock, Assign Role
- Status tags with colors
- Pagination

**File: `src/views/system/user/components/UserFormDialog.vue` (new)**

Form dialog for add/edit user:
- Form fields: loginName, username, phone, email, roleIds (multi-select)
- Validation rules
- Edit mode: fetch user detail and pre-fill

**File: `src/views/system/user/components/AssignRoleDialog.vue` (new)**

Role assignment dialog:
- Role multi-select (fetch from roleApi.getRoleList)
- Pre-select current roles

---

### Layer 3: Role Management

#### API Layer

**File: `src/api/role.ts` (new)**

```typescript
import request from '@/utils/request'

export interface QueryRoleParams {
  roleName?: string
  roleCode?: string
  status?: number
}

export interface RoleVO {
  roleId: number
  roleName: string
  roleCode: string
  roleType: number   // 1=系统角色, 2=自定义角色
  status: number     // 0=正常, 1=禁用
  remark?: string
  createTime?: string
}

export interface AddRoleParams {
  roleName: string
  roleCode: string
  roleType: number
  remark?: string
}

export interface ModifyRoleParams extends AddRoleParams {
  roleId: number
}

export const getRoleList = (params?: QueryRoleParams): Promise<RoleVO[]> => {
  return request.post('/role/getRoleList', { body: params || {} })
}

export const saveRole = (params: AddRoleParams): Promise<void> => {
  return request.post('/role/saveRole', { body: params })
}

export const modifyRole = (params: ModifyRoleParams): Promise<void> => {
  return request.post('/role/modifyRole', { body: params })
}

export const deleteRole = (params: { roleId: number }): Promise<void> => {
  return request.post('/role/deleteRole', { body: params })
}

export const assignMenu = (params: { roleId: number; menuIds: number[] }): Promise<void> => {
  return request.post('/role/assignMenu', { body: params })
}
```

#### View Layer

**File: `src/views/system/role/index.vue` (new)**

Main role management page with:
- Search form: roleName, roleCode, status
- Data table with columns: roleName, roleCode, roleType, status, remark, createTime
- Operations: Edit, Delete, Assign Menu
- Role type and status tags

**File: `src/views/system/role/components/RoleFormDialog.vue` (new)**

Form dialog for add/edit role:
- Form fields: roleName, roleCode, roleType (radio), remark
- Validation rules

**File: `src/views/system/role/components/AssignMenuDialog.vue` (new)**

Menu assignment dialog:
- Menu tree with checkboxes (el-tree)
- Fetch menu tree from menuApi.getMenuTree
- Pre-select based on role's existing permissions

---

### Layer 4: Menu Management

#### API Layer

**File: `src/api/menu.ts` (new)**

```typescript
import request from '@/utils/request'

export interface MenuVO {
  menuId: number
  menuName: string
  menuEnName: string
  parentId: number
  menuLevel: number
  menuType: number    // 1=目录, 2=菜单, 3=按钮
  path: string
  component: string
  perms: string
  icon: string
  orderNum: number
  visible: number     // 0=可见, 1=隐藏
  status: number      // 0=正常, 1=禁用
  createTime?: string
  children?: MenuVO[]
}

export interface AddMenuParams {
  menuName: string
  menuEnName: string
  parentId: number
  menuLevel: number
  menuType: number
  path?: string
  component?: string
  perms?: string
  icon?: string
  orderNum: number
  visible: number
}

export interface ModifyMenuParams extends AddMenuParams {
  menuId: number
}

export const getMenuTree = (): Promise<MenuVO[]> => {
  return request.post('/menu/getMenuTree', { body: {} })
}

export const saveMenu = (params: AddMenuParams): Promise<void> => {
  return request.post('/menu/saveMenu', { body: params })
}

export const modifyMenu = (params: ModifyMenuParams): Promise<void> => {
  return request.post('/menu/modifyMenu', { body: params })
}

export const deleteMenu = (params: { menuId: number }): Promise<void> => {
  return request.post('/menu/deleteMenu', { body: params })
}
```

#### View Layer

**File: `src/views/system/menu/index.vue` (new)**

Main menu management page with:
- Toolbar: Add root menu button
- Tree table (el-table with tree-props)
- Columns: menuName, menuEnName, menuType, path, component, perms, icon, orderNum, visible, status, operations
- Operations: Add child, Edit, Delete

**File: `src/views/system/menu/components/MenuFormDialog.vue` (new)**

Form dialog for add/edit menu:
- Form fields vary by menuType:
  - Common: menuName, menuEnName, menuType, icon, orderNum, visible
  - 目录/菜单 only: path, component
  - 按钮 only: perms
- Parent selection via cascader or hidden field
- Validation rules

---

### Routing Integration

**File: `src/router/index.ts`**

Add system routes:

```typescript
{
  path: 'system',
  redirect: '/system/user',
  meta: { title: 'system.title' },
  children: [
    {
      path: 'user',
      name: 'SystemUser',
      component: () => import('@/views/system/user/index.vue'),
      meta: { title: 'system.user.title' }
    },
    {
      path: 'role',
      name: 'SystemRole',
      component: () => import('@/views/system/role/index.vue'),
      meta: { title: 'system.role.title' }
    },
    {
      path: 'menu',
      name: 'SystemMenu',
      component: () => import('@/views/system/menu/index.vue'),
      meta: { title: 'system.menu.title' }
    }
  ]
}
```

### Navigation Integration

**File: `src/layouts/MainLayout.vue`**

Add "System" navigation item:
- Position in nav menu based on existing pattern
- Children: User, Role, Menu

### i18n Keys

**File: `src/locales/zh-cn.ts`**

```typescript
system: {
  title: '系统管理',
  user: {
    title: '用户管理',
    loginName: '登录名',
    username: '用户昵称',
    phone: '手机号',
    email: '邮箱',
    status: '状态',
    statusNormal: '正常',
    statusDisabled: '禁用',
    locked: '锁定状态',
    lockedNo: '未锁定',
    lockedAdmin: '管理员锁定',
    lockedPassword: '密码错误锁定',
    roles: '角色',
    firstLogin: '首次登录',
    lastLoginTime: '最后登录',
    createTime: '创建时间',
    addUser: '新增用户',
    editUser: '编辑用户',
    deleteUser: '删除用户',
    resetPassword: '重置密码',
    unlockUser: '解锁用户',
    assignRole: '分配角色',
    deleteConfirm: '确定要删除该用户吗？',
    resetPasswordConfirm: '确定要重置该用户的密码吗？',
    unlockConfirm: '确定要解锁该用户吗？',
    passwordResetSuccess: '密码已重置为默认密码',
    unlockSuccess: '用户已解锁'
  },
  role: {
    title: '角色管理',
    roleName: '角色名称',
    roleCode: '角色编码',
    roleType: '角色类型',
    roleTypeSystem: '系统角色',
    roleTypeCustom: '自定义角色',
    remark: '备注',
    addRole: '新增角色',
    editRole: '编辑角色',
    deleteRole: '删除角色',
    assignMenu: '分配权限',
    deleteConfirm: '确定要删除该角色吗？',
    assignMenuTitle: '分配菜单权限'
  },
  menu: {
    title: '菜单管理',
    menuName: '菜单名称',
    menuEnName: '英文名',
    menuType: '类型',
    menuTypeDir: '目录',
    menuTypeMenu: '菜单',
    menuTypeButton: '按钮',
    path: '路由地址',
    component: '组件路径',
    perms: '权限标识',
    icon: '图标',
    orderNum: '排序',
    visible: '可见',
    visibleYes: '是',
    visibleNo: '否',
    addMenu: '新增菜单',
    editMenu: '编辑菜单',
    deleteMenu: '删除菜单',
    deleteConfirm: '确定要删除该菜单吗？',
    addRootMenu: '新增根菜单',
    addChildMenu: '新增子菜单',
    selectParent: '请选择上级菜单'
  }
}
```

**File: `src/locales/en-us.ts`**

```typescript
system: {
  title: 'System Management',
  user: {
    title: 'User Management',
    loginName: 'Login Name',
    username: 'Username',
    phone: 'Phone',
    email: 'Email',
    status: 'Status',
    statusNormal: 'Normal',
    statusDisabled: 'Disabled',
    locked: 'Lock Status',
    lockedNo: 'Unlocked',
    lockedAdmin: 'Admin Locked',
    lockedPassword: 'Password Locked',
    roles: 'Roles',
    firstLogin: 'First Login',
    lastLoginTime: 'Last Login',
    createTime: 'Create Time',
    addUser: 'Add User',
    editUser: 'Edit User',
    deleteUser: 'Delete User',
    resetPassword: 'Reset Password',
    unlockUser: 'Unlock User',
    assignRole: 'Assign Role',
    deleteConfirm: 'Are you sure you want to delete this user?',
    resetPasswordConfirm: 'Are you sure you want to reset this user\'s password?',
    unlockConfirm: 'Are you sure you want to unlock this user?',
    passwordResetSuccess: 'Password has been reset to default',
    unlockSuccess: 'User has been unlocked'
  },
  role: {
    title: 'Role Management',
    roleName: 'Role Name',
    roleCode: 'Role Code',
    roleType: 'Role Type',
    roleTypeSystem: 'System Role',
    roleTypeCustom: 'Custom Role',
    remark: 'Remark',
    addRole: 'Add Role',
    editRole: 'Edit Role',
    deleteRole: 'Delete Role',
    assignMenu: 'Assign Permissions',
    deleteConfirm: 'Are you sure you want to delete this role?',
    assignMenuTitle: 'Assign Menu Permissions'
  },
  menu: {
    title: 'Menu Management',
    menuName: 'Menu Name',
    menuEnName: 'English Name',
    menuType: 'Type',
    menuTypeDir: 'Directory',
    menuTypeMenu: 'Menu',
    menuTypeButton: 'Button',
    path: 'Route Path',
    component: 'Component Path',
    perms: 'Permission',
    icon: 'Icon',
    orderNum: 'Order',
    visible: 'Visible',
    visibleYes: 'Yes',
    visibleNo: 'No',
    addMenu: 'Add Menu',
    editMenu: 'Edit Menu',
    deleteMenu: 'Delete Menu',
    deleteConfirm: 'Are you sure you want to delete this menu?',
    addRootMenu: 'Add Root Menu',
    addChildMenu: 'Add Child Menu',
    selectParent: 'Please select parent menu'
  }
}
```

---

## File Structure

### New Files to Create

```
gateway-admin-web/src/
├── api/
│   ├── user.ts                    # User API
│   ├── role.ts                    # Role API
│   └── menu.ts                    # Menu API
└── views/
    └── system/
        ├── user/
        │   ├── index.vue          # User list page
        │   └── components/
        │       ├── UserFormDialog.vue
        │       └── AssignRoleDialog.vue
        ├── role/
        │   ├── index.vue          # Role list page
        │   └── components/
        │       ├── RoleFormDialog.vue
        │       └── AssignMenuDialog.vue
        └── menu/
            ├── index.vue          # Menu tree page
            └── components/
                └── MenuFormDialog.vue
```

### Files to Modify

```
gateway-admin-web/src/
├── api/
│   └── auth.ts                    # Replace mock with real API
├── stores/
│   └── user.ts                    # Add menus, fetchUserInfo, fetchMenus
├── utils/
│   └── request.ts                 # Add token header, 401 handling
├── router/
│   └── index.ts                   # Add system routes
├── layouts/
│   └── MainLayout.vue             # Add system navigation
├── views/
│   └── login/
│       └── index.vue              # Connect to real auth API
└── locales/
    ├── zh-cn.ts                   # Add system i18n
    └── en-us.ts                   # Add system i18n
```

---

## Success Criteria

1. **Auth Integration**
   - [ ] Login with real backend API
   - [ ] Token persisted and sent with requests
   - [ ] 401 triggers redirect to login
   - [ ] User info and menus fetched after login

2. **User Management**
   - [ ] List users with pagination
   - [ ] Add/Edit/Delete users
   - [ ] Reset password
   - [ ] Unlock user
   - [ ] Assign roles to user

3. **Role Management**
   - [ ] List roles
   - [ ] Add/Edit/Delete roles
   - [ ] Assign menu permissions

4. **Menu Management**
   - [ ] Display menu tree
   - [ ] Add/Edit/Delete menus
   - [ ] Add root menu and child menu

5. **General**
   - [ ] All pages use i18n
   - [ ] Consistent UI with existing pages
   - [ ] No console errors
   - [ ] Build succeeds