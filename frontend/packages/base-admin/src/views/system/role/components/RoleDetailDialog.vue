<template>
  <el-dialog
    :title="t('role.roleDetail')"
    v-model="visible"
    width="900px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    @closed="handleClose"
  >
    <div v-loading="loading" class="role-detail">
      <el-descriptions :title="t('role.basicInfo')" :column="2" border>
        <el-descriptions-item :label="t('role.roleName')">
          {{ roleDetail?.roleInfo?.roleName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('role.roleEnName')">
          {{ roleDetail?.roleInfo?.roleEnName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('role.roleCode')">
          {{ roleDetail?.roleInfo?.roleCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.status')">
          <el-tag v-if="roleDetail?.roleInfo?.status === 0" type="success">
            {{ t('common.statusEnable') }}
          </el-tag>
          <el-tag v-else type="danger">{{ t('common.statusDisable') }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('role.roleType')">
          <el-tag v-if="roleDetail?.roleInfo?.roleType === 1" type="primary">
            {{ t('role.typeSystem') }}
          </el-tag>
          <el-tag v-else type="info">{{ t('role.typeCustom') }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.createTime')">
          {{ roleDetail?.roleInfo?.createTime || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <el-tabs v-model="activeTab" class="detail-tabs">
        <el-tab-pane :label="t('role.permissionList')" name="permissions">
          <!-- 权限子页签 -->
          <el-tabs v-model="permissionSubTab" class="sub-tabs">
            <el-tab-pane :label="t('role.apiPermission')" name="api">
              <el-table :data="apiPermissions" stripe border max-height="280">
                <el-table-column prop="acName" :label="t('permission.acName')" min-width="120" />
                <el-table-column
                  prop="acIdentity"
                  :label="t('permission.acIdentity')"
                  min-width="140"
                />
                <el-table-column prop="url" label="URL" min-width="180" />
              </el-table>
              <el-empty v-if="!apiPermissions.length" :description="t('common.noData')" />
            </el-tab-pane>
            <el-tab-pane :label="t('role.dataPermission')" name="data">
              <el-table :data="dataPermissions" stripe border max-height="280">
                <el-table-column prop="acName" :label="t('permission.acName')" min-width="120" />
                <el-table-column
                  prop="acIdentity"
                  :label="t('permission.acIdentity')"
                  min-width="140"
                />
                <el-table-column
                  prop="dataFilterName"
                  :label="t('permission.dataFilterId')"
                  min-width="140"
                />
              </el-table>
              <el-empty v-if="!dataPermissions.length" :description="t('common.noData')" />
            </el-tab-pane>
          </el-tabs>
        </el-tab-pane>

        <el-tab-pane :label="t('role.menuList')" name="menus">
          <el-table
            :data="menuTreeData"
            stripe
            border
            max-height="400"
            row-key="menuId"
            :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
            default-expand-all
          >
            <el-table-column prop="menuName" :label="t('menu.menuName')" min-width="180" />
            <el-table-column prop="url" label="URL" min-width="150" />
            <el-table-column prop="type" :label="t('menu.type')" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.type === 1" type="info">{{ t('menu.typeDirectory') }}</el-tag>
                <el-tag v-else-if="row.type === 2" type="primary">{{ t('menu.typeMenu') }}</el-tag>
                <el-tag v-else-if="row.type === 3" type="warning">
                  {{ t('menu.typeButton') }}
                </el-tag>
                <el-tag v-else type="info">-</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" :label="t('common.status')" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.status === 0" type="success">{{ t('common.show') }}</el-tag>
                <el-tag v-else type="info">{{ t('common.hide') }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!roleDetail?.menus?.length" :description="t('common.noData')" />
        </el-tab-pane>

        <el-tab-pane :label="t('role.userList')" name="users">
          <el-table :data="roleDetail?.users || []" stripe border max-height="300">
            <el-table-column prop="loginName" :label="t('user.loginName')" min-width="120" />
            <el-table-column prop="username" :label="t('user.username')" min-width="120" />
            <el-table-column prop="phone" :label="t('user.phone')" min-width="120" />
            <el-table-column prop="email" :label="t('user.email')" min-width="150" />
            <el-table-column prop="locked" :label="t('user.locked')" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.locked === 0" type="success">{{ t('user.unlocked') }}</el-tag>
                <el-tag v-else type="danger">{{ t('user.locked') }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!roleDetail?.users?.length" :description="t('common.noData')" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <template #footer>
      <el-button @click="visible = false">{{ t('common.close') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { getRoleDetail, type RoleDetailRsp, type RoleInfo, type MenuInfo } from '@/api/role'

interface Props {
  modelValue: boolean
  role: RoleInfo | null
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue'])

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const loading = ref(false)
const activeTab = ref('permissions')
const permissionSubTab = ref<'api' | 'data'>('api')
const roleDetail = ref<RoleDetailRsp | null>(null)

// 接口权限列表（ac_type=1）
const apiPermissions = computed(() =>
  (roleDetail.value?.permissions || []).filter((p) => p.acType === 1)
)

// 数据权限列表（ac_type=2）
const dataPermissions = computed(() =>
  (roleDetail.value?.permissions || []).filter((p) => p.acType === 2)
)

/**
 * 构建菜单树形数据
 */
const menuTreeData = computed(() => {
  const menus = roleDetail.value?.menus || []
  if (!menus.length) return []

  // 创建菜单映射
  const menuMap = new Map<number, MenuInfo>()
  menus.forEach((menu) => {
    menuMap.set(menu.menuId, { ...menu, children: [] })
  })

  // 构建树形结构
  const tree: MenuInfo[] = []
  menuMap.forEach((menu) => {
    const parentId = menu.parentId
    if (!parentId || parentId === 0) {
      // 根节点
      tree.push(menu)
    } else {
      // 子节点
      const parent = menuMap.get(parentId)
      if (parent) {
        // 父节点在列表中，添加为子节点
        if (!parent.children) {
          parent.children = []
        }
        parent.children.push(menu)
      } else {
        // 父节点不在列表中，作为根节点显示
        tree.push(menu)
      }
    }
  })

  return tree
})

const fetchRoleDetail = async () => {
  if (!props.role?.roleId) return

  loading.value = true
  try {
    roleDetail.value = await getRoleDetail(props.role.roleId)
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  roleDetail.value = null
  activeTab.value = 'permissions'
  permissionSubTab.value = 'api'
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      fetchRoleDetail()
    }
  }
)
</script>

<style scoped lang="scss">
.role-detail {
  .detail-tabs {
    margin-top: 20px;

    .sub-tabs {
      :deep(.el-tabs__header) {
        margin-bottom: 12px;
      }

      :deep(.el-tabs__item) {
        font-size: 13px;
      }
    }
  }
}
</style>
