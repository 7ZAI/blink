# 权限菜单关联功能设计

**日期**: 2026-03-23
**作者**: Claude Code
**状态**: 设计完成

---

## 一、背景与目标

### 1.1 当前问题

- 角色需要分别分配菜单和权限，管理复杂
- 按钮菜单与接口权限是两套独立数据，无关联关系
- 用户配置权限时操作分散，容易遗漏

### 1.2 目标

- 实现接口权限与菜单的关联，分配菜单时自动获得关联权限
- 数据权限保持独立分配
- 简化权限配置流程，提升管理效率

---

## 二、核心概念

### 2.1 权限类型

| ac_type | 类型 | 说明 | 关联菜单 |
|---------|------|------|----------|
| 1 | 接口权限 | 控制 API 访问 | ✅ 可关联 |
| 2 | 数据权限 | 控制数据范围 | ❌ 不关联 |

### 2.2 菜单类型

| type | 类型 | 关联权限 |
|------|------|----------|
| 1 | 目录 | ❌ 不可关联 |
| 2 | 页面 | ✅ 可关联 |
| 3 | 按钮 | ✅ 可关联 |

### 2.3 关联关系

```
sys_menu.perm_id  ←→  sys_permission.ac_id

- 一个权限可关联多个菜单
- 一个菜单只能关联一个权限
- 双向绑定：权限端/菜单端都可操作
```

---

## 三、数据模型

### 3.1 现有表结构（无需改动）

**sys_menu 表：**

| 字段 | 类型 | 说明 |
|------|------|------|
| menu_id | int | 菜单ID |
| menu_name | varchar | 菜单名称 |
| type | tinyint | 1=目录, 2=页面, 3=按钮 |
| perm_id | int | 关联的权限ID（已有字段） |

**sys_permission 表：**

| 字段 | 类型 | 说明 |
|------|------|------|
| ac_id | int | 权限ID |
| ac_name | varchar | 权限名称 |
| ac_identity | varchar | 权限标识（如 sysUser:add） |
| ac_type | tinyint | 1=接口权限, 2=数据权限 |
| url | varchar | 接口地址 |
| data_filter_id | int | 数据过滤ID |

---

## 四、后端改造

### 4.1 权限管理接口

| 接口 | 改动内容 |
|------|----------|
| 新增权限 | 请求参数增加 `menuIds`（可选），创建后更新 `sys_menu.perm_id` |
| 编辑权限 | 请求参数增加 `menuIds`（可选），更新关联菜单 |
| 删除权限 | 删除前清空关联菜单的 `perm_id` |
| 查询权限详情 | 响应增加关联菜单列表 |

**请求参数示例：**

```java
// AddSysPermissionReq / UpdateSysPermissionReq
public class AddSysPermissionReq {
    private String acName;
    private String acIdentity;
    private Byte acType;        // 1=接口权限, 2=数据权限
    private String url;         // 接口权限必填
    private List<Integer> menuIds;  // 关联菜单ID列表（可选，仅 ac_type=1 时有效）
}
```

**联动逻辑：**

```java
// 权限端选择菜单时
1. 先清空其他菜单对当前权限的关联（一个权限关联多个菜单，但一个菜单只能关联一个权限）
2. 更新选中菜单的 perm_id = 当前权限ID
```

### 4.2 菜单管理接口

| 接口 | 改动内容 |
|------|----------|
| 新增菜单 | 请求参数增加 `permId`（可选），仅 type=2,3 时有效 |
| 编辑菜单 | 请求参数增加 `permId`（可选），更新关联权限 |
| 查询菜单详情 | 响应增加 `permId`、`permIdentity`、`permName` 字段 |

**响应字段扩展：**

```java
// SysMenuVO
public class SysMenuVO {
    // ... 现有字段
    private Integer permId;        // 关联的权限ID
    private String permIdentity;   // 关联的权限标识
    private String permName;       // 关联的权限名称
}
```

**联动逻辑：**

```java
// 菜单端选择权限时
1. 更新当前菜单的 perm_id = 选中的权限ID
```

### 4.3 角色管理接口

| 接口 | 改动内容 |
|------|----------|
| 分配菜单 | 增加自动写入接口权限关联的逻辑 |
| 分配权限 | 请求/响应不变，前端只传入数据权限 (ac_type=2) |

**分配菜单流程：**

```java
@Transactional
public void assignMenus(AssignMenuReq req) {
    Integer roleId = req.getRoleId();
    List<Integer> menuIds = req.getMenuIds();

    // 1. 删除原有菜单关联
    roleMenuRelaMapper.deleteByRoleId(roleId);

    // 2. 插入新的菜单关联
    batchInsertMenus(menuIds, roleId);

    // 3. 提取菜单关联的接口权限ID（新增逻辑）
    List<Integer> permIds = menuMapper.selectByIds(menuIds).stream()
        .filter(menu -> menu.getPermId() != null)
        .map(SysMenuDO::getPermId)
        .distinct()
        .toList();

    // 4. 删除原有的接口权限关联（仅 ac_type=1）
    rolePermRelaMapper.deleteApiPermissionsByRoleId(roleId);

    // 5. 插入新的接口权限关联
    if (!permIds.isEmpty()) {
        batchInsertPermissions(permIds, roleId);
    }
}
```

### 4.4 登录响应调整

**SysMenuVO 新增字段：**

```java
/**
 * 关联的权限标识（如 sysUser:add）
 */
private String permIdentity;

/**
 * 关联的权限名称
 */
private String permName;
```

**查询菜单时 JOIN 权限表：**

```xml
<!-- SysMenuMapper.xml -->
<select id="findSysMenuListByRole" resultType="com.blink.vo.dto.com.blink.base.SysMenuVO">
    SELECT sm.*,
           sp.ac_identity as perm_identity,
           sp.ac_name as perm_name
    FROM sys_menu sm
    INNER JOIN sys_role_menu_rela srmr ON srmr.menu_id = sm.menu_id
    LEFT JOIN sys_permission sp ON sp.ac_id = sm.perm_id
    WHERE srmr.role_id IN
    <foreach collection="roleIds" item="roleId" open="(" close=")" separator=",">
        #{roleId}
    </foreach>
    AND sm.status = 0
    AND sm.delFlag = 0
    ORDER BY sm.parent_id ASC, sm.order_number ASC
</select>
```

---

## 五、前端改造

### 5.1 权限管理页面

**页面位置：** `src/views/system/permission/index.vue`

| 功能 | 改动内容 |
|------|----------|
| 新增/编辑弹窗 | 增加"关联菜单"树形选择器（仅 ac_type=1 时显示） |
| 权限列表 | 增加"关联菜单"列 |
| 删除权限 | 提示会自动解除菜单关联 |

**关联菜单选择器交互：**

```
┌─────────────────────────────────────┐
│ 关联菜单（可选）                      │
├─────────────────────────────────────┤
│ ○ 系统管理 (目录 - 不可选)            │
│   ○ 用户管理 (页面)                  │
│     ☑ 新增用户 (按钮)                │
│     ○ 编辑用户 (按钮)                │
│   ○ 角色管理 (页面)                  │
└─────────────────────────────────────┘

规则：
- 显示完整菜单树（包含目录）
- 目录 (type=1) 的 checkbox 禁用/隐藏
- 仅页面 (type=2) 和按钮 (type=3) 可选择
- 可多选（一个权限关联多个菜单）
- 提交时过滤只提交 type=2,3 的菜单ID
```

### 5.2 菜单管理页面

**页面位置：** `src/views/system/menu/index.vue`

| 功能 | 改动内容 |
|------|----------|
| 新增/编辑弹窗 | 增加"关联权限"下拉选择器（仅 type=2,3 时显示） |
| 菜单列表 | 增加"关联权限"列 |

**关联权限选择器交互：**

```
┌─────────────────────────────────────┐
│ 关联权限（可选）                      │
├─────────────────────────────────────┤
│ ▼ sysUser:add - 新增系统用户         │
│   sysUser:update - 更新系统用户      │
│   sysUser:delete - 删除系统用户      │
└─────────────────────────────────────┘

规则：
- 仅显示接口权限 (ac_type=1)
- 单选（一个菜单只能关联一个权限）
- 下拉项显示格式：权限标识 - 权限名称
```

### 5.3 角色管理页面

**页面位置：** `src/views/system/role/index.vue`

| 功能 | 改动内容 |
|------|----------|
| "分配权限"按钮 | 改名为"分配数据权限" |
| 分配权限弹窗 | 仅显示数据权限 (ac_type=2) |
| 分配菜单弹窗 | 无需改动 |

### 5.4 类型定义更新

**src/types/index.ts：**

```typescript
export interface Menu {
  menuId: number
  menuName: string
  menuEnName: string
  type: number
  icon: string
  url: string
  orderNumber: number
  status: number
  parentId: number
  menuLevel: number
  componentPath: string
  hasChildren: boolean
  permId?: number
  permIdentity?: string  // 新增：关联的权限标识
  permName?: string      // 新增：关联的权限名称
  children?: Menu[]
}
```

### 5.5 现有权限组件（无需改动）

现有权限组件已正确实现，无需改动：

- `v-auth` 指令
- `AuthButton` 组件
- `usePermission()` 组合函数
- `ButtonPerms` 常量

---

## 六、权限判断流程

### 6.1 分配菜单流程

```
用户选择菜单 → 写入 sys_role_menu_rela
            → 提取菜单的 perm_id
            → 写入 sys_role_perm_rela（接口权限）
```

### 6.2 登录流程

```
用户登录 → 查询 sys_role_menu_rela 获取菜单
        → 菜单包含 permIdentity 字段（用于前端显示）
        → 查询 sys_role_perm_rela 获取权限标识
        → 返回 menus + permissions 给前端
```

### 6.3 前端权限判断

```
按钮权限判断 → 检查 userStore.permissions 数组
            → 包含权限标识则显示按钮
            → 不包含则隐藏/禁用
```

---

## 七、边界情况处理

### 7.1 权限删除

- 自动清空关联菜单的 `perm_id`
- 不影响已分配给角色的权限关联

### 7.2 菜单删除

- 不影响权限数据
- 下次分配菜单时自动排除已删除菜单

### 7.3 权限关联冲突

- 一个菜单只能关联一个权限
- 选择新权限时自动覆盖旧关联

---

## 八、改造文件清单

### 后端文件

| 文件 | 改动类型 |
|------|----------|
| `SysPermissionController.java` | 修改：新增/编辑接口增加 menuIds 参数 |
| `SysPermissionServiceImpl.java` | 修改：权限操作时同步更新菜单关联 |
| `AddSysPermissionReq.java` | 修改：增加 menuIds 字段 |
| `UpdateSysPermissionReq.java` | 修改：增加 menuIds 字段 |
| `SysMenuController.java` | 修改：新增/编辑接口增加 permId 参数 |
| `SysMenuServiceImpl.java` | 修改：菜单操作时同步更新权限关联 |
| `AddSysMenuReq.java` | 修改：增加 permId 字段 |
| `UpdateSysMenuReq.java` | 修改：增加 permId 字段 |
| `SysMenuVO.java` | 修改：增加 permIdentity、permName 字段 |
| `SysMenuMapper.xml` | 修改：查询时 JOIN 权限表 |
| `SysRoleServiceImpl.java` | 修改：assignMenus 方法增加权限同步逻辑 |

### 前端文件

| 文件 | 改动类型 |
|------|----------|
| `src/views/system/permission/index.vue` | 修改：增加关联菜单选择器 |
| `src/views/system/menu/index.vue` | 修改：增加关联权限选择器 |
| `src/views/system/role/index.vue` | 修改：按钮文字改为"分配数据权限" |
| `src/types/index.ts` | 修改：Menu 类型增加 permIdentity、permName |

---

## 九、测试要点

1. **权限管理**：创建/编辑/删除权限，验证菜单关联正确
2. **菜单管理**：创建/编辑菜单，验证权限关联正确
3. **角色分配**：分配菜单后验证权限自动写入
4. **登录验证**：验证返回的菜单包含权限标识，permissions 正确
5. **前端权限**：验证按钮根据权限正确显示/隐藏