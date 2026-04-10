<template>
  <el-dialog
    :title="dialogTitle"
    v-model="visible"
    width="600px"
    max-height="70vh"
    :close-on-click-modal="false"
    :lock-scroll="false"
    @closed="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="user-form">
      <el-form-item :label="t('user.avatar')" prop="avatar">
        <AvatarSelector v-model="form.avatar" :size="60" />
      </el-form-item>

      <el-form-item :label="t('user.loginName')" prop="loginName">
        <el-input
          v-model.trim="form.loginName"
          :placeholder="t('user.loginNamePlaceholder')"
          :disabled="props.type === 'edit'"
        />
      </el-form-item>

      <el-form-item :label="t('user.username')" prop="username">
        <el-input v-model.trim="form.username" :placeholder="t('user.usernamePlaceholder')" />
      </el-form-item>

      <el-form-item :label="t('user.group')" prop="groupId">
        <el-input
          v-model.trim="selectedGroupName"
          readonly
          :placeholder="t('user.selectGroup')"
          @click="groupSelectorVisible = true"
        >
          <template #suffix>
            <el-icon
              class="el-input__icon"
              style="cursor: pointer"
              @click="groupSelectorVisible = true"
            >
              <Search />
            </el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item :label="t('user.assignRole')" prop="roleIds">
        <div class="role-selector">
          <div class="selected-roles">
            <el-tag
              v-for="role in selectedRoles"
              :key="role.roleId"
              closable
              @close="handleRemoveRole(role)"
            >
              {{ role.roleName }}
            </el-tag>
            <el-button type="primary" link @click="roleSelectorVisible = true">
              <el-icon><Plus /></el-icon>
              {{ t('user.selectRoles') }}
            </el-button>
          </div>
        </div>
      </el-form-item>

      <el-form-item :label="t('user.sex')" prop="sex">
        <el-radio-group v-model="form.sex">
          <el-radio :value="1">{{ t('user.male') }}</el-radio>
          <el-radio :value="2">{{ t('user.female') }}</el-radio>
          <el-radio :value="3">{{ t('user.unknown') }}</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item :label="t('user.phone')" prop="phone">
        <el-input v-model.trim="form.phone" :placeholder="t('user.phonePlaceholder')" />
      </el-form-item>

      <el-form-item :label="t('user.email')" prop="email">
        <el-input v-model.trim="form.email" :placeholder="t('user.emailPlaceholder')" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="isSubmitting" @click="handleSubmit">
        {{ t('common.confirm') }}
      </el-button>
    </template>

    <GroupSelector
      v-model="groupSelectorVisible"
      :selected-id="form.groupId"
      @select="handleGroupSelect"
    />

    <RoleSelector
      v-model="roleSelectorVisible"
      :selected-ids="form.roleIds"
      @select="handleRoleSelect"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  addUser,
  updateUser,
  type UserInfo,
  type AddUserParams,
  type UpdateUserParams,
} from '@/api/user'
import type { GroupInfo } from '@/api/group'
import type { RoleInfo } from '@/api/role'
import AvatarSelector from '@/components/AvatarSelector.vue'
import GroupSelector from '@/components/GroupSelector.vue'
import RoleSelector from '@/components/RoleSelector.vue'
import { useSystemConfigStore } from '@/stores/systemConfig'
import { useSubmitGuard } from '@blink/components'

const { t } = useI18n()

interface Props {
  modelValue: boolean
  type: 'add' | 'edit'
  data: UserInfo | null
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue', 'success'])

const systemConfigStore = useSystemConfigStore()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const dialogTitle = computed(() => (props.type === 'add' ? t('user.addUser') : t('user.editUser')))

const formRef = ref<FormInstance>()
const { isSubmitting, submitGuard } = useSubmitGuard()
const groupSelectorVisible = ref(false)
const roleSelectorVisible = ref(false)
const selectedGroupName = ref('')
const selectedRoles = ref<RoleInfo[]>([])

const form = reactive({
  userId: undefined as number | undefined,
  loginName: '',
  username: '',
  avatar: 'fun-emoji',
  sex: 1,
  phone: '',
  email: '',
  groupId: undefined as number | undefined,
  roleIds: [] as number[],
})

const rules = computed<FormRules>(() => ({
  loginName: [
    {
      required: true,
      message: t('validation.required', { field: t('user.loginName') }),
      trigger: 'blur',
    },
    { min: 3, max: 20, message: t('validation.length', { min: 3, max: 20 }), trigger: 'blur' },
  ],
  sex: [
    {
      required: true,
      message: t('validation.required', { field: t('user.sex') }),
      trigger: 'change',
    },
  ],
  phone: [
    {
      required: true,
      message: t('validation.required', { field: t('user.phone') }),
      trigger: 'blur',
    },
    { pattern: /^1[3-9]\d{9}$/, message: t('validation.phone'), trigger: 'blur' },
  ],
  email: [{ type: 'email', message: t('validation.email'), trigger: 'blur' }],
  groupId: [
    {
      required: true,
      message: t('validation.required', { field: t('user.group') }),
      trigger: 'change',
    },
  ],
}))

const handleGroupSelect = (group: GroupInfo) => {
  form.groupId = group.groupId
  selectedGroupName.value = group.groupName
}

const handleRoleSelect = (roles: RoleInfo[]) => {
  selectedRoles.value = roles
  form.roleIds = roles.map((r) => r.roleId)
}

const handleRemoveRole = (role: RoleInfo) => {
  selectedRoles.value = selectedRoles.value.filter((r) => r.roleId !== role.roleId)
  form.roleIds = selectedRoles.value.map((r) => r.roleId)
}

const initForm = () => {
  form.userId = undefined
  form.loginName = ''
  form.username = ''
  // 使用系统配置的默认头像
  form.avatar = systemConfigStore.defaultAvatar || 'fun-emoji'
  form.sex = 1
  form.phone = ''
  form.email = ''
  form.groupId = undefined
  form.roleIds = []
  selectedGroupName.value = ''
  selectedRoles.value = []
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    await submitGuard(async () => {
      if (props.type === 'add') {
        const params: AddUserParams = {
          loginName: form.loginName,
          username: form.username || undefined,
          avatar: form.avatar,
          sex: form.sex,
          phone: form.phone,
          email: form.email || undefined,
          groupId: form.groupId,
          roles: form.roleIds,
        }
        await addUser(params)
        ElMessage.success(t('user.addSuccessMsg'))
      } else {
        const params: UpdateUserParams = {
          userId: form.userId!,
          username: form.username || undefined,
          avatar: form.avatar,
          sex: form.sex,
          phone: form.phone,
          email: form.email || undefined,
          groupIdList: form.groupId ? [form.groupId] : undefined,
          roleIdList: form.roleIds.length > 0 ? form.roleIds : undefined,
        }
        await updateUser(params)
        ElMessage.success(t('user.editSuccessMsg'))
      }
      visible.value = false
      emit('success')
    })
  })
}

const handleClose = () => {
  initForm()
  formRef.value?.resetFields()
}

watch(
  () => props.modelValue,
  (val) => {
    if (val && props.type === 'edit' && props.data) {
      form.userId = props.data.userId
      form.loginName = props.data.loginName
      form.username = props.data.username
      // 使用 SVG 文件名，如果为空则使用默认头像
      form.avatar = props.data.avatar || 'fun-emoji'
      form.sex = props.data.sex
      form.phone = props.data.phone
      form.email = props.data.email || ''
      form.groupId = props.data.group?.groupId
      selectedGroupName.value = props.data.group?.groupName || ''
      form.roleIds = props.data.roles?.map((r) => r.roleId) || []
      selectedRoles.value = (props.data.roles || []) as RoleInfo[]
    } else if (val && props.type === 'add') {
      initForm()
    }
  }
)
</script>

<style scoped lang="scss">
.user-form {
  padding: 20px 20px 0;
}

.role-selector {
  width: 100%;

  .selected-roles {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;

    .el-tag {
      margin: 0;
    }
  }
}
</style>
