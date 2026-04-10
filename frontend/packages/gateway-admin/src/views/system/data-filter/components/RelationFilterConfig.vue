<template>
  <div class="relation-filter-config">
    <!-- 无关联关系提示 -->
    <el-alert v-if="!hasRelations" type="warning" :closable="false" show-icon>
      {{ t('dataScope.noRelationSupport') }}
    </el-alert>

    <template v-else>
      <!-- 关联关系选择 -->
      <el-form-item :label="t('dataScope.relationName')">
        <el-select
          v-model="selectedRelationIndex"
          :disabled="disabled"
          @change="handleRelationChange"
        >
          <el-option
            v-for="(relation, index) in relations"
            :key="index"
            :label="relation.name"
            :value="index"
          />
        </el-select>
      </el-form-item>

      <!-- 匹配类型选择 -->
      <el-form-item
        v-if="selectedRelation && matchTypeOptions.length > 0"
        :label="t('dataScope.matchType')"
      >
        <el-select
          v-model="internalConfig.relationMatchType"
          :disabled="disabled"
          @change="handleMatchTypeChange"
        >
          <el-option
            v-for="option in matchTypeOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>

      <!-- 匹配值选择（列表类型）- 使用弹窗形式 -->
      <el-form-item v-if="showMatchValues" :label="getMatchValuesLabel">
        <div
          class="select-trigger"
          :class="{ 'is-disabled': disabled }"
          @click="!disabled && openSelectDialog()"
        >
          <div v-if="selectedMatchItems.length > 0" class="selected-tags">
            <el-tag
              v-for="item in selectedMatchItems.slice(0, 3)"
              :key="item.id"
              :closable="!disabled"
              @close="handleRemoveMatchItem(item)"
            >
              {{ item.name }}
            </el-tag>
            <el-tag v-if="selectedMatchItems.length > 3" type="info">
              +{{ selectedMatchItems.length - 3 }}
            </el-tag>
          </div>
          <span v-else class="placeholder">{{ t('common.pleaseSelect') }}</span>
          <el-icon class="arrow-icon"><ArrowRight /></el-icon>
        </div>
      </el-form-item>

      <!-- 当前用户提示 -->
      <el-alert v-if="showDynamicTip" type="info" :closable="false" show-icon>
        {{ dynamicTip }}
      </el-alert>
    </template>

    <!-- 用户选择弹窗 -->
    <UserSelectDialog
      v-model="userSelectVisible"
      :selected-users="selectedUsers"
      @confirm="handleUserConfirm"
    />

    <!-- 角色选择弹窗 -->
    <RoleSelectDialog
      v-model="roleSelectVisible"
      :selected-ids="internalConfig.relationMatchValues || []"
      @confirm="handleRoleConfirm"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * 关联过滤规则配置组件
 * 用于配置关联表过滤规则
 * 注意：gateway-admin 没有部门功能，已排除部门相关选项
 *
 * @author binblink
 */
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowRight } from '@element-plus/icons-vue'
import { getMatchTypes, type RelationInfoVO, type MatchTypeOption } from '@/api/dataScope'
import type { UserInfo } from '@/api/user'
import type { RoleInfo } from '@/api/role'
import UserSelectDialog from './UserSelectDialog.vue'
import RoleSelectDialog from './RoleSelectDialog.vue'

interface Props {
  entityInfo?: {
    tableName?: string
    relations?: RelationInfoVO[]
  }
  modelValue: string // JSON字符串
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const { t } = useI18n()

const selectedRelationIndex = ref(0)
const matchTypeOptions = ref<MatchTypeOption[]>([])

interface MatchItem {
  id: number
  name: string
}

// 弹窗显示状态
const userSelectVisible = ref(false)
const roleSelectVisible = ref(false)

// 已选择的项目（用于展示）
const selectedMatchItems = ref<MatchItem[]>([])
const selectedUsers = ref<UserInfo[]>([])

// 内部配置对象
const internalConfig = ref<Record<string, any>>({})

// 从 JSON 字符串解析配置
const parseConfig = (jsonStr: string) => {
  try {
    if (jsonStr) {
      return JSON.parse(jsonStr)
    }
  } catch {
    // ignore
  }
  return {}
}

// 监听 modelValue 变化，更新内部配置
watch(
  () => props.modelValue,
  (val) => {
    internalConfig.value = parseConfig(val)
  },
  { immediate: true }
)

// 监听内部配置变化，更新 modelValue
watch(
  internalConfig,
  (val) => {
    emit('update:modelValue', JSON.stringify(val))
  },
  { deep: true }
)

const relations = computed(() => props.entityInfo?.relations || [])
const hasRelations = computed(() => relations.value.length > 0)
const selectedRelation = computed(() => relations.value[selectedRelationIndex.value])

// 是否显示匹配值选择器
const showMatchValues = computed(() => {
  // dynamic=false 的类型需要选择匹配值
  const currentOption = matchTypeOptions.value.find(
    (o) => o.value === internalConfig.value?.relationMatchType
  )
  return currentOption && !currentOption.dynamic
})

// 是否显示动态类型提示
const showDynamicTip = computed(() => {
  const currentOption = matchTypeOptions.value.find(
    (o) => o.value === internalConfig.value?.relationMatchType
  )
  return currentOption && currentOption.dynamic
})

// 动态类型提示内容
const dynamicTip = computed(() => {
  const currentOption = matchTypeOptions.value.find(
    (o) => o.value === internalConfig.value?.relationMatchType
  )
  if (!currentOption) return ''
  return `将过滤出"${currentOption.label}"相关的数据`
})

// 匹配值选择器的标签
const getMatchValuesLabel = computed(() => {
  const type = internalConfig.value?.relationMatchType
  if (type === 'USER_LIST') return t('dataScope.selectUser')
  if (type === 'ROLE_LIST') return t('dataScope.selectRole')
  return t('dataScope.selectMatchValues')
})

// 加载匹配类型选项
const loadMatchTypes = async () => {
  if (!props.entityInfo?.tableName || !selectedRelation.value?.name) {
    matchTypeOptions.value = []
    return
  }

  try {
    const res = await getMatchTypes(props.entityInfo.tableName, selectedRelation.value.name)
    // 过滤掉部门相关的选项（gateway-admin 没有部门功能）
    const deptRelatedTypes = [
      'CURRENT_DEPT',
      'CURRENT_DEPT_CHILDREN',
      'DEPT_LIST',
      'CURRENT_USER_DEPT_CHILDREN',
    ]
    matchTypeOptions.value = (res.options || []).filter(
      (opt) => !deptRelatedTypes.includes(opt.value)
    )

    // 设置默认匹配类型
    const firstOption = matchTypeOptions.value[0]
    if (firstOption && !internalConfig.value?.relationMatchType) {
      internalConfig.value = {
        ...internalConfig.value,
        relationMatchType: firstOption.value,
      }
    }
  } catch {
    matchTypeOptions.value = []
  }
}

/**
 * 打开选择弹窗
 */
const openSelectDialog = () => {
  if (props.disabled) return

  const type = internalConfig.value?.relationMatchType
  if (type === 'USER_LIST') {
    userSelectVisible.value = true
  } else if (type === 'ROLE_LIST') {
    roleSelectVisible.value = true
  }
}

/**
 * 处理用户选择确认
 */
const handleUserConfirm = (users: UserInfo[]) => {
  selectedUsers.value = users
  internalConfig.value = {
    ...internalConfig.value,
    relationMatchValues: users.map((u) => u.userId),
  }
  selectedMatchItems.value = users.map((u) => ({
    id: u.userId,
    name: u.username || u.loginName,
  }))
}

/**
 * 处理角色选择确认
 */
const handleRoleConfirm = (roles: RoleInfo[]) => {
  internalConfig.value = {
    ...internalConfig.value,
    relationMatchValues: roles.map((r) => r.roleId),
  }
  selectedMatchItems.value = roles.map((r) => ({
    id: r.roleId,
    name: r.roleName,
  }))
}

/**
 * 移除已选择项
 */
const handleRemoveMatchItem = (item: MatchItem) => {
  selectedMatchItems.value = selectedMatchItems.value.filter((i) => i.id !== item.id)
  internalConfig.value = {
    ...internalConfig.value,
    relationMatchValues: selectedMatchItems.value.map((i) => i.id),
  }
  // 同步更新用户列表
  if (internalConfig.value?.relationMatchType === 'USER_LIST') {
    selectedUsers.value = selectedUsers.value.filter((u) => u.userId !== item.id)
  }
}

// 关联关系变更处理
const handleRelationChange = () => {
  if (selectedRelation.value) {
    internalConfig.value = {
      ...internalConfig.value,
      relationTable: selectedRelation.value.relationTable,
      sourceField: selectedRelation.value.sourceField,
      relationSourceField: selectedRelation.value.relationSourceField,
      relationTargetField: selectedRelation.value.relationTargetField,
      relationMatchType: '',
      relationMatchValues: [],
    }
    selectedMatchItems.value = []
    selectedUsers.value = []
    loadMatchTypes()
  }
}

// 匹配类型变更处理
const handleMatchTypeChange = () => {
  internalConfig.value = {
    ...internalConfig.value,
    relationMatchValues: [],
  }
  selectedMatchItems.value = []
  selectedUsers.value = []
}

// 是否已完成初始化（防止编辑模式下重复重置配置）
const isInitialized = ref(false)

// 初始化
watch(
  () => props.entityInfo,
  () => {
    if (hasRelations.value) {
      // 如果已有配置（编辑模式），不重置配置，只加载匹配类型
      if (props.modelValue && !isInitialized.value) {
        // 编辑模式：解析已有配置，恢复选中状态
        const config = parseConfig(props.modelValue)
        if (config.relationTable) {
          // 查找已选中的关联关系索引
          const index = relations.value.findIndex((r) => r.relationTable === config.relationTable)
          if (index >= 0) {
            selectedRelationIndex.value = index
          }
        }
        loadMatchTypes()
      } else if (!isInitialized.value) {
        // 新增模式：初始化配置
        handleRelationChange()
      }
      isInitialized.value = true
    }
  },
  { immediate: true }
)

// 监听 modelValue 变化重置初始化状态（用于弹窗关闭后重新打开）
watch(
  () => props.modelValue,
  (val, oldVal) => {
    // 当 modelValue 从有值变为空时，说明是新增模式，重置初始化状态
    if (oldVal && !val) {
      isInitialized.value = false
      selectedRelationIndex.value = 0
      selectedMatchItems.value = []
      selectedUsers.value = []
    }
  }
)

onMounted(() => {
  if (hasRelations.value && selectedRelation.value) {
    loadMatchTypes()
  }
})
</script>

<style scoped lang="scss">
.relation-filter-config {
  .el-alert {
    margin-bottom: 16px;
  }

  :deep(.el-form-item) {
    margin-bottom: 16px;
  }

  .select-trigger {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-width: 200px;
    padding: 0 12px;
    height: 32px;
    background: var(--card-bg);
    border: 1px solid var(--border-color-base);
    border-radius: 4px;
    cursor: pointer;
    transition: border-color 0.2s;

    &:hover {
      border-color: var(--primary-color);
    }

    &.is-disabled {
      cursor: not-allowed;
      background-color: var(--bg-color-page);

      &:hover {
        border-color: var(--border-color-base);
      }

      .placeholder,
      .arrow-icon {
        color: var(--text-color-placeholder);
      }
    }

    .selected-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
      flex: 1;
      overflow: hidden;

      .el-tag {
        max-width: 120px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .placeholder {
      color: var(--text-color-placeholder);
      font-size: 14px;
    }

    .arrow-icon {
      color: var(--text-color-placeholder);
      font-size: 14px;
      margin-left: 8px;
    }
  }
}
</style>
