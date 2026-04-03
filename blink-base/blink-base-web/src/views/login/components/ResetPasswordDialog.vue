<template>
  <el-dialog
    v-model="visible"
    :title="t('login.resetPasswordTitle')"
    width="450px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    class="reset-password-dialog"
  >
    <div class="reset-hint">
      <el-icon class="hint-icon"><Warning /></el-icon>
      <span>{{ t('login.resetPasswordHint') }}</span>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      class="reset-form"
    >
      <el-form-item :label="t('login.newPassword')" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          :placeholder="t('login.newPasswordPlaceholder')"
          show-password
          size="large"
        />
      </el-form-item>

      <el-form-item :label="t('login.confirmPassword')" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          :placeholder="t('login.confirmPasswordPlaceholder')"
          show-password
          size="large"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button type="primary" :loading="isSubmitting" @click="handleSubmit">
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { firstTimeResetPassword } from '@/api/auth'
import { useSubmitGuard } from '@/composables/useSubmitGuard'

const { t } = useI18n()

const visible = ref(true)
const { isSubmitting, submitGuard } = useSubmitGuard()
const formRef = ref<FormInstance>()

const form = reactive({
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== form.newPassword) {
    callback(new Error(t('login.passwordNotMatch')))
  } else {
    callback()
  }
}

const rules: FormRules = {
  newPassword: [
    { required: true, message: t('login.newPasswordPlaceholder'), trigger: 'blur' },
    { min: 6, message: t('login.passwordMinLength'), trigger: 'blur' },
    { max: 20, message: t('login.passwordMaxLength'), trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: t('login.confirmPasswordPlaceholder'), trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

const emit = defineEmits<{
  success: []
}>()

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    await submitGuard(async () => {
      await firstTimeResetPassword({
        newPassword: form.newPassword,
        confirmPassword: form.confirmPassword,
      })
      ElMessage.success(t('login.resetPasswordSuccess'))
      visible.value = false
      emit('success')
    })
  })
}
</script>

<style scoped lang="scss">
.reset-password-dialog {
  :deep(.el-dialog__header) {
    margin-bottom: 0;
  }

  :deep(.el-dialog__body) {
    padding-top: 10px;
  }
}

.reset-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  margin-bottom: 20px;
  background: var(--el-color-warning-light-9);
  border-radius: 8px;
  color: var(--el-color-warning-dark-2);
  font-size: 14px;

  .hint-icon {
    font-size: 18px;
    color: var(--el-color-warning);
  }
}

.reset-form {
  padding: 10px 20px 0;

  :deep(.el-input__wrapper) {
    border-radius: 8px;
  }
}
</style>