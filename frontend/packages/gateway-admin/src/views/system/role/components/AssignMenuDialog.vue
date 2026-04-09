<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('system.role.assignMenuTitle')"
    width="500px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
    @closed="handleClose"
  >
    <div class="menu-tree">
      <div class="tree-header">
        <el-checkbox
          v-model="checkAll"
          :indeterminate="isIndeterminate"
          @change="handleCheckAll"
        >
          {{ t('common.selectAll') }}
        </el-checkbox>
        <div class="selected-count">
          {{ t('system.role.selectedCount') }}: {{ checkedMenuIds.length }}
        </div>
      </div>

      <el-tree
        ref="treeRef"
        v-loading="loading"
        :data="menuTree"
        :props="{ label: 'menuName', children: 'children' }"
        show-checkbox
        node-key="menuId"
        default-expand-all
        @check="handleCheck"
      >
        <template #default="{ data }">
          <span class="menu-node">
            <BlinkIcon v-if="data.icon" :icon="data.icon" size="16" />
            <span>{{ data.menuName }}</span>
            <el-tag v-if="data.type === 3" type="warning" size="small" style="margin-left: 8px">
              {{ t('menu.typeButton') }}
            </el-tag>
          </span>
        </template>
      </el-tree>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ t('common.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { ElTree } from 'element-plus'
import { getMenuTree, type MenuVO } from '@/api/menu'
import { assignMenus, getRoleDetail } from '@/api/role'

const props = defineProps<{
  modelValue: boolean
  roleId: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'success': []
}>()

const { t } = useI18n()

const treeRef = ref<InstanceType<typeof ElTree>>()
const loading = ref(false)
const submitting = ref(false)
const menuTree = ref<MenuVO[]>([])
const checkedMenuIds = ref<number[]>([])
const checkAll = ref(false)
const isIndeterminate = ref(false)

/**
 * 获取所有菜单ID
 */
const getAllMenuIds = (menus: MenuVO[]): number[] => {
  const ids: number[] = []
  const traverse = (list: MenuVO[]) => {
    list.forEach((menu) => {
      ids.push(menu.menuId)
      if (menu.children?.length) {
        traverse(menu.children)
      }
    })
  }
  traverse(menus)
  return ids
}

/**
 * 加载菜单树
 */
const loadMenuTree = async () => {
  loading.value = true
  try {
    const res = await getMenuTree()
    menuTree.value = res || []
  } catch (error) {
    console.error('[AssignMenu] Load menu tree error:', error)
    menuTree.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 加载角色已分配的菜单
 */
const loadRoleMenus = async () => {
  if (!props.roleId) return

  try {
    const detail = await getRoleDetail(props.roleId)
    const assignedIds = (detail.menus || []).map((m) => m.menuId)
    checkedMenuIds.value = assignedIds

    // 延迟设置选中状态，确保树已渲染
    setTimeout(() => {
      assignedIds.forEach((id) => {
        treeRef.value?.setChecked(id, true, false)
      })
      updateCheckAllStatus()
    }, 100)
  } catch (error) {
    console.error('[AssignMenu] Load role menus error:', error)
  }
}

/**
 * 处理节点选中
 */
const handleCheck = () => {
  checkedMenuIds.value = treeRef.value?.getCheckedKeys() as number[]
  updateCheckAllStatus()
}

/**
 * 处理全选
 */
const handleCheckAll = (val: boolean) => {
  if (val) {
    const allIds = getAllMenuIds(menuTree.value)
    treeRef.value?.setCheckedKeys(allIds)
    checkedMenuIds.value = allIds
  } else {
    treeRef.value?.setCheckedKeys([])
    checkedMenuIds.value = []
  }
  isIndeterminate.value = false
}

/**
 * 更新全选状态
 */
const updateCheckAllStatus = () => {
  const allIds = getAllMenuIds(menuTree.value)
  const checkedCount = checkedMenuIds.value.length
  checkAll.value = checkedCount === allIds.length && allIds.length > 0
  isIndeterminate.value = checkedCount > 0 && checkedCount < allIds.length
}

/**
 * 提交分配
 */
const handleSubmit = async () => {
  if (!props.roleId) return

  try {
    submitting.value = true
    // 获取选中的节点和半选中的父节点
    const checkedKeys = treeRef.value?.getCheckedKeys() as number[]
    const halfCheckedKeys = treeRef.value?.getHalfCheckedKeys() as number[]

    // 合并选中和半选中的节点（确保父菜单被包含）
    const allMenuIds = [...new Set([...checkedKeys, ...halfCheckedKeys])]

    await assignMenus({
      roleId: props.roleId,
      menuIds: allMenuIds,
    })
    ElMessage.success(t('common.success'))
    emit('update:modelValue', false)
    emit('success')
  } catch (error) {
    console.error('[AssignMenu] Submit error:', error)
  } finally {
    submitting.value = false
  }
}

/**
 * 关闭时重置
 */
const handleClose = () => {
  checkedMenuIds.value = []
  checkAll.value = false
  isIndeterminate.value = false
  treeRef.value?.setCheckedKeys([])
}

watch(() => props.modelValue, (val) => {
  if (val) {
    loadMenuTree()
    loadRoleMenus()
  }
})
</script>

<style scoped lang="scss">
.menu-tree {
  .tree-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--border-color-light);

    .selected-count {
      font-size: 14px;
      color: var(--text-color-secondary);
    }
  }

  .menu-node {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  :deep(.el-tree) {
    max-height: 400px;
    overflow-y: auto;
  }
}
</style>