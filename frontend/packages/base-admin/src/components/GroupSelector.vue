<template>
  <el-dialog
    v-model="visible"
    :title="t('group.selectGroup')"
    width="400px"
    :close-on-click-modal="false"
  >
    <el-input
      v-model.trim="filterText"
      :placeholder="t('common.pleaseInput') + t('group.groupName')"
      clearable
      style="margin-bottom: 16px"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>

    <el-tree
      ref="treeRef"
      :data="groupTreeData"
      :props="defaultProps"
      :highlight-current="true"
      :expand-on-click-node="false"
      :filter-node-method="filterNode"
      v-loading="loading"
      node-key="groupId"
      default-expand-all
      @node-click="handleNodeClick"
    />

    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :disabled="!selectedNode" @click="handleConfirm">
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import type { ElTree } from 'element-plus'
import { getGroupTree, type GroupInfo } from '@/api/group'

interface Props {
  modelValue: boolean
  selectedId?: number | null
}

const props = withDefaults(defineProps<Props>(), {
  selectedId: null,
})

const emit = defineEmits(['update:modelValue', 'select'])

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const treeRef = ref<InstanceType<typeof ElTree>>()
const loading = ref(false)
const groupTreeData = ref<GroupInfo[]>([])
const filterText = ref('')
const selectedNode = ref<GroupInfo | null>(null)

const defaultProps = {
  children: 'children',
  label: 'groupName',
  value: 'groupId',
}

const filterNode = (value: string, data: GroupInfo) => {
  if (!value) return true
  return data.groupName.includes(value)
}

watch(filterText, (val) => {
  treeRef.value?.filter(val)
})

const fetchGroupTree = async () => {
  loading.value = true
  try {
    groupTreeData.value = await getGroupTree()
  } catch (error) {
    groupTreeData.value = []
    const message = error instanceof Error ? error.message : t('common.networkError')
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const handleNodeClick = (data: GroupInfo) => {
  selectedNode.value = data
}

const handleConfirm = () => {
  if (selectedNode.value) {
    emit('select', selectedNode.value)
    visible.value = false
  }
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      fetchGroupTree()
      selectedNode.value = null
      filterText.value = ''
      if (props.selectedId) {
        setTimeout(() => {
          treeRef.value?.setCurrentKey(props.selectedId)
        }, 100)
      }
    }
  }
)
</script>

<style scoped lang="scss">
:deep(.el-tree) {
  max-height: 400px;
  overflow-y: auto;

  .el-tree-node__content {
    height: 36px;

    &:hover {
      background-color: var(--table-row-hover);
    }
  }

  .el-tree-node.is-current > .el-tree-node__content {
    background-color: var(--sidebar-active-bg);
    color: var(--primary-color);
  }
}
</style>
