<template>
  <div class="profile-page">
    <el-card shadow="never">
      <el-tabs v-model="activeTab" class="profile-tabs">
        <!-- 个人信息 Tab -->
        <el-tab-pane :label="t('profile.personalInfo')" name="info">
          <div class="profile-content">
            <div class="avatar-section">
              <AvatarSelector v-model="form.avatar" :size="120" />
              <div class="avatar-tip">{{ t('avatar.clickToChange') }}</div>
            </div>

            <el-form
              :model="form"
              :rules="rules"
              ref="formRef"
              label-width="100px"
              class="profile-form"
            >
              <el-form-item :label="t('user.loginName')">
                <el-input v-model="form.loginName" disabled />
              </el-form-item>

              <el-form-item :label="t('user.username')" prop="username">
                <el-input
                  v-model.trim="form.username"
                  :placeholder="t('common.pleaseInput') + t('user.username')"
                />
              </el-form-item>

              <el-form-item :label="t('user.sex')" prop="sex">
                <el-radio-group v-model="form.sex">
                  <el-radio :value="1">{{ t('user.male') }}</el-radio>
                  <el-radio :value="2">{{ t('user.female') }}</el-radio>
                  <el-radio :value="3">{{ t('user.unknown') }}</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item :label="t('user.phone')" prop="phone">
                <el-input
                  v-model.trim="form.phone"
                  :placeholder="t('common.pleaseInput') + t('user.phone')"
                />
              </el-form-item>

              <el-form-item :label="t('user.email')" prop="email">
                <el-input
                  v-model.trim="form.email"
                  :placeholder="t('common.pleaseInput') + t('user.email')"
                />
              </el-form-item>

              <el-form-item :label="t('user.remark')">
                <el-input
                  v-model="form.remark"
                  type="textarea"
                  :rows="4"
                  :placeholder="t('common.pleaseInput') + t('user.remark')"
                />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" :loading="submitting" @click="handleSubmit">
                  <el-icon><Check /></el-icon>
                  {{ t('common.confirm') }}
                </el-button>
                <el-button @click="handleReset">
                  <el-icon><Refresh /></el-icon>
                  {{ t('common.reset') }}
                </el-button>
                <el-button type="warning" @click="handleChangePassword">
                  <el-icon><Lock /></el-icon>
                  {{ t('user.changePassword') }}
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- 偏好设置 Tab -->
        <el-tab-pane :label="t('profile.preferences')" name="preferences">
          <div class="preferences-content">
            <el-form label-width="120px" class="preferences-form">
              <!-- 主题设置 -->
              <el-form-item :label="t('preferences.theme')">
                <el-radio-group v-model="preferenceForm.theme" @change="handleThemeChange">
                  <el-radio-button value="light">
                    <el-icon><Sunny /></el-icon>
                    {{ t('header.lightMode') }}
                  </el-radio-button>
                  <el-radio-button value="dark">
                    <el-icon><Moon /></el-icon>
                    {{ t('header.darkMode') }}
                  </el-radio-button>
                  <el-radio-button value="auto">
                    <el-icon><Timer /></el-icon>
                    {{ t('preferences.autoTheme') }}
                  </el-radio-button>
                </el-radio-group>
              </el-form-item>

              <!-- 语言设置 -->
              <el-form-item :label="t('preferences.language')">
                <el-radio-group v-model="preferenceForm.language" @change="handleLanguageChange">
                  <el-radio-button value="zh_cn">{{ t('header.zhCN') }}</el-radio-button>
                  <el-radio-button value="en_us">{{ t('header.enUS') }}</el-radio-button>
                </el-radio-group>
              </el-form-item>

              <!-- 侧边栏设置 -->
              <el-form-item :label="t('preferences.sidebar')">
                <el-switch
                  v-model="preferenceForm.sidebarCollapsed"
                  :active-text="t('preferences.collapsed')"
                  :inactive-text="t('preferences.expanded')"
                  @change="handleSidebarChange"
                />
              </el-form-item>

              <!-- 字体大小 -->
              <el-form-item :label="t('preferences.fontSize')">
                <el-slider
                  v-model="preferenceForm.fontSize"
                  :min="12"
                  :max="18"
                  :step="1"
                  show-stops
                />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="handleSavePreferences">
                  <el-icon><Check /></el-icon>
                  {{ t('common.save') }}
                </el-button>
                <el-button @click="handleResetPreferences">
                  <el-icon><Refresh /></el-icon>
                  {{ t('common.reset') }}
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 修改密码弹窗 -->
    <el-dialog
      v-model="passwordDialogVisible"
      :title="t('user.changePassword')"
      width="450px"
      :close-on-click-modal="false"
      :lock-scroll="false"
      @closed="handlePasswordDialogClose"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="120px"
      >
        <el-form-item :label="t('user.oldPassword')" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
            :placeholder="t('common.pleaseInput') + t('user.oldPassword')"
          />
        </el-form-item>
        <el-form-item :label="t('user.newPassword')" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            :placeholder="t('common.pleaseInput') + t('user.newPassword')"
          />
        </el-form-item>
        <el-form-item :label="t('user.confirmPassword')" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            :placeholder="t('common.pleaseInput') + t('user.confirmPassword')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="passwordSubmitting" @click="handlePasswordSubmit">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Check, Refresh, Lock, Sunny, Moon, Timer } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import {
  updateUser,
  modifyPassword,
  saveUserPreference,
  getUserPreference,
  type UserPreference,
} from '@/api/user'
import AvatarSelector from '@/components/AvatarSelector.vue'
import { setLocale, getCurrentLocale } from '@/locales'
import type { FormInstance, FormRules } from 'element-plus'

defineOptions({
  name: 'Profile',
})

const { t } = useI18n()
const userStore = useUserStore()
const themeStore = useThemeStore()

const activeTab = ref('info')
const formRef = ref<FormInstance>()
const submitting = ref(false)

const passwordDialogVisible = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordSubmitting = ref(false)

// 个人信息表单
const form = reactive({
  userId: null as number | null,
  loginName: '',
  username: '',
  avatar: 'fun-emoji',
  sex: 3,
  phone: '',
  email: '',
  remark: '',
})

// 偏好设置表单
const preferenceForm = reactive({
  theme: 'light' as 'light' | 'dark' | 'auto',
  language: 'zh_cn',
  sidebarCollapsed: false,
  fontSize: 14,
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPassword = (
  _rule: unknown,
  value: string,
  callback: (error?: Error) => void
) => {
  if (value === '') {
    callback(new Error(t('validation.required', { field: t('user.confirmPassword') })))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error(t('validation.passwordNotMatch')))
  } else {
    callback()
  }
}

const rules = reactive<FormRules>({
  username: [
    { required: true, message: t('common.pleaseInput') + t('user.username'), trigger: 'blur' },
  ],
  sex: [{ required: true, message: t('common.pleaseSelect') + t('user.sex'), trigger: 'change' }],
})

const passwordRules = reactive<FormRules>({
  oldPassword: [
    { required: true, message: t('common.pleaseInput') + t('user.oldPassword'), trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: t('common.pleaseInput') + t('user.newPassword'), trigger: 'blur' },
    { min: 6, max: 20, message: t('validation.passwordLength'), trigger: 'blur' },
  ],
  confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }],
})

// 加载偏好设置
const loadPreferences = async () => {
  try {
    const prefs = await getUserPreference()
    if (prefs) {
      preferenceForm.theme = (prefs.theme as 'light' | 'dark' | 'auto') || 'light'
      preferenceForm.language = prefs.language || 'zh_cn'
      preferenceForm.sidebarCollapsed = prefs.sidebarCollapsed || false
      preferenceForm.fontSize = prefs.fontSize || 14
    }
  } catch (error) {
    // 使用默认值
    preferenceForm.theme = themeStore.theme
    preferenceForm.language = getCurrentLocale()
  }
}

const loadUserInfo = () => {
  if (userStore.userInfo) {
    form.userId = userStore.userInfo.userId
    form.loginName = userStore.userInfo.loginName || ''
    form.username = userStore.userInfo.username || ''
    // 使用 SVG 文件名作为头像，如果为空则使用默认头像
    form.avatar = userStore.userInfo.avatar || 'fun-emoji'
    form.sex = userStore.userInfo.sex || 3
    form.phone = userStore.userInfo.phone || ''
    form.email = userStore.userInfo.email || ''
    // remark 字段不在 UserInfoVO 中，保持为空
    form.remark = ''
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await updateUser({
          userId: form.userId!,
          username: form.username,
          avatar: form.avatar,
          sex: form.sex,
          phone: form.phone,
          email: form.email,
        })
        ElMessage.success(t('message.operationSuccess'))
        await userStore.fetchUserInfo()
        loadUserInfo()
      } catch {
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleReset = () => {
  loadUserInfo()
  ElMessage.info(t('message.resetSuccess'))
}

const handleChangePassword = () => {
  passwordDialogVisible.value = true
}

const handlePasswordDialogClose = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.resetFields()
}

const handlePasswordSubmit = async () => {
  if (!passwordFormRef.value) return

  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return

    passwordSubmitting.value = true
    try {
      await modifyPassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
        confirmPassword: passwordForm.confirmPassword,
      })
      ElMessage.success(t('message.passwordChangeSuccess'))
      passwordDialogVisible.value = false
    } catch {
      // 错误已在请求拦截器中处理
    } finally {
      passwordSubmitting.value = false
    }
  })
}

// 主题切换
const handleThemeChange = () => {
  if (preferenceForm.theme === 'auto') {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    themeStore.setTheme(prefersDark ? 'dark' : 'light')
  } else {
    themeStore.setTheme(preferenceForm.theme)
  }
}

// 语言切换
const handleLanguageChange = () => {
  setLocale(preferenceForm.language)
}

// 侧边栏切换
const handleSidebarChange = () => {
  // 触发自定义事件通知 layout 组件
  window.dispatchEvent(
    new CustomEvent('sidebar-preference-change', {
      detail: { collapsed: preferenceForm.sidebarCollapsed },
    })
  )
}

// 保存偏好设置
const handleSavePreferences = async () => {
  try {
    await saveUserPreference({
      theme: preferenceForm.theme,
      language: preferenceForm.language,
      sidebarCollapsed: preferenceForm.sidebarCollapsed,
      fontSize: preferenceForm.fontSize,
    })
    ElMessage.success(t('preferences.saveSuccess'))
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

// 重置偏好设置
const handleResetPreferences = () => {
  preferenceForm.theme = 'light'
  preferenceForm.language = 'zh_cn'
  preferenceForm.sidebarCollapsed = false
  preferenceForm.fontSize = 14

  // 应用重置后的设置
  themeStore.setTheme('light')
  setLocale('zh_cn')
  ElMessage.info(t('preferences.resetSuccess'))
}

onMounted(() => {
  loadUserInfo()
  loadPreferences()
})
</script>

<style scoped lang="scss">
.profile-page {
  .profile-tabs {
    :deep(.el-tabs__header) {
      margin-bottom: 24px;
    }

    :deep(.el-tabs__nav-wrap::after) {
      height: 1px;
    }
  }

  .profile-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 24px;
    padding: 24px 0;

    .avatar-section {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;

      .avatar-tip {
        font-size: 12px;
        color: var(--text-color-secondary);
      }
    }

    .profile-form {
      width: 100%;
      max-width: 500px;
    }
  }

  .preferences-content {
    padding: 24px 0;

    .preferences-form {
      width: 100%;
      max-width: 500px;
      margin: 0 auto;
    }
  }
}
</style>