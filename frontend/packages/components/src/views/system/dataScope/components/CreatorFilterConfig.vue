<template>
  <div class="creator-filter-config">
    <!-- 无用户字段提示 -->
    <el-alert
      v-if="userFields.length === 0"
      :title="t('dataScope.noUserIdField')"
      type="warning"
      :closable="false"
      show-icon
      class="no-user-id-field-warning"
    />

    <!-- 有用户字段时显示配置 -->
    <template v-else>
      <el-form-item :label="t('dataScope.matchField')">
        <el-select v-model="config.field" :placeholder="t('common.pleaseSelect')" :disabled="disabled" @change="updateConfig">
          <el-option
            v-for="field in userFields"
            :key="field.columnName"
            :label="field.columnName"
            :value="field.columnName"
          />
        </el-select>
      </el-form-item>

      <el-form-item :label="t('dataScope.matchType')">
        <el-select v-model="config.matchType" :placeholder="t('common.pleaseSelect')" :disabled="disabled" @change="handleMatchTypeChange">
          <el-option :label="t('dataScope.currentUser')" value="CURRENT_USER" />
          <el-option :label="t('dataScope.specifiedUser')" value="USER_LIST" />
          <el-option :label="t('dataScope.roleUser')" value="ROLE_USER" />
        </el-select>
      </el-form-item>

      <!-- 指定用户选择 -->
      <el-form-item v-if="config.matchType === 'USER_LIST'" :label="t('dataScope.selectUser')">
        <div class="select-trigger" :class="{ 'is-disabled': disabled }" @click="!disabled && (userSelectVisible = true)">
          <div v-if="selectedUsers.length > 0" class="selected-tags">
            <el-tag
              v-for="user in selectedUsers.slice(0, 3)"
              :key="user.userId"
              :closable="!disabled"
              @close="handleRemoveUser(user)"
            >
              {{ user.username || user.loginName }}
            </el-tag>
            <el-tag v-if="selectedUsers.length > 3" type="info">
              +{{ selectedUsers.length - 3 }}
            </el-tag>
          </div>
          <span v-else class="placeholder">{{ t('common.pleaseSelect') }}</span>
          <el-icon class="arrow-icon"><ArrowRight /></el-icon>
        </div>
      </el-form-item>

      <!-- 角色用户选择 -->
      <el-form-item v-if="config.matchType === 'ROLE_USER'" :label="t('dataScope.selectRole')">
        <div class="select-trigger" :class="{ 'is-disabled': disabled }" @click="!disabled && (roleSelectVisible = true)">
          <div v-if="selectedRoles.length > 0" class="selected-tags">
            <el-tag
              v-for="role in selectedRoles.slice(0, 3)"
              :key="role.roleId"
              :closable="!disabled"
              @close="handleRemoveRole(role)"
            >
              {{ role.roleName }}
            </el-tag>
            <el-tag v-if="selectedRoles.length > 3" type="info">
              +{{ selectedRoles.length - 3 }}
            </el-tag>
          </div>
          <span v-else class="placeholder">{{ t('common.pleaseSelect') }}</span>
          <el-icon class="arrow-icon"><ArrowRight /></el-icon>
        </div>
      </el-form-item>
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
      :selected-ids="config.roleIds || []"
      @confirm="handleRoleConfirm"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * 用户过滤配置组件
 * 支持当前用户、指定用户、指定角色三种匹配类型
 * 仅支持 create_by、update_by 等登入名字段
 *
 * @author binblink
 * @since 2024-01-01
 */
import { reactive, watch, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowRight } from '@element-plus/icons-vue'
import type { EntityFieldVO } from '@/api/dataScope'
import type { UserInfo } from '@/api/user'
import type { RoleInfo } from '@/api/role'
import UserSelectDialog from './UserSelectDialog.vue'
import RoleSelectDialog from './RoleSelectDialog.vue'

defineOptions({ name: 'CreatorFilterConfig' })

const { t } = useI18n()

interface Props {
  modelValue: string
  fields: EntityFieldVO[]
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'update:valid': [value: boolean]
}>()

/**
 * 用户字段列表（只支持 create_by、update_by 等登入名字段）
 */
const userFields = computed(() => {
  // 只匹配登入名字段（数据库字段名）
  const loginNameFieldPatterns = [
    'create_by', 'update_by', 'creator', 'updater'
  ]

  return props.fields.filter(field => {
    // 使用 columnName（数据库字段名）匹配
    const columnNameLower = field.columnName.toLowerCase()
    return loginNameFieldPatterns.some(pattern => columnNameLower === pattern || columnNameLower.includes(pattern))
  })
})

// 当用户字段列表变化时，通知父组件
watch(userFields, (fields) => {
  emit('update:valid', fields.length > 0)
}, { immediate: true })

interface CreatorFilterConfig {
  field: string
  matchType: string
  loginNames: string[] | null
  roleIds: number[] | null
}

const config = reactive<CreatorFilterConfig>({
  field: '',
  matchType: 'CURRENT_USER',
  loginNames: null,
  roleIds: null
})

// 弹窗显示状态
const userSelectVisible = ref(false)
const roleSelectVisible = ref(false)

// 已选择的用户和角色（用于展示）
const selectedUsers = ref<UserInfo[]>([])
const selectedRoles = ref<RoleInfo[]>([])

/**
 * 初始化配置
 */
const initConfig = () => {
  if (!props.modelValue) {
    config.field = ''
    config.matchType = 'CURRENT_USER'
    config.loginNames = null
    config.roleIds = null
    selectedUsers.value = []
    selectedRoles.value = []
    return
  }

  try {
    const parsed: CreatorFilterConfig = JSON.parse(props.modelValue)
    Object.assign(config, parsed)
  } catch {
    // 解析失败时使用默认值
  }
}

watch(() => props.modelValue, initConfig, { immediate: true })

/**
 * 处理匹配类型变化
 */
const handleMatchTypeChange = () => {
  config.loginNames = null
  config.roleIds = null
  selectedUsers.value = []
  selectedRoles.value = []
  updateConfig()
}

/**
 * 更新配置
 */
const updateConfig = () => {
  const result: CreatorFilterConfig = {
    field: config.field,
    matchType: config.matchType,
    loginNames: config.matchType === 'USER_LIST' ? config.loginNames : null,
    roleIds: config.matchType === 'ROLE_USER' ? config.roleIds : null
  }
  emit('update:modelValue', JSON.stringify(result))
}

/**
 * 处理用户选择确认
 */
const handleUserConfirm = (users: UserInfo[]) => {
  selectedUsers.value = users
  config.loginNames = users.map(u => u.loginName)
  updateConfig()
}

/**
 * 处理角色选择确认
 */
const handleRoleConfirm = (roles: RoleInfo[]) => {
  selectedRoles.value = roles
  config.roleIds = roles.map(r => r.roleId)
  updateConfig()
}

/**
 * 移除已选用户
 */
const handleRemoveUser = (user: UserInfo) => {
  selectedUsers.value = selectedUsers.value.filter(u => u.userId !== user.userId)
  config.loginNames = selectedUsers.value.map(u => u.loginName)
  updateConfig()
}

/**
 * 移除已选角色
 */
const handleRemoveRole = (role: RoleInfo) => {
  selectedRoles.value = selectedRoles.value.filter(r => r.roleId !== role.roleId)
  config.roleIds = selectedRoles.value.map(r => r.roleId)
  updateConfig()
}
</script>

<style scoped lang="scss">
.creator-filter-config {
  .no-user-id-field-warning {
    margin-bottom: 16px;
  }

  :deep(.el-alert) {
    --el-alert-bg-color: var(--card-bg);
    --el-alert-border-color: var(--border-color-light);
  }

  :deep(.el-select .el-select__placeholder) {
    color: var(--text-color-placeholder);
  }

  :deep(.el-select .el-select__selected-item) {
    color: var(--text-color-primary);
  }

  :deep(.el-form-item__label) {
    color: var(--text-color-regular);
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