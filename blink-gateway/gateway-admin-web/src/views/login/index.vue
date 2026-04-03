<template>
  <div class="login-container">
    <!-- Left Panel - Branding -->
    <div class="login-left">
      <div class="brand-content">
        <div class="logo-wrapper">
          <div class="logo-icon">
            <el-icon :size="28"><Guide /></el-icon>
          </div>
        </div>
        <h1 class="brand-title">Gateway Admin</h1>
        <p class="brand-subtitle">{{ t('login.brandSubtitle') }}</p>
      </div>
    </div>

    <!-- Right Panel - Login Form -->
    <div class="login-right">
      <div class="login-form-wrapper">
        <h2 class="form-title">{{ t('login.welcomeBack') }}</h2>

        <el-form ref="formRef" :model="loginForm" :rules="rules" size="large">
          <el-form-item prop="loginName">
            <el-input
              v-model="loginForm.loginName"
              :placeholder="t('login.username')"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              :placeholder="t('login.password')"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <el-checkbox v-model="loginForm.rememberMe">
              {{ t('login.rememberMe') }}
            </el-checkbox>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
            >
              {{ t('login.loginBtn') }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <button class="nav-action" :title="themeStore.theme === 'light' ? 'Dark Mode' : 'Light Mode'" @click="themeStore.toggleTheme">
            <el-icon v-if="themeStore.theme === 'light'"><Moon /></el-icon>
            <el-icon v-else><Sunny /></el-icon>
          </button>

          <el-dropdown @command="handleLanguageChange">
            <button class="nav-action">
              {{ appStore.language === 'zh-cn' ? t('settings.chinese') : t('settings.english') }}
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="zh-cn">{{ t('settings.chinese') }}</el-dropdown-item>
                <el-dropdown-item command="en-us">{{ t('settings.english') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { User, Lock, Guide, Moon, Sunny } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { useAppStore } from '@/stores/app'

defineOptions({ name: 'Login' })

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const userStore = useUserStore()
const themeStore = useThemeStore()
const appStore = useAppStore()

const formRef = ref()
const loading = ref(false)

const loginForm = reactive({
  loginName: '',
  password: '',
  rememberMe: false
})

const rules = {
  loginName: [
    { required: true, message: t('login.usernameRequired'), trigger: 'blur' }
  ],
  password: [
    { required: true, message: t('login.passwordRequired'), trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const rsp = await userStore.login(
      loginForm.loginName,
      loginForm.password,
      loginForm.rememberMe
    )

    ElMessage.success(t('login.loginSuccess'))

    // 登录响应已包含用户信息和菜单，无需再次请求
    // Check if first login - could prompt password change
    if (rsp.needResetPassword) {
      // TODO: Show password change dialog
    }

    // 使用 replace 而不是 push，避免回退到登录页
    // 同时使用 name 路由而不是 path，更可靠
    const redirect = (route.query.redirect as string) || '/dashboard'
    await router.replace(redirect)
  } catch (error: any) {
    // Error message already shown by request interceptor
    console.error('Login failed:', error)
  } finally {
    loading.value = false
  }
}

const handleLanguageChange = (lang: string) => {
  appStore.setLanguage(lang)
  locale.value = lang
}
</script>

<style scoped lang="scss">
// Styles are in global index.scss
</style>