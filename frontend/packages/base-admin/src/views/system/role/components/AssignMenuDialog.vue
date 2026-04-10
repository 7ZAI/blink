<template>
  <el-dialog
    :title="t('role.assignMenu')"
    v-model="visible"
    width="600px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    @closed="handleClose"
  >
    <div class="menu-tree">
      <div class="tree-header">
        <el-checkbox v-model="checkAll" :indeterminate="isIndeterminate" @change="handleCheckAll">
          {{ t('common.selectAll') }}
        </el-checkbox>
        <div class="selected-count">{{ t('role.selectedCount') }}: {{ checkedMenuIds.length }}</div>
      </div>

      <el-tree
        ref="treeRef"
        v-loading="loading"
        :data="menuTree"
        :props="treeProps"
        show-checkbox
        node-key="menuId"
        default-expand-all
        @check="handleCheck"
      >
        <template #default="{ node, data }">
          <span class="menu-node">
            <BlinkIcon v-if="data.icon" :icon="data.icon" size="16" />
            <span>{{ data.menuName }}</span>
            <el-tag v-if="data.type === 2" type="warning" size="small" style="margin-left: 8px">
              {{ t('menu.typeButton') }}
            </el-tag>
          </span>
        </template>
      </el-tree>
    </div>

    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="isSubmitting" @click="handleSubmit">
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { ElTree } from 'element-plus'
import { getMenuList, type MenuInfo } from '@/api/menu'
import { assignMenus, getRoleDetail, type RoleInfo } from '@/api/role'
import { useSubmitGuard } from '@blink/components'

interface Props {
  modelValue: boolean
  role: RoleInfo | null
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue', 'success'])

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const treeRef = ref<InstanceType<typeof ElTree>>()
const loading = ref(false)
const { isSubmitting, submitGuard } = useSubmitGuard()
const menuTree = ref<MenuInfo[]>([])
const checkedMenuIds = ref<number[]>([])
const checkAll = ref(false)
const isIndeterminate = ref(false)

const treeProps = {
  children: 'children',
  label: 'menuName',
}

const getAllMenuIds = (menus: MenuInfo[]): number[] => {
  const ids: number[] = []
  const traverse = (list: MenuInfo[]) => {
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

const fetchMenuTree = async () => {
  loading.value = true
  try {
    const res = await getMenuList()

    // 后端返回的是 { rows: [...], total: ... } 结构
    // rows 已经是树形结构
    if (res?.rows) {
      menuTree.value = res.rows
    } else if (Array.isArray(res)) {
      menuTree.value = res
    } else {
      menuTree.value = []
    }
  } catch (error) {
    menuTree.value = []
  } finally {
    loading.value = false
  }
}

const fetchRoleMenus = async () => {
  if (!props.role?.roleId) return

  try {
    const detail = await getRoleDetail(props.role.roleId)
    const assignedIds = (detail.menus || []).map((m) => m.menuId)
    checkedMenuIds.value = assignedIds

    setTimeout(() => {
      assignedIds.forEach((id) => {
        treeRef.value?.setChecked(id, true, false)
      })
      updateCheckAllStatus()
    }, 100)
  } catch {
    // ignore
  }
}

const handleCheck = () => {
  checkedMenuIds.value = treeRef.value?.getCheckedKeys() as number[]
  updateCheckAllStatus()
}

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

const updateCheckAllStatus = () => {
  const allIds = getAllMenuIds(menuTree.value)
  const checkedCount = checkedMenuIds.value.length
  checkAll.value = checkedCount === allIds.length
  isIndeterminate.value = checkedCount > 0 && checkedCount < allIds.length
}

const handleSubmit = async () => {
  if (!props.role?.roleId) return
  const role = props.role

  await submitGuard(async () => {
    // 获取选中的节点和半选中的父节点
    const checkedKeys = treeRef.value?.getCheckedKeys() as number[]
    const halfCheckedKeys = treeRef.value?.getHalfCheckedKeys() as number[]

    // 合并选中和半选中的节点（确保父菜单被包含）
    const allMenuIds = [...new Set([...checkedKeys, ...halfCheckedKeys])]

    await assignMenus({
      roleId: role.roleId,
      menuIds: allMenuIds,
    })
    ElMessage.success(t('message.success'))
    visible.value = false
    emit('success')
  })
}

const handleClose = () => {
  checkedMenuIds.value = []
  checkAll.value = false
  isIndeterminate.value = false
  treeRef.value?.setCheckedKeys([])
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      fetchMenuTree()
      fetchRoleMenus()
    }
  }
)
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
