<template>
  <el-dialog
    :title="t('system.user.viewRolePermission')"
    v-model="visible"
    width="900px"
    :close-on-click-modal="false"
    :lock-scroll="false"
  >
    <div v-loading="loading" class="permission-content">
      <!-- 角色列表 -->
      <div class="section">
        <div class="section-header">
          <el-icon><User /></el-icon>
          <span>{{ t('system.user.roles') }}</span>
          <el-tag type="info" size="small">{{ roles.length }}</el-tag>
        </div>
        <div class="section-body">
          <el-tag v-for="role in roles" :key="role.roleId" type="primary" class="role-tag">
            {{ role.roleName }}
          </el-tag>
          <el-empty v-if="roles.length === 0" :description="t('common.noData')" :image-size="60" />
        </div>
      </div>

      <!-- 菜单树 -->
      <div class="section">
        <div class="section-header">
          <el-icon><Menu /></el-icon>
          <span>{{ t('system.user.menus') }}</span>
          <el-tag type="info" size="small">{{ menuCount }}</el-tag>
        </div>
        <div class="section-body menu-body">
          <el-tree
            :data="menuTree"
            :props="{ label: 'menuName', children: 'children' }"
            default-expand-all
            :expand-on-click-node="false"
          >
            <template #default="{ data }">
              <div class="tree-node">
                <span class="node-label">{{ data.menuName }}</span>
                <el-tag v-if="data.type === 1" type="primary" size="small">
                  {{ t('menu.typeDirectory') }}
                </el-tag>
                <el-tag v-else-if="data.type === 2" type="success" size="small">
                  {{ t('menu.typeMenu') }}
                </el-tag>
                <el-tag v-else-if="data.type === 3" type="warning" size="small">
                  {{ t('menu.typeButton') }}
                </el-tag>
              </div>
            </template>
          </el-tree>
          <el-empty
            v-if="menuTree.length === 0"
            :description="t('common.noData')"
            :image-size="60"
          />
        </div>
      </div>

      <!-- 权限列表 -->
      <div class="section">
        <div class="section-header">
          <el-icon><Key /></el-icon>
          <span>{{ t('system.user.permissions') }}</span>
          <el-tag type="info" size="small">{{ permissions.length }}</el-tag>
        </div>
        <div class="section-body permission-body">
          <el-table :data="permissions" stripe border height="100%" size="small">
            <el-table-column
              prop="acName"
              :label="t('system.permission.acName')"
              min-width="120"
              show-overflow-tooltip
            />
            <el-table-column
              prop="acIdentity"
              :label="t('system.permission.acIdentity')"
              min-width="180"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <el-tag type="info" size="small">{{ row.acIdentity }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="acType" :label="t('system.permission.acType')" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.acType === 1" type="primary" size="small">
                  {{ t('system.permission.acTypeApi') }}
                </el-tag>
                <el-tag v-else-if="row.acType === 2" type="success" size="small">
                  {{ t('system.permission.acTypeData') }}
                </el-tag>
                <el-tag v-else type="info" size="small">-</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty
            v-if="permissions.length === 0"
            :description="t('common.noData')"
            :image-size="60"
          />
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">{{ t('common.close') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { User, Menu, Key } from '@element-plus/icons-vue'
import {
  getUserPermissions,
  type UserPermissionRsp,
  type RoleInfo,
  type MenuInfo,
  type PermissionInfo,
} from '@/api/user'

interface Props {
  modelValue: boolean
  userId: number | null
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue'])

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const loading = ref(false)
const roles = ref<RoleInfo[]>([])
const menus = ref<MenuInfo[]>([])
const permissions = ref<PermissionInfo[]>([])

// 菜单总数
const menuCount = computed(() => {
  const count = (list: MenuInfo[]): number => {
    let total = 0
    for (const item of list) {
      total += 1
      if (item.children && item.children.length > 0) {
        total += count(item.children)
      }
    }
    return total
  }
  return count(menus.value)
})

// 构建菜单树
const menuTree = computed(() => {
  const buildTree = (list: MenuInfo[], parentId: number | null = null): MenuInfo[] => {
    const result: MenuInfo[] = []
    for (const item of list) {
      if (item.parentId === parentId || (parentId === null && !item.parentId)) {
        const children = buildTree(list, item.menuId)
        result.push({
          ...item,
          children: children.length > 0 ? children : undefined,
        })
      }
    }
    return result.sort((a, b) => (a.orderNumber || 0) - (b.orderNumber || 0))
  }
  return buildTree(menus.value)
})

const fetchData = async () => {
  if (!props.userId) return

  loading.value = true
  try {
    const res: UserPermissionRsp = await getUserPermissions(props.userId)
    roles.value = res.roles || []
    menus.value = res.menus || []
    permissions.value = res.permissions || []
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (val) => {
    if (val && props.userId) {
      fetchData()
    }
  }
)
</script>

<style scoped lang="scss">
.permission-content {
  .section {
    margin-bottom: 20px;

    &:last-child {
      margin-bottom: 0;
    }

    .section-header {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      background: linear-gradient(90deg, rgba(59, 130, 246, 0.08) 0%, transparent 100%);
      border-radius: 6px 6px 0 0;
      border: 1px solid var(--el-border-color-lighter);
      border-bottom: none;
      font-weight: 500;
      color: var(--el-text-color-primary);

      .el-icon {
        font-size: 18px;
        color: var(--el-color-primary);
      }
    }

    .section-body {
      padding: 12px 16px;
      border: 1px solid var(--el-border-color-lighter);
      border-radius: 0 0 6px 6px;
      max-height: 300px;
      overflow: auto;

      &.menu-body {
        max-height: 400px;
      }

      &.permission-body {
        height: 300px;
        padding: 12px;
        overflow: hidden;
      }
    }

    .role-tag {
      margin-right: 8px;
      margin-bottom: 8px;
    }

    .tree-node {
      display: flex;
      align-items: center;
      gap: 8px;

      .node-label {
        flex: 1;
      }
    }
  }
}
</style>
