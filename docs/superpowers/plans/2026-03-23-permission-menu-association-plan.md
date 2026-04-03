# 权限菜单关联功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现接口权限与菜单的双向关联，分配菜单时自动获得关联权限，简化权限配置流程。

**Architecture:** 在现有权限模型基础上，通过 sys_menu.perm_id 字段建立菜单与权限的关联。权限管理页面和菜单管理页面均支持双向绑定操作。角色分配菜单时自动提取关联的接口权限写入 sys_role_perm_rela。

**Tech Stack:** Spring Boot 3.2, MyBatis-Plus, Vue 3, TypeScript, Element Plus

---

## 文件结构

### 后端文件

| 文件 | 职责 |
|------|------|
| `AddSysPermissionReq.java` | 新增权限请求DTO，增加menuIds字段 |
| `UpdateSysPermissionReq.java` | 更新权限请求DTO，增加menuIds字段 |
| `SysPermissionServiceImpl.java` | 权限服务实现，处理权限-菜单关联逻辑 |
| `AddSysMenuReq.java` | 新增菜单请求DTO，增加permId字段 |
| `UpdateSysMenuReq.java` | 更新菜单请求DTO，增加permId字段 |
| `SysMenuVO.java` | 菜单VO，增加permIdentity和permName字段 |
| `SysMenuServiceImpl.java` | 菜单服务实现，处理菜单-权限关联逻辑 |
| `SysMenuMapper.xml` | 菜单Mapper，JOIN权限表查询 |
| `SysRoleServiceImpl.java` | 角色服务实现，分配菜单时同步权限 |
| `SysRolePermRelaMapper.java` | 角色权限关联Mapper，增加删除接口权限方法 |
| `SysRolePermRelaMapper.xml` | 角色权限关联Mapper XML |
| `SysPermissionVO.java` | 权限VO，增加menuIds字段用于编辑回显 |

### 前端文件

| 文件 | 职责 |
|------|------|
| `src/types/index.ts` | 类型定义，Menu增加permIdentity和permName |
| `src/api/permission.ts` | 权限API，增加menuIds参数 |
| `src/api/menu.ts` | 菜单API，增加permId参数 |
| `src/views/system/permission/components/PermissionFormDialog.vue` | 权限表单弹窗，增加关联菜单选择器 |
| `src/views/system/menu/components/MenuFormDialog.vue` | 菜单表单弹窗，增加关联权限选择器 |
| `src/views/system/role/index.vue` | 角色管理页面，按钮文字改为"分配数据权限" |
| `src/views/system/role/components/AssignPermissionDialog.vue` | 分配权限弹窗，仅显示数据权限 |

---

## Task 1: 后端 - 权限请求DTO增加menuIds字段

**Files:**
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/dto/req/AddSysPermissionReq.java`
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/dto/req/UpdateSysPermissionReq.java`

- [ ] **Step 1: 修改AddSysPermissionReq.java**

在文件末尾 `}` 之前添加：

```java
    /**
     * 关联菜单ID列表（仅接口权限ac_type=1时有效）
     */
    private List<Integer> menuIds;
```

- [ ] **Step 2: 修改UpdateSysPermissionReq.java**

在文件末尾 `}` 之前添加：

```java
    /**
     * 关联菜单ID列表（仅接口权限ac_type=1时有效）
     */
    private List<Integer> menuIds;
```

- [ ] **Step 3: 提交**

```bash
git add blink-base/blink-base-app/src/main/java/com/blink/base/dto/req/AddSysPermissionReq.java blink-base/blink-base-app/src/main/java/com/blink/base/dto/req/UpdateSysPermissionReq.java
git commit -m "feat(dto): 权限请求DTO增加menuIds字段

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 2: 后端 - 权限服务实现关联菜单逻辑

**Files:**
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/service/impl/SysPermissionServiceImpl.java`
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/mapper/SysMenuMapper.java`

- [ ] **Step 1: 在SysPermissionServiceImpl中注入SysMenuMapper**

在类的依赖注入区域添加：

```java
    @Resource
    private SysMenuMapper sysMenuMapper;
```

- [ ] **Step 2: 修改saveSysPermission方法**

在 `sysPermissionMapper.insert(sysPermissionDO);` 之后添加：

```java
        // 处理关联菜单（仅接口权限）
        if (CommonConstans.PERMISSION_API_TYPE.equals(saveParam.getAcType())
                && CollUtil.isNotEmpty(saveParam.getMenuIds())) {
            updateMenuPermissionRelation(sysPermissionDO.getAcId(), saveParam.getMenuIds());
        }
```

- [ ] **Step 3: 修改modifySysPermission方法**

在 `sysPermissionMapper.updateById(sysPermissionDO);` 之后添加：

```java
        // 处理关联菜单（仅接口权限）
        if (CommonConstans.PERMISSION_API_TYPE.equals(updateParam.getAcType())) {
            updateMenuPermissionRelation(sysPermissionDO.getAcId(),
                Optional.ofNullable(updateParam.getMenuIds()).orElse(Collections.emptyList()));
        }
```

- [ ] **Step 4: 修改deleteSysPermission方法**

在删除逻辑之前（`sysPermissionMapper.deleteByIds` 或 `sysPermissionMapper.deleteById` 之前）添加：

```java
        // 清空关联菜单的perm_id
        List<Integer> permIds = deleteParam.isBatchDelete()
            ? deleteParam.getIdList()
            : Collections.singletonList(deleteParam.getDeleteId());
        clearMenuPermissionRelation(permIds);
```

- [ ] **Step 5: 添加辅助方法**

在类的末尾 `}` 之前添加：

```java
    /**
     * 更新菜单与权限的关联关系
     *
     * @param permId  权限ID
     * @param menuIds 菜单ID列表
     */
    private void updateMenuPermissionRelation(Integer permId, List<Integer> menuIds) {
        // 先清空所有关联此权限的菜单
        sysMenuMapper.updatePermIdToNullByPermId(permId);

        // 更新选中菜单的perm_id
        if (CollUtil.isNotEmpty(menuIds)) {
            sysMenuMapper.updatePermIdByMenuIds(permId, menuIds);
            log.info("[SysPermission] 更新菜单权限关联 | permId: {}, menuIds: {}", permId, menuIds);
        }
    }

    /**
     * 清空菜单与权限的关联关系
     *
     * @param permIds 权限ID列表
     */
    private void clearMenuPermissionRelation(List<Integer> permIds) {
        if (CollUtil.isEmpty(permIds)) {
            return;
        }
        permIds.forEach(permId -> sysMenuMapper.updatePermIdToNullByPermId(permId));
        log.info("[SysPermission] 清空菜单权限关联 | permIds: {}", permIds);
    }
```

- [ ] **Step 6: 在SysMenuMapper中添加方法**

在 `SysMenuMapper.java` 接口中添加：

```java
    /**
     * 清空指定权限关联的所有菜单的perm_id
     *
     * @param permId 权限ID
     */
    void updatePermIdToNullByPermId(@Param("permId") Integer permId);

    /**
     * 批量更新菜单的perm_id
     *
     * @param permId  权限ID
     * @param menuIds 菜单ID列表
     */
    void updatePermIdByMenuIds(@Param("permId") Integer permId, @Param("menuIds") List<Integer> menuIds);
```

- [ ] **Step 7: 在SysMenuMapper.xml中添加SQL**

在 `</mapper>` 之前添加：

```xml
    <!-- 清空指定权限关联的所有菜单的perm_id -->
    <update id="updatePermIdToNullByPermId">
        UPDATE sys_menu SET perm_id = NULL WHERE perm_id = #{permId}
    </update>

    <!-- 批量更新菜单的perm_id -->
    <update id="updatePermIdByMenuIds">
        UPDATE sys_menu SET perm_id = #{permId}
        WHERE menu_id IN
        <foreach collection="menuIds" item="menuId" open="(" close=")" separator=",">
            #{menuId}
        </foreach>
    </update>
```

- [ ] **Step 8: 提交**

```bash
git add blink-base/blink-base-app/src/main/java/com/blink/base/service/impl/SysPermissionServiceImpl.java blink-base/blink-base-app/src/main/java/com/blink/base/mapper/SysMenuMapper.java blink-base/blink-base-app/src/main/resources/mapper/SysMenuMapper.xml
git commit -m "feat(permission): 权限服务实现关联菜单逻辑

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 3: 后端 - 菜单请求DTO增加permId字段

**Files:**
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/dto/req/AddSysMenuReq.java`
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/dto/req/UpdateSysMenuReq.java`

- [ ] **Step 1: 修改AddSysMenuReq.java**

在文件末尾 `}` 之前添加：

```java
    /**
     * 关联的权限ID（仅type=2页面或type=3按钮时有效）
     */
    private Integer permId;
```

- [ ] **Step 2: 修改UpdateSysMenuReq.java**

在文件末尾 `}` 之前添加：

```java
    /**
     * 关联的权限ID（仅type=2页面或type=3按钮时有效）
     */
    private Integer permId;
```

- [ ] **Step 3: 提交**

```bash
git add blink-base/blink-base-app/src/main/java/com/blink/base/dto/req/AddSysMenuReq.java blink-base/blink-base-app/src/main/java/com/blink/base/dto/req/UpdateSysMenuReq.java
git commit -m "feat(dto): 菜单请求DTO增加permId字段

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 4: 后端 - 菜单VO增加权限标识字段

**Files:**
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/dto/vo/SysMenuVO.java`

- [ ] **Step 1: 修改SysMenuVO.java**

在现有 `permId` 字段之后添加：

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

- [ ] **Step 2: 提交**

```bash
git add blink-base/blink-base-app/src/main/java/com/blink/base/dto/vo/SysMenuVO.java
git commit -m "feat(vo): 菜单VO增加权限标识字段

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 5: 后端 - 菜单服务实现关联权限逻辑

**Files:**
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/service/impl/SysMenuServiceImpl.java`

- [ ] **Step 1: 修改saveSysMenu方法**

在 `sysMenuMapper.insert(sysMenuDO);` 之前，将 `BeanUtil.copyProperties(saveParam, sysMenuDO);` 之后添加：

```java
        // 处理关联权限（仅页面和按钮菜单）
        if ((saveParam.getType() == 2 || saveParam.getType() == 3)
                && ObjectUtil.isNotNull(saveParam.getPermId())) {
            sysMenuDO.setPermId(saveParam.getPermId());
        }
```

- [ ] **Step 2: 修改modifySysMenu方法**

在 `BeanUtil.copyProperties(updateParam, sysMenuDO);` 之后添加：

```java
        // 处理关联权限（仅页面和按钮菜单）
        if (updateParam.getType() == 2 || updateParam.getType() == 3) {
            sysMenuDO.setPermId(updateParam.getPermId());
        } else {
            sysMenuDO.setPermId(null);
        }
```

- [ ] **Step 3: 提交**

```bash
git add blink-base/blink-base-app/src/main/java/com/blink/base/service/impl/SysMenuServiceImpl.java
git commit -m "feat(menu): 菜单服务实现关联权限逻辑

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 6: 后端 - 菜单Mapper JOIN权限表查询

**Files:**
- Modify: `blink-base/blink-base-app/src/main/resources/mapper/SysMenuMapper.xml`

- [ ] **Step 1: 修改findSysMenuListByRole查询**

将原有的 `select distinct sm.* from` 改为：

```xml
    <!-- 根据角色查询能访问的所有菜单-->
    <select id="findSysMenuListByRole" resultType="com.blink.vo.dto.com.blink.base.SysMenuVO">
        SELECT DISTINCT sm.*,
               sp.ac_identity as perm_identity,
               sp.ac_name as perm_name
        FROM sys_menu sm
        INNER JOIN sys_role_menu_rela srmr ON srmr.menu_id = sm.menu_id
        LEFT JOIN sys_permission sp ON sp.ac_id = sm.perm_id
        WHERE srmr.role_id IN
        <foreach collection="roleIds" item="roleId" index="index" open="(" close=")" separator=",">
            #{roleId}
        </foreach>
        AND sm.status = 0
        AND sm.delFlag = 0
        ORDER BY sm.parent_id ASC, sm.order_number ASC
    </select>
```

- [ ] **Step 2: 提交**

```bash
git add blink-base/blink-base-app/src/main/resources/mapper/SysMenuMapper.xml
git commit -m "feat(mapper): 菜单查询JOIN权限表获取权限标识

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 7: 后端 - 角色分配菜单时同步接口权限

**Files:**
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/mapper/SysRolePermRelaMapper.java`
- Modify: `blink-base/blink-base-app/src/main/resources/mapper/SysRolePermRelaMapper.xml`
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/service/impl/SysRoleServiceImpl.java`

- [ ] **Step 1: 在SysRolePermRelaMapper.java中添加方法**

```java
    /**
     * 删除角色的接口权限关联（ac_type=1）
     *
     * @param roleId 角色ID
     */
    void deleteApiPermissionsByRoleId(@Param("roleId") Integer roleId);
```

- [ ] **Step 2: 在SysRolePermRelaMapper.xml中添加SQL**

在 `</mapper>` 之前添加：

```xml
    <!-- 删除角色的接口权限关联 -->
    <delete id="deleteApiPermissionsByRoleId">
        DELETE srpr FROM sys_role_perm_rela srpr
        INNER JOIN sys_permission sp ON sp.ac_id = srpr.ac_id
        WHERE srpr.role_id = #{roleId} AND sp.ac_type = 1
    </delete>
```

- [ ] **Step 3: 修改SysRoleServiceImpl.assignMenus方法**

在 `roleMenuRelaMapper.delete(new LambdaQueryWrapper...` 之后，`batchInsertMenus` 之后添加：

```java
        // 提取菜单关联的接口权限ID
        List<Integer> permIds = Collections.emptyList();
        if (CollUtil.isNotEmpty(menuIds)) {
            List<SysMenuDO> menus = sysMenuMapper.selectByIds(menuIds);
            permIds = menus.stream()
                    .filter(menu -> ObjectUtil.isNotNull(menu.getPermId()))
                    .map(SysMenuDO::getPermId)
                      .distinct()
                      .toList();
        }

        // 删除原有的接口权限关联（仅 ac_type=1）
        rolePermRelaMapper.deleteApiPermissionsByRoleId(roleId);

        // 插入新的接口权限关联
        if (CollUtil.isNotEmpty(permIds)) {
            batchInsertPermissions(permIds, roleId);
            log.info("[SysRole] 分配菜单自动关联接口权限 | roleId: {}, permIds: {}", roleId, permIds);
        }
```

- [ ] **Step 4: 提交**

```bash
git add blink-base/blink-base-app/src/main/java/com/blink/base/mapper/SysRolePermRelaMapper.java blink-base/blink-base-app/src/main/resources/mapper/SysRolePermRelaMapper.xml blink-base/blink-base-app/src/main/java/com/blink/base/service/impl/SysRoleServiceImpl.java
git commit -m "feat(role): 分配菜单时自动同步接口权限

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 8: 后端 - 权限VO增加menuIds字段

**Files:**
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/dto/vo/SysPermissionVO.java`
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/service/impl/SysPermissionServiceImpl.java`
- Modify: `blink-base/blink-base-app/src/main/java/com/blink/base/mapper/SysMenuMapper.java`
- Modify: `blink-base/blink-base-app/src/main/resources/mapper/SysMenuMapper.xml`

- [ ] **Step 1: 修改SysPermissionVO.java**

在文件末尾 `}` 之前添加：

```java
    /**
     * 关联的菜单ID列表
     */
    private List<Integer> menuIds;
```

- [ ] **Step 2: 在SysMenuMapper.java中添加方法**

```java
    /**
     * 根据权限ID查询关联的菜单ID列表
     *
     * @param permId 权限ID
     * @return 菜单ID列表
     */
    List<Integer> findMenuIdsByPermId(@Param("permId") Integer permId);
```

- [ ] **Step 3: 在SysMenuMapper.xml中添加SQL**

在 `</mapper>` 之前添加：

```xml
    <!-- 根据权限ID查询关联的菜单ID列表 -->
    <select id="findMenuIdsByPermId" resultType="java.lang.Integer">
        SELECT menu_id FROM sys_menu WHERE perm_id = #{permId}
    </select>
```

- [ ] **Step 4: 在SysPermissionServiceImpl中增加查询逻辑**

在 `saveSysPermission` 方法返回之前，设置 menuIds：

```java
        // 查询关联的菜单ID列表
        List<Integer> menuIds = sysMenuMapper.findMenuIdsByPermId(sysPermissionDO.getAcId());
        permissionVO.setMenuIds(menuIds);
```

同样在 `modifySysPermission` 和 `getSysPermissionList` 方法中，查询权限详情时也要设置 menuIds。

- [ ] **Step 5: 提交**

```bash
git add blink-base/blink-base-app/src/main/java/com/blink/base/dto/vo/SysPermissionVO.java blink-base/blink-base-app/src/main/java/com/blink/base/service/impl/SysPermissionServiceImpl.java blink-base/blink-base-app/src/main/java/com/blink/base/mapper/SysMenuMapper.java blink-base/blink-base-app/src/main/resources/mapper/SysMenuMapper.xml
git commit -m "feat(permission): 权限VO增加menuIds字段用于编辑回显

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 9: 前端 - 类型定义更新

**Files:**
- Modify: `blink-base/blink-base-web/src/types/index.ts`

- [ ] **Step 1: 修改Menu接口**

在 `permId?: number` 之后添加：

```typescript
  permIdentity?: string  // 关联的权限标识
  permName?: string      // 关联的权限名称
```

- [ ] **Step 2: 提交**

```bash
git add blink-base/blink-base-web/src/types/index.ts
git commit -m "feat(types): Menu类型增加权限标识字段

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 10: 前端 - 权限API增加menuIds参数

**Files:**
- Modify: `blink-base/blink-base-web/src/api/permission.ts`

- [ ] **Step 1: 修改AddPermissionParams接口**

在 `dataFilterId?: number` 之后添加：

```typescript
  menuIds?: number[]
```

- [ ] **Step 2: 修改UpdatePermissionParams接口**

在 `dataFilterId?: number` 之后添加：

```typescript
  menuIds?: number[]
```

- [ ] **Step 3: 添加获取菜单树API**

在文件末尾添加：

```typescript
/**
 * 获取菜单树（用于权限关联选择）
 */
export const getMenuTreeForPermission = (): Promise<MenuInfo[]> => {
  return request.post('/sysMenu/getSysMenuList', { body: {} }) as Promise<MenuInfo[]>
}

export interface MenuInfo {
  menuId: number
  menuName: string
  type: number
  parentId: number
  children?: MenuInfo[]
}
```

- [ ] **Step 4: 提交**

```bash
git add blink-base/blink-base-web/src/api/permission.ts
git commit -m "feat(api): 权限API增加menuIds参数

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 11: 前端 - 菜单API增加permId参数

**Files:**
- Modify: `blink-base/blink-base-web/src/api/menu.ts`

- [ ] **Step 1: 修改AddMenuParams接口**

在 `componentPath?: string` 之后添加：

```typescript
  permId?: number
```

- [ ] **Step 2: 修改UpdateMenuParams接口**

在 `componentPath?: string` 之后添加：

```typescript
  permId?: number
```

- [ ] **Step 3: 添加获取接口权限API**

在文件末尾添加：

```typescript
/**
 * 获取接口权限列表（用于菜单关联选择）
 */
export const getApiPermissions = (): Promise<PermissionInfo[]> => {
  return request.post('/sysPermission/getAllApiPermission', { body: {} }) as Promise<PermissionInfo[]>
}

export interface PermissionInfo {
  acId: number
  acName: string
  acIdentity: string
  acType: number
  url: string
}
```

- [ ] **Step 4: 提交**

```bash
git add blink-base/blink-base-web/src/api/menu.ts
git commit -m "feat(api): 菜单API增加permId参数

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 12: 前端 - 权限表单弹窗增加关联菜单选择器

**Files:**
- Modify: `blink-base/blink-base-web/src/views/system/permission/components/PermissionFormDialog.vue`

- [ ] **Step 1: 添加关联菜单选择器（在接口权限URL输入框之后）**

在 `<el-form-item v-if="currentAcType === 1" :label="t('permission.url')"...>` 之后添加：

```vue
      <!-- 关联菜单选择器（仅接口权限显示） -->
      <el-form-item v-if="currentAcType === 1" :label="t('permission.relatedMenus')">
        <el-tree-select
          v-model="form.menuIds"
          :data="menuTreeData"
          :props="{ label: 'menuName', value: 'menuId', children: 'children', disabled: 'disabled' }"
          :placeholder="t('common.pleaseSelect')"
          multiple
          collapse-tags
          collapse-tags-tooltip
          clearable
          check-strictly
          :render-after-expand="false"
          style="width: 100%"
          :empty-text="t('common.noData')"
        />
      </el-form-item>
```

- [ ] **Step 2: 添加必要的import**

在 `<script setup>` 部分的 import 中添加：

```typescript
import { getMenuTreeForPermission, type MenuInfo } from '@/api/permission'
```

- [ ] **Step 3: 添加menuTreeData状态和处理逻辑**

在 `const dataFilterList = ref<DataFilterInfo[]>([])` 之后添加：

```typescript
const menuTreeData = ref<MenuInfo[]>([])
```

在 `const fetchDataFilterList` 方法之后添加：

```typescript
// 获取菜单树（用于接口权限关联）
const fetchMenuTree = async () => {
  try {
    const res = await getMenuTreeForPermission()
    // 过滤并处理菜单树：只保留页面和按钮，目录设为禁用
    const processMenuTree = (menus: MenuInfo[]): MenuInfo[] => {
      return menus.map(menu => ({
        ...menu,
        disabled: menu.type === 1, // 目录不可选
        children: menu.children ? processMenuTree(menu.children) : undefined
      }))
    }
    menuTreeData.value = processMenuTree(res || [])
  } catch {
    menuTreeData.value = []
  }
}
```

- [ ] **Step 4: 修改form reactive添加menuIds**

在 `const form = reactive({...})` 中添加：

```typescript
  menuIds: [] as number[],
```

- [ ] **Step 5: 修改handleSubmit方法**

在 `addPermission` 和 `updatePermission` 调用中添加 menuIds：

```typescript
// 在 addPermission 调用中
await addPermission({
  acName: form.acName,
  acEnName: form.acEnName || undefined,
  acIdentity: acIdentity,
  acType: acType,
  url: acType === 1 ? (form.url || undefined) : undefined,
  dataFilterId: acType === 2 ? form.dataFilterId : undefined,
  menuIds: acType === 1 ? form.menuIds : undefined,  // 新增
})

// 在 updatePermission 调用中
await updatePermission({
  acId: form.acId!,
  acName: form.acName,
  acEnName: form.acEnName || undefined,
  acIdentity: acIdentity,
  acType: acType,
  url: acType === 1 ? (form.url || undefined) : undefined,
  dataFilterId: acType === 2 ? form.dataFilterId : undefined,
  menuIds: acType === 1 ? form.menuIds : undefined,  // 新增
})
```

- [ ] **Step 6: 修改handleClose方法**

添加：

```typescript
  form.menuIds = []
```

- [ ] **Step 7: 修改watch**

在 `if (props.type === 'edit' && props.data)` 块中添加：

```typescript
        form.menuIds = props.data.menuIds || []
```

在 `fetchDataFilterList()` 调用之后添加：

```typescript
      fetchMenuTree()
```

- [ ] **Step 8: 提交**

```bash
git add blink-base/blink-base-web/src/views/system/permission/components/PermissionFormDialog.vue
git commit -m "feat(permission): 权限表单增加关联菜单选择器

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 13: 前端 - 菜单表单弹窗增加关联权限选择器

**Files:**
- Modify: `blink-base/blink-base-web/src/views/system/menu/components/MenuFormDialog.vue`

- [ ] **Step 1: 添加关联权限选择器（在状态选择器之后）**

在 `<el-form-item :label="t('common.status')"...>` 之后添加：

```vue
      <!-- 关联权限选择器（仅页面和按钮显示） -->
      <el-form-item v-if="form.type === 2 || form.type === 3" :label="t('menu.relatedPermission')">
        <el-select
          v-model="form.permId"
          :placeholder="t('common.pleaseSelect')"
          clearable
          filterable
          style="width: 100%"
        >
          <el-option
            v-for="perm in apiPermissions"
            :key="perm.acId"
            :label="`${perm.acIdentity} - ${perm.acName}`"
            :value="perm.acId"
          />
        </el-select>
      </el-form-item>
```

- [ ] **Step 2: 添加必要的import**

添加：

```typescript
import { getApiPermissions, type PermissionInfo } from '@/api/menu'
```

- [ ] **Step 3: 添加apiPermissions状态**

在 `const menuTreeData = ref<MenuInfo[]>([])` 之后添加：

```typescript
const apiPermissions = ref<PermissionInfo[]>([])
```

- [ ] **Step 4: 添加获取接口权限方法**

在 `const fetchMenuTree` 方法之后添加：

```typescript
// 获取接口权限列表
const fetchApiPermissions = async () => {
  try {
    const res = await getApiPermissions()
    apiPermissions.value = res || []
  } catch {
    apiPermissions.value = []
  }
}
```

- [ ] **Step 5: 修改form reactive添加permId**

在 `const form = reactive({...})` 中添加：

```typescript
  permId: undefined as number | undefined,
```

- [ ] **Step 6: 修改handleSubmit方法**

在 `addMenu` 和 `updateMenu` 调用中添加 permId：

```typescript
// 在 addMenu 调用中
await addMenu({
  menuName: form.menuName,
  menuEnName: form.menuEnName || undefined,
  type: form.type,
  icon: form.icon || undefined,
  url: form.url || undefined,
  componentPath: form.componentPath || undefined,
  orderNumber: form.orderNumber,
  status: form.status,
  parentId: form.parentId || 0,
  permId: (form.type === 2 || form.type === 3) ? form.permId : undefined,  // 新增
})

// 在 updateMenu 调用中
await updateMenu({
  menuId: form.menuId!,
  menuName: form.menuName,
  menuEnName: form.menuEnName || undefined,
  type: form.type,
  icon: form.icon || undefined,
  url: form.url || undefined,
  componentPath: form.componentPath || undefined,
  orderNumber: form.orderNumber,
  status: form.status,
  parentId: form.parentId,
  permId: (form.type === 2 || form.type === 3) ? form.permId : undefined,  // 新增
})
```

- [ ] **Step 7: 修改handleClose方法**

添加：

```typescript
  form.permId = undefined
```

- [ ] **Step 8: 修改watch**

在 `if (props.type === 'edit' && props.data)` 块中添加：

```typescript
        form.permId = props.data.permId || undefined
```

在 `fetchMenuTree()` 调用之后添加：

```typescript
      fetchApiPermissions()
```

- [ ] **Step 9: 提交**

```bash
git add blink-base/blink-base-web/src/views/system/menu/components/MenuFormDialog.vue
git commit -m "feat(menu): 菜单表单增加关联权限选择器

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 14: 前端 - 角色管理页面按钮文字修改

**Files:**
- Modify: `blink-base/blink-base-web/src/views/system/role/index.vue`
- Modify: `blink-base/blink-base-web/src/locales/zh-cn.ts`
- Modify: `blink-base/blink-base-web/src/locales/en-us.ts`

- [ ] **Step 1: 修改角色管理页面按钮文字**

将 `{{ t('role.assignPermission') }}` 改为 `{{ t('role.assignDataPermission') }}`

- [ ] **Step 2: 修改中文国际化文件**

在 `role:` 部分添加：

```typescript
  assignDataPermission: '分配数据权限',
```

- [ ] **Step 3: 修改英文国际化文件**

在 `role:` 部分添加：

```typescript
  assignDataPermission: 'Assign Data Permission',
```

- [ ] **Step 4: 提交**

```bash
git add blink-base/blink-base-web/src/views/system/role/index.vue blink-base/blink-base-web/src/locales/zh-cn.ts blink-base/blink-base-web/src/locales/en-us.ts
git commit -m "feat(role): 分配权限按钮改名为分配数据权限

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 15: 前端 - 分配权限弹窗仅显示数据权限

**Files:**
- Modify: `blink-base/blink-base-web/src/views/system/role/components/AssignPermissionDialog.vue`

- [ ] **Step 1: 移除接口权限Tab和相关逻辑**

修改模板，只保留数据权限Tab：

```vue
<template>
  <el-dialog
    :title="t('role.assignDataPermission')"
    v-model="visible"
    width="700px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    @closed="handleClose"
  >
    <div class="permission-content">
      <!-- 搜索栏 -->
      <div class="search-header">
        <el-input
          v-model.trim="searchKeyword"
          :placeholder="t('common.pleaseInput')"
          clearable
          style="width: 240px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <div class="selected-info">
          <el-tag type="success" size="small">
            {{ t('role.selectedCount') }}: {{ selectedDataPermissions.length }}
          </el-tag>
        </div>
      </div>

      <!-- 数据过滤权限表格 -->
      <el-table
        ref="dataTableRef"
        v-loading="loading"
        :data="filteredDataPermissions"
        stripe
        border
        max-height="400px"
        @selection-change="handleDataSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="acName" :label="t('permission.acName')" min-width="120" show-overflow-tooltip />
        <el-table-column prop="acIdentity" :label="t('permission.acIdentity')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.acIdentity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dataFilterName" :label="t('permission.dataFilterId')" min-width="140" show-overflow-tooltip />
      </el-table>
    </div>

    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>
```

- [ ] **Step 2: 简化script逻辑**

将整个 `<script setup>` 替换为：

```typescript
<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getPermissionList, type PermissionInfo } from '@/api/permission'
import { assignPermissions, getRoleDetail, type RoleInfo } from '@/api/role'

interface Props {
  modelValue: boolean
  role: RoleInfo | null
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue', 'success'])

const { t } = useI18n()

const dataTableRef = ref()
const loading = ref(false)
const submitting = ref(false)
const searchKeyword = ref('')

// 所有权限数据
const allPermissions = ref<PermissionInfo[]>([])

// 数据过滤权限（仅ac_type=2）
const dataPermissions = computed(() =>
  allPermissions.value.filter((p) => p.acType === 2)
)

// 筛选后的数据权限（支持搜索）
const filteredDataPermissions = computed(() => {
  if (!searchKeyword.value) return dataPermissions.value
  const keyword = searchKeyword.value.toLowerCase()
  return dataPermissions.value.filter(
    (p) =>
      p.acName?.toLowerCase().includes(keyword) ||
      p.acIdentity?.toLowerCase().includes(keyword) ||
      p.dataFilterName?.toLowerCase().includes(keyword)
  )
})

// 已选中的数据权限
const selectedDataPermissions = ref<PermissionInfo[]>([])

// 获取所有权限列表
const fetchAllPermissions = async () => {
  loading.value = true
  try {
    const res = await getPermissionList({ pageNum: 1, pageSize: 1000 })
    allPermissions.value = res.rows || []
  } finally {
    loading.value = false
  }
}

// 获取角色已分配的权限并设置选中状态
const fetchRolePermissions = async () => {
  if (!props.role?.roleId) return

  try {
    const detail = await getRoleDetail(props.role.roleId)
    const assignedIds = (detail.permissions || []).map((p) => p.acId)

    await nextTick()
    setTimeout(() => {
      dataPermissions.value.forEach((row) => {
        if (assignedIds.includes(row.acId)) {
          dataTableRef.value?.toggleRowSelection(row, true)
        }
      })
    }, 100)
  } catch {
    // ignore
  }
}

// 数据权限选择变化
const handleDataSelectionChange = (selection: PermissionInfo[]) => {
  selectedDataPermissions.value = selection
}

// 提交授权
const handleSubmit = async () => {
  if (!props.role?.roleId) return

  submitting.value = true
  try {
    const selectedIds = selectedDataPermissions.value.map((p) => p.acId)

    await assignPermissions({
      roleId: props.role.roleId,
      permissionIds: selectedIds,
    })
    ElMessage.success(t('message.success'))
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}

// 关闭弹窗时重置
const handleClose = () => {
  searchKeyword.value = ''
  selectedDataPermissions.value = []
  dataTableRef.value?.clearSelection()
}

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      fetchAllPermissions()
      fetchRolePermissions()
    }
  }
)
</script>
```

- [ ] **Step 3: 修改handleSubmit方法**

```typescript
const handleSubmit = async () => {
  if (!props.role?.roleId) return

  submitting.value = true
  try {
    const selectedIds = selectedDataPermissions.value.map((p) => p.acId)

    await assignPermissions({
      roleId: props.role.roleId,
      permissionIds: selectedIds,
    })
    ElMessage.success(t('message.success'))
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}
```

- [ ] **Step 4: 提交**

```bash
git add blink-base/blink-base-web/src/views/system/role/components/AssignPermissionDialog.vue
git commit -m "feat(role): 分配权限弹窗仅显示数据权限

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 16: 前端 - 国际化文案补充

**Files:**
- Modify: `blink-base/blink-base-web/src/locales/zh-cn.ts`
- Modify: `blink-base/blink-base-web/src/locales/en-us.ts`

- [ ] **Step 1: 添加权限相关中文文案**

在 `permission:` 部分添加：

```typescript
  relatedMenus: '关联菜单',
```

- [ ] **Step 2: 添加菜单相关中文文案**

在 `menu:` 部分添加：

```typescript
  relatedPermission: '关联权限',
```

- [ ] **Step 3: 添加权限相关英文文案**

在 `permission:` 部分添加：

```typescript
  relatedMenus: 'Related Menus',
```

- [ ] **Step 4: 添加菜单相关英文文案**

在 `menu:` 部分添加：

```typescript
  relatedPermission: 'Related Permission',
```

- [ ] **Step 5: 提交**

```bash
git add blink-base/blink-base-web/src/locales/zh-cn.ts blink-base/blink-base-web/src/locales/en-us.ts
git commit -m "feat(i18n): 补充权限菜单关联国际化文案

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 17: 构建和测试

- [ ] **Step 1: 构建后端**

```bash
cd D:/ideaProject/blink
./gradlew :blink-base:blink-base-app:build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 构建前端**

```bash
cd D:/ideaProject/blink/blink-base/blink-base-web
npm run build
```

Expected: Build completed without errors

- [ ] **Step 3: 启动后端验证**

启动 blink-base-app 应用，验证无启动错误。

- [ ] **Step 4: 启动前端验证**

```bash
cd D:/ideaProject/blink/blink-base/blink-base-web
npm run dev
```

访问页面验证功能正常。

---

## 验收标准

1. **权限管理页面**：新增/编辑权限时可以关联菜单，删除权限时自动解除关联
2. **菜单管理页面**：新增/编辑菜单时可以关联权限，列表显示关联权限名称
3. **角色管理页面**：分配菜单后自动获得关联的接口权限
4. **登录验证**：返回的菜单包含权限标识，permissions 数组正确
5. **前端权限**：按钮根据权限正确显示/隐藏