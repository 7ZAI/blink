<template>
  <el-dialog
    :title="t('dataScope.selectDept')"
    v-model="visible"
    width="500px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    @closed="handleClose"
    class="dept-select-dialog"
  >
    <div class="dept-selector-content">
      <!-- 搜索区域 -->
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

      <!-- 已选部门展示 -->
      <div v-if="selectedDepts.length > 0" class="selected-depts">
        <span class="label">{{ t('dataScope.selectedDept') }}:</span>
        <el-tag
          v-for="dept in selectedDepts"
          :key="dept.groupId"
          closable
          @close="handleRemoveDept(dept)"
        >
          {{ dept.groupName }}
        </el-tag>
      </div>

      <!-- 部门树 -->
      <el-tree
        ref="treeRef"
        v-loading="loading"
        :data="groupTreeData"
        :props="defaultProps"
        show-checkbox
        check-strictly
        :highlight-current="true"
        :expand-on-click-node="false"
        :filter-node-method="filterNode"
        node-key="groupId"
        default-expand-all
        @check="handleCheck"
      />
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :disabled="selectedDepts.length === 0" @click="handleSubmit">
          {{ t('common.confirm') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
/**
 * 部门选择弹窗组件
 * 用于数据权限配置中选择指定部门，支持多选
 *
 * @author binblink
 * @since 2024-01-01
 */
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import type { ElTree } from 'element-plus'
import { getGroupTree, type GroupInfo } from '@/api/group'

defineOptions({ name: 'DeptSelectDialog' })

interface Props {
  modelValue: boolean
  selectedIds: number[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: [depts: GroupInfo[]]
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const treeRef = ref<InstanceType<typeof ElTree>>()
const loading = ref(false)
const groupTreeData = ref<GroupInfo[]>([])
const filterText = ref('')
const selectedDepts = ref<GroupInfo[]>([])

const defaultProps = {
  children: 'children',
  label: 'groupName',
  value: 'groupId',
}

/**
 * 过滤节点
 */
const filterNode = (value: string, data: GroupInfo) => {
  if (!value) return true
  return data.groupName.includes(value)
}

/**
 * 监听搜索文本变化
 */
watch(filterText, (val) => {
  treeRef.value?.filter(val)
})

/**
 * 获取部门树数据
 */
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

/**
 * 处理树节点勾选
 */
const handleCheck = (data: GroupInfo, { checkedKeys }: { checkedKeys: number[] }) => {
  // 根据 checkedKeys 更新选中列表
  const allNodes = flattenTree(groupTreeData.value)
  selectedDepts.value = allNodes.filter((node) => checkedKeys.includes(node.groupId))
}

/**
 * 扁平化树结构
 */
const flattenTree = (trees: GroupInfo[]): GroupInfo[] => {
  const result: GroupInfo[] = []
  const traverse = (nodes: GroupInfo[]) => {
    nodes.forEach((node) => {
      result.push(node)
      if (node.children && node.children.length > 0) {
        traverse(node.children)
      }
    })
  }
  traverse(trees)
  return result
}

/**
 * 移除已选部门
 */
const handleRemoveDept = (dept: GroupInfo) => {
  selectedDepts.value = selectedDepts.value.filter((d) => d.groupId !== dept.groupId)
  treeRef.value?.setChecked(dept.groupId, false, false)
}

/**
 * 确认选择
 */
const handleSubmit = () => {
  emit('confirm', selectedDepts.value)
  visible.value = false
}

/**
 * 关闭弹窗时重置
 */
const handleClose = () => {
  filterText.value = ''
  selectedDepts.value = []
  treeRef.value?.setCheckedKeys([])
}

/**
 * 监听弹窗打开
 */
watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      fetchGroupTree()
      selectedDepts.value = []
      filterText.value = ''
    }
  }
)

/**
 * 监听部门树数据加载完成，恢复选中状态
 */
watch(groupTreeData, (data) => {
  if (data.length > 0 && props.selectedIds && props.selectedIds.length > 0 && props.modelValue) {
    // 延迟设置选中状态，确保树已渲染完成
    setTimeout(() => {
      treeRef.value?.setCheckedKeys(props.selectedIds)
      // 更新选中列表
      const allNodes = flattenTree(data)
      selectedDepts.value = allNodes.filter((node) => props.selectedIds.includes(node.groupId))
    }, 100)
  }
})
</script>

<style scoped lang="scss">
.dept-select-dialog {
  .dept-selector-content {
    .selected-depts {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;
      margin-bottom: 16px;
      padding: 12px;
      background-color: var(--bg-color-page);
      border-radius: 4px;
      border: 1px solid var(--border-color-light);

      .label {
        font-size: 14px;
        color: var(--text-color-secondary);
      }

      .el-tag {
        margin: 0;
      }
    }

    :deep(.el-tree) {
      max-height: 350px;
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
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
