<template>
  <el-dialog
    :title="t('system.user.assignRole')"
    v-model="visible"
    width="900px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    @closed="handleClose"
    class="assign-role-dialog"
  >
    <div v-loading="loading" class="dialog-content">
      <div class="selected-info">
        <span class="info-label">{{ t('system.user.selectedUsers') }}:</span>
        <div class="user-tags">
          <el-tag v-for="user in users" :key="user.userId" class="user-tag" type="primary" effect="plain">
            {{ user.username || user.loginName }}
          </el-tag>
        </div>
      </div>

      <el-divider border-style="dashed" />

      <el-form :model="form" label-width="100px" class="role-form">
        <el-form-item :label="t('system.user.selectRole')">
          <el-select
            v-model="form.roleIdList"
            multiple
            :placeholder="t('common.pleaseSelect')"
            style="width: 100%"
            @change="handleRoleChange"
          >
            <el-option
              v-for="role in filteredRoleList"
              :key="role.roleId"
              :label="role.roleName"
              :value="role.roleId"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <el-divider border-style="dashed" />

      <div v-if="selectedRoleDetails.length > 0" class="role-details">
        <el-tabs v-model="activeDetailTab" type="border-card" class="role-tabs">
          <el-tab-pane
            v-for="detail in selectedRoleDetails"
            :key="detail.roleInfo.roleId"
            :label="detail.roleInfo.roleName"
            :name="String(detail.roleInfo.roleId)"
          >
            <div class="role-detail-content">
              <div class="detail-section">
                <div class="section-header">
                  <el-icon><Key /></el-icon>
                  <span>{{ t('system.user.permissionList') }}</span>
                  <el-tag size="small" type="info">{{ detail.permissions?.length || 0 }}</el-tag>
                </div>
                <div class="section-content">
                  <el-table
                    v-if="detail.permissions && detail.permissions.length > 0"
                    :data="detail.permissions"
                    stripe
                    size="small"
                    max-height="200"
                  >
                    <el-table-column prop="acName" :label="t('system.permission.acName')" min-width="150" />
                    <el-table-column prop="acIdentity" :label="t('system.permission.acIdentity')" min-width="150" />
                    <el-table-column prop="acType" :label="t('system.permission.acType')" width="100" align="center">
                      <template #default="{ row }">
                        <el-tag v-if="row.acType === 1" type="primary" size="small">{{ t('system.permission.typePage') }}</el-tag>
                        <el-tag v-else-if="row.acType === 2" type="success" size="small">{{ t('system.permission.typeButton') }}</el-tag>
                        <el-tag v-else type="info" size="small">{{ row.acType }}</el-tag>
                      </template>
                    </el-table-column>
                  </el-table>
                  <el-empty v-else :description="t('common.noData')" :image-size="60" />
                </div>
              </div>

              <div class="detail-section">
                <div class="section-header">
                  <el-icon><Menu /></el-icon>
                  <span>{{ t('system.user.menuTree') }}</span>
                  <el-tag size="small" type="info">{{ detail.menus?.length || 0 }}</el-tag>
                </div>
                <div class="section-content">
                  <el-tree
                    v-if="detail.menus && detail.menus.length > 0"
                    :data="buildMenuTree(detail.menus)"
                    :props="menuTreeProps"
                    node-key="menuId"
                    default-expand-all
                    highlight-current
                    class="menu-tree"
                  >
                    <template #default="{ node, data }">
                      <span class="custom-tree-node">
                        <span class="node-label">{{ node.label }}</span>
                        <el-tag v-if="data.type === 1" size="small" type="info" class="tree-tag">{{ t('system.menu.typeDirectory') }}</el-tag>
                        <el-tag v-else-if="data.type === 2" size="small" type="success" class="tree-tag">{{ t('system.menu.typeMenu') }}</el-tag>
                        <el-tag v-else-if="data.type === 3" size="small" type="primary" class="tree-tag">{{ t('system.menu.typeButton') }}</el-tag>
                      </span>
                    </template>
                  </el-tree>
                  <el-empty v-else :description="t('common.noData')" :image-size="60" />
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
      <el-empty v-else :description="t('system.user.pleaseSelectRoleToView')" :image-size="80" />
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="isSubmitting" @click="handleSubmit">
          {{ t('common.confirm') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Key, Menu } from '@element-plus/icons-vue'
import { assignUserRoles, type UserInfo } from '@/api/user'
import { getAllRoles, getRoleDetail, type RoleInfo, type RoleDetailRsp } from '@/api/role'
import { useUserStore } from '@/stores/user'
import { useSubmitGuard } from '@/composables/useSubmitGuard'

interface Props {
  modelValue: boolean
  users: UserInfo[]
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue', 'success'])

const { t } = useI18n()
const userStore = useUserStore()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const loading = ref(false)
const { isSubmitting, submitGuard } = useSubmitGuard()
const roleList = ref<RoleInfo[]>([])
const selectedRoleDetails = ref<RoleDetailRsp[]>([])
const activeDetailTab = ref('')

const SUPER_ADMIN_ROLE_ID = 1
const SUPER_ADMIN_ROLE_CODE = 'superAdmin'

const isSuperAdmin = computed(() => {
  return userStore.roles.includes(SUPER_ADMIN_ROLE_CODE)
})

const filteredRoleList = computed(() => {
  if (isSuperAdmin.value) {
    return roleList.value
  }
  return roleList.value.filter(role => role.roleId !== SUPER_ADMIN_ROLE_ID)
})

const menuTreeProps = {
  children: 'children',
  label: 'menuName',
}

const form = ref({
  roleIdList: [] as number[],
})

const fetchRoleList = async () => {
  loading.value = true
  try {
    roleList.value = await getAllRoles()
  } finally {
    loading.value = false
  }
}

const fetchRoleDetail = async (roleId: number): Promise<RoleDetailRsp | null> => {
  try {
    const detail = await getRoleDetail(roleId)
    return detail
  } catch {
    return null
  }
}

const handleRoleChange = async (newRoleIds: number[]) => {
  const existingRoleIds = selectedRoleDetails.value.map(d => d.roleInfo.roleId)

  const toAdd = newRoleIds.filter(id => !existingRoleIds.includes(id))
  const toRemove = existingRoleIds.filter(id => !newRoleIds.includes(id))

  if (toRemove.length > 0) {
    selectedRoleDetails.value = selectedRoleDetails.value.filter(d => !toRemove.includes(d.roleInfo.roleId))
  }

  for (const roleId of toAdd) {
    const detail = await fetchRoleDetail(roleId)
    if (detail) {
      selectedRoleDetails.value.push(detail)
    }
  }

  await nextTick()

  if (newRoleIds.length > 0) {
    if (!newRoleIds.includes(Number(activeDetailTab.value))) {
      activeDetailTab.value = String(newRoleIds[0])
    }
  } else {
    activeDetailTab.value = ''
  }
}

const buildMenuTree = (menus: any[]) => {
  if (!menus || menus.length === 0) {
    return []
  }

  const menuMap = new Map<number, any>()
  const rootMenus: any[] = []

  menus.forEach(menu => {
    menuMap.set(menu.menuId, { ...menu, children: [] })
  })

  menus.forEach(menu => {
    const menuItem = menuMap.get(menu.menuId)
    if (menuItem) {
      if (menu.parentId === 0 || !menuMap.has(menu.parentId)) {
        rootMenus.push(menuItem)
      } else {
        const parent = menuMap.get(menu.parentId)
        if (parent) {
          parent.children.push(menuItem)
        }
      }
    }
  })

  return rootMenus
}

const handleSubmit = async () => {
  if (props.users.length === 0) {
    ElMessage.warning(t('system.user.selectUser'))
    return
  }

  if (!isSuperAdmin.value && form.value.roleIdList.includes(SUPER_ADMIN_ROLE_ID)) {
    ElMessage.warning(t('system.user.onlySuperAdminCanAssign'))
    return
  }

  await submitGuard(async () => {
    await assignUserRoles({
      userIdList: props.users.map((u) => u.userId),
      roleIdList: form.value.roleIdList,
    })
    ElMessage.success(t('message.success'))
    emit('success')
    visible.value = false
  })
}

const handleClose = () => {
  form.value.roleIdList = []
  selectedRoleDetails.value = []
  activeDetailTab.value = ''
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      fetchRoleList()
    }
  }
)
</script>

<style scoped lang="scss">
.assign-role-dialog {
  .dialog-content {
    min-height: 300px;
  }

  .selected-info {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 12px 16px;
    background: var(--bg-color);
    border-radius: 8px;
    border: 1px solid var(--border-color-light);

    .info-label {
      font-weight: 500;
      color: var(--text-color-primary);
      white-space: nowrap;
      line-height: 24px;
    }

    .user-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      flex: 1;
    }

    .user-tag {
      margin: 0;
    }
  }

  .role-form {
    padding: 0 16px;
  }

  .role-details {
    .role-tabs {
      border-radius: 8px;
      border: 1px solid var(--border-color-light);
      overflow: hidden;

      :deep(.el-tabs__header) {
        background: var(--bg-color);
        border-bottom: 1px solid var(--border-color-light);
        margin: 0;
      }

      :deep(.el-tabs__item) {
        height: 40px;
        line-height: 40px;
        color: var(--text-color-regular);

        &.is-active {
          color: var(--primary-color);
          background: var(--card-bg);
        }

        &:hover {
          color: var(--primary-color);
        }
      }

      :deep(.el-tabs__content) {
        padding: 0;
      }
    }

    .role-detail-content {
      display: flex;
      flex-direction: column;
      gap: 20px;
      padding: 16px;
      background: var(--card-bg);

      .detail-section {
        .section-header {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 10px 12px;
          background: var(--bg-color);
          border-radius: 6px;
          margin-bottom: 12px;
          border-left: 3px solid var(--primary-color);

          .el-icon {
            color: var(--primary-color);
            font-size: 16px;
          }

          span {
            font-weight: 600;
            color: var(--text-color-primary);
            font-size: 14px;
          }
        }

        .section-content {
          padding: 0 4px;
          max-height: 250px;
          overflow-y: auto;

          &::-webkit-scrollbar {
            width: 4px;
          }

          &::-webkit-scrollbar-thumb {
            background: var(--border-color-base);
            border-radius: 2px;
          }
        }
      }
    }
  }

  .menu-tree {
    background: transparent;

    :deep(.el-tree-node__content) {
      height: 32px;
      border-radius: 4px;

      &:hover {
        background: var(--table-row-hover);
      }
    }

    :deep(.el-tree-node.is-current > .el-tree-node__content) {
      background: var(--table-row-hover);
    }
  }

  .custom-tree-node {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-right: 8px;
    font-size: 13px;

    .node-label {
      color: var(--text-color-primary);
    }

    .tree-tag {
      margin-left: 8px;
    }
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }

  :deep(.el-divider) {
    margin: 16px 0;
    border-color: var(--border-color-light);
  }

  :deep(.el-empty) {
    padding: 40px 0;
  }
}
</style>