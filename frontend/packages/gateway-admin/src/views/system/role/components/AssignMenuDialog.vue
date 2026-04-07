<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('system.role.assignMenuTitle')"
    width="500px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-tree
      ref="treeRef"
      :data="menuTree"
      :props="{ label: 'menuName', children: 'children' }"
      show-checkbox
      node-key="menuId"
      default-expand-all
      :check-strictly="false"
    />
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
import { getMenuTree, type MenuVO } from '@/api/menu'
import { assignMenus } from '@/api/role'

const props = defineProps<{
  modelValue: boolean
  roleId: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'success': []
}>()

const { t } = useI18n()

const treeRef = ref()
const submitting = ref(false)
const menuTree = ref<MenuVO[]>([])

const loadMenuTree = async () => {
  try {
    const res = await getMenuTree()
    menuTree.value = res || []
  } catch (error) {
    console.error('Load menu tree error:', error)
  }
}

const handleSubmit = async () => {
  if (!props.roleId) return

  try {
    submitting.value = true
    const checkedKeys = treeRef.value?.getCheckedKeys(false) || []
    await assignMenus({
      roleId: props.roleId,
      menuIds: checkedKeys as number[]
    })
    ElMessage.success(t('common.success'))
    emit('update:modelValue', false)
    emit('success')
  } catch (error) {
    console.error('Submit error:', error)
  } finally {
    submitting.value = false
  }
}

watch(() => props.modelValue, (val) => {
  if (val) {
    loadMenuTree()
  }
})
</script>