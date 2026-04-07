<template>
  <div class="menu-management">
    <div class="left-panel">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <span>{{ t('menu.title') }}</span>
            <div class="header-buttons">
              <AuthButton :perm="ButtonPerms.Menu.Add" type="success" size="small" @click="handleAdd(null)">
                <el-icon><Plus /></el-icon>{{ t('common.add') }}
              </AuthButton>
              <el-button type="info" size="small" @click="fetchMenuList">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </div>
        </template>
        <div class="tree-wrapper">
          <el-tree
            ref="treeRef"
            v-loading="loading"
            :data="menuList"
            :props="defaultProps"
            :highlight-current="true"
            :expand-on-click-node="false"
            node-key="menuId"
            :default-expanded-keys="expandedKeys"
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <div class="tree-node">
                <div class="node-content">
                  <BlinkIcon v-if="data.icon" :icon="data.icon" size="16" class="node-icon" />
                  <span class="node-label">{{ node.label }}</span>
                  <el-tag v-if="data.type === 1" type="primary" size="small" class="node-tag">{{ t('menu.typeDirectory') }}</el-tag>
                  <el-tag v-else-if="data.type === 2" type="success" size="small" class="node-tag">{{ t('menu.typeMenu') }}</el-tag>
                  <el-tag v-else-if="data.type === 3" type="warning" size="small" class="node-tag">{{ t('menu.typeButton') }}</el-tag>
                </div>
                <div class="node-actions">
                  <AuthButton :perm="ButtonPerms.Menu.Edit" type="primary" link size="small" @click.stop="handleEdit(data)">
                    <el-icon><Edit /></el-icon>
                  </AuthButton>
                  <AuthButton v-if="data.type !== 3" :perm="ButtonPerms.Menu.Add" type="success" link size="small" @click.stop="handleAdd(data)">
                    <el-icon><Plus /></el-icon>
                  </AuthButton>
                  <AuthButton :perm="ButtonPerms.Menu.Delete" type="danger" link size="small" @click.stop="handleDelete(data)">
                    <el-icon><Delete /></el-icon>
                  </AuthButton>
                </div>
              </div>
            </template>
          </el-tree>
        </div>
      </el-card>
    </div>

    <div class="right-panel">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <span>{{ currentMenu ? t('menu.detail') : t('menu.selectMenu') }}</span>
          </div>
        </template>

        <transition name="fade-slide" mode="out-in">
          <div v-if="currentMenu" class="menu-detail card-content-fade" :key="currentMenu.menuId">
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="t('menu.menuName')">
                {{ currentMenu.menuName }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('menu.menuEnName')">
                {{ currentMenu.menuEnName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('menu.type')">
                <el-tag v-if="currentMenu.type === 1" type="primary">{{ t('menu.typeDirectory') }}</el-tag>
                <el-tag v-else-if="currentMenu.type === 2" type="success">{{ t('menu.typeMenu') }}</el-tag>
                <el-tag v-else-if="currentMenu.type === 3" type="warning">{{ t('menu.typeButton') }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item :label="t('menu.icon')">
                <BlinkIcon v-if="currentMenu.icon" :icon="currentMenu.icon" size="18" class="detail-icon" />
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item :label="t('menu.url')">
                {{ currentMenu.url || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('menu.componentPath')">
                {{ currentMenu.componentPath || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('menu.permName')">
                {{ currentMenu.permName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('menu.permIdentity')">
                {{ currentMenu.permIdentity || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('menu.orderNumber')">
                {{ currentMenu.orderNumber || 0 }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('common.status')">
                <el-tag v-if="currentMenu.status === 0" type="success">{{ t('menu.statusShow') }}</el-tag>
                <el-tag v-else type="info">{{ t('menu.statusHide') }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item :label="t('menu.createTime')">
                {{ currentMenu.createTime || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('menu.updateTime')">
                {{ currentMenu.updateTime || '-' }}
              </el-descriptions-item>
            </el-descriptions>

            <div class="detail-actions">
              <AuthButton :perm="ButtonPerms.Menu.Edit" type="primary" @click="handleEdit(currentMenu)">
                <el-icon><Edit /></el-icon>{{ t('common.edit') }}
              </AuthButton>
              <AuthButton v-if="currentMenu.type !== 3" :perm="ButtonPerms.Menu.Add" type="success" @click="handleAdd(currentMenu)">
                <el-icon><Plus /></el-icon>{{ t('menu.addChild') }}
              </AuthButton>
              <AuthButton :perm="ButtonPerms.Menu.Delete" type="danger" @click="handleDelete(currentMenu)">
                <el-icon><Delete /></el-icon>{{ t('common.delete') }}
              </AuthButton>
            </div>
          </div>

          <el-empty v-else :description="t('menu.selectMenuHint')" :key="'empty'" />
        </transition>
      </el-card>
    </div>

    <MenuFormDialog
      v-model="formDialogVisible"
      :type="formType"
      :data="currentMenu"
      :parent-menu="parentMenu"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import { getMenuList, deleteMenu, checkMenuRoleAssignment, type MenuInfo } from '@/api/menu'
import { ButtonPerms } from '@/composables/usePermission'
import AuthButton from '@/components/AuthButton.vue'

defineOptions({
  name: 'SystemMenu',
})
import MenuFormDialog from './components/MenuFormDialog.vue'

const { t } = useI18n()

const treeRef = ref()
const loading = ref(false)
const menuList = ref<MenuInfo[]>([])
const expandedKeys = ref<number[]>([])
const formDialogVisible = ref(false)
const formType = ref<'add' | 'edit'>('add')
const currentMenu = ref<MenuInfo | null>(null)
const parentMenu = ref<MenuInfo | null>(null)

const defaultProps = {
  children: 'children',
  label: 'menuName',
  value: 'menuId',
}

const fetchMenuList = async () => {
  loading.value = true

  try {
    const res = await getMenuList()

    // 后端返回的是 { rows: [...], total: ... } 结构
    // rows 已经是树形结构
    if (res?.rows) {
      menuList.value = res.rows
    } else if (Array.isArray(res)) {
      menuList.value = res
    } else {
      menuList.value = []
    }

    // 设置默认展开一级节点（根节点）
    expandedKeys.value = menuList.value.map(item => item.menuId)

    // 如果有当前选中的菜单，需要从新数据中找到并更新
    if (currentMenu.value) {
      const updatedMenu = findMenuById(menuList.value, currentMenu.value.menuId)
      if (updatedMenu) {
        currentMenu.value = updatedMenu
        // 展开选中菜单的父节点，确保树形结构能正确显示
        if (updatedMenu.parentId && updatedMenu.parentId > 0) {
          expandedKeys.value = [...expandedKeys.value, updatedMenu.parentId]
        }
      }
    }

  } catch (error) {
    menuList.value = []
  } finally {
    loading.value = false
  }
}

// 递归查找菜单
const findMenuById = (menus: MenuInfo[], menuId: number): MenuInfo | null => {
  for (const menu of menus) {
    if (menu.menuId === menuId) {
      return menu
    }
    if (menu.children && menu.children.length > 0) {
      const found = findMenuById(menu.children, menuId)
      if (found) return found
    }
  }
  return null
}

const handleNodeClick = (data: MenuInfo) => {
  currentMenu.value = data
}

const handleAdd = (row: MenuInfo | null) => {
  formType.value = 'add'
  parentMenu.value = row
  formDialogVisible.value = true
}

const handleEdit = (row: MenuInfo) => {
  formType.value = 'edit'
  currentMenu.value = row
  parentMenu.value = null
  formDialogVisible.value = true
}

const handleDelete = async (row: MenuInfo) => {
  try {
    // 先检查菜单是否已分配给角色
    const checkResult = await checkMenuRoleAssignment({ menuId: row.menuId })

    // 如果已分配给角色，显示角色信息并确认
    if (checkResult.assigned && checkResult.roles && checkResult.roles.length > 0) {
      const roleNames = checkResult.roles.map(r => r.roleName).join('、')
      const confirmMsg = t('menu.deleteConfirmWithRoles', { roles: roleNames })

      await ElMessageBox.confirm(confirmMsg, t('message.tips'), {
        type: 'warning',
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
      })
    } else {
      await ElMessageBox.confirm(t('menu.deleteConfirm'), t('message.tips'), {
        type: 'warning',
      })
    }

    await deleteMenu({ deleteId: row.menuId, batchDelete: false })
    ElMessage.success(t('message.deleteSuccess'))
    currentMenu.value = null
    fetchMenuList()
  } catch {
    // 取消删除
  }
}

// 处理新增/编辑成功后的回调
const handleFormSuccess = async (parentId?: number) => {
  // 记录需要展开的父节点ID
  const expandParentId = parentId ?? (parentMenu.value?.menuId ?? 0)

  await fetchMenuList()

  // 如果是新增子菜单，展开父节点
  if (expandParentId > 0 && !expandedKeys.value.includes(expandParentId)) {
    expandedKeys.value = [...expandedKeys.value, expandParentId]
  }
}

onMounted(() => {
  fetchMenuList()
})
</script>

<style scoped lang="scss">
.menu-management {
  display: flex;
  gap: 16px;
  height: calc(100vh - 120px);
  min-height: 500px;

  .left-panel {
    width: 400px;
    flex-shrink: 0;

    :deep(.el-card) {
      height: 100%;
      display: flex;
      flex-direction: column;

      .el-card__header {
        padding: 0;
      }

      .el-card__body {
        flex: 1;
        overflow: auto;
        padding: 12px;
      }
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid var(--border-color-light);
      font-weight: 500;
      color: var(--text-color-primary);

      .header-buttons {
        display: flex;
        gap: 8px;
      }
    }

    .tree-wrapper {
      min-height: 200px;
    }

    .tree-node {
      display: flex;
      justify-content: space-between;
      align-items: center;
      width: 100%;
      padding-right: 8px;

      .node-content {
        display: flex;
        align-items: center;
        gap: 8px;
        flex: 1;
        min-width: 0;

        .node-icon {
          font-size: 16px;
          color: var(--primary-color);
          flex-shrink: 0;
        }

        .detail-icon {
          font-size: 18px;
          color: var(--primary-color);
        }

        .node-label {
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .node-tag {
          flex-shrink: 0;
          margin-left: 4px;
        }
      }

      .node-actions {
        display: flex;
        align-items: center;
        opacity: 0;
        transition: opacity 0.2s;

        .el-button {
          padding: 2px 4px;
        }
      }

      &:hover .node-actions {
        opacity: 1;
      }
    }
  }

  .right-panel {
    flex: 1;
    min-width: 0;

    :deep(.el-card) {
      height: 100%;
      display: flex;
      flex-direction: column;

      .el-card__header {
        padding: 0;
      }

      .el-card__body {
        flex: 1;
        overflow: auto;
        padding: 16px;
      }
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid var(--border-color-light);
      font-weight: 500;
      color: var(--text-color-primary);
    }

    .menu-detail {
      display: flex;
      flex-direction: column;
      gap: 24px;

      :deep(.el-descriptions) {
        .el-descriptions__label {
          width: 120px;
        }
      }

      .detail-actions {
        display: flex;
        gap: 12px;
        padding-top: 16px;
        border-top: 1px solid var(--border-color-light);
      }
    }
  }
}

/* 过渡动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease-out;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>