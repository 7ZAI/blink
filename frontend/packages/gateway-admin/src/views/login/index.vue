<template>
  <div class="login-container">
    <!-- Left Panel - Animated Characters -->
    <div class="login-left">
      <div class="characters-wrapper">
        <AnimatedCharacters
          :isTyping="isTyping"
          :showPassword="showPasswordState"
          :passwordLength="loginForm.password.length"
        />
      </div>
    </div>

    <!-- Right Panel - Login Form -->
    <div class="login-right">
      <div class="brand-content">
        <div class="brand-header">
          <div class="logo-wrapper">
            <div class="logo-icon">
              <el-icon :size="24"><Guide /></el-icon>
            </div>
          </div>
          <h1 class="brand-title">Gateway Admin</h1>
        </div>
<!--        <p class="brand-subtitle">{{ t('login.brandSubtitle') }}</p>-->
      </div>

      <div class="login-content">
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
              ref="passwordInputRef"
              v-model="loginForm.password"
              type="password"
              :placeholder="t('login.password')"
              :prefix-icon="Lock"
              show-password
              @input="handlePasswordInput"
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
          <button class="nav-action theme-toggle" :title="themeStore.theme === 'light' ? t('settings.darkMode') : t('settings.lightMode')" @click="themeStore.toggleTheme">
            <el-icon><Moon v-if="themeStore.theme === 'light'" /><Sunny v-else /></el-icon>
            <span>{{ themeStore.theme === 'light' ? t('settings.darkMode') : t('settings.lightMode') }}</span>
          </button>

          <el-dropdown @command="handleLanguageChange" trigger="click">
            <button class="nav-action language-toggle">
              <svg class="language-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="2" y1="12" x2="22" y2="12"></line>
                <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path>
              </svg>
              <span>{{ appStore.language === 'zh-cn' ? '中文' : 'EN' }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="zh-cn" :class="{ 'is-active': appStore.language === 'zh-cn' }">
                  中文
                </el-dropdown-item>
                <el-dropdown-item command="en-us" :class="{ 'is-active': appStore.language === 'en-us' }">
                  English
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { User, Lock, Guide, Moon, Sunny } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { useAppStore } from '@/stores/app'
import AnimatedCharacters from './components/AnimatedCharacters.vue'

defineOptions({ name: 'Login' })

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const userStore = useUserStore()
const themeStore = useThemeStore()
const appStore = useAppStore()

const formRef = ref()
const passwordInputRef = ref()
const loading = ref(false)
const isTyping = ref(false)
const showPasswordState = ref(false)
let typingTimeout: ReturnType<typeof setTimeout> | null = null
let observer: MutationObserver | null = null

const loginForm = reactive({
  loginName: '',
  password: '',
  rememberMe: false
})

// Watch password changes to trigger typing animation
const handlePasswordInput = () => {
  isTyping.value = true
  if (typingTimeout) clearTimeout(typingTimeout)
  typingTimeout = setTimeout(() => {
    isTyping.value = false
  }, 150)
}

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

// Monitor password visibility changes using MutationObserver
onMounted(() => {
  // Wait for the component to render
  setTimeout(() => {
    const inputWrapper = passwordInputRef.value?.$el
    if (inputWrapper) {
      const inputElement = inputWrapper.querySelector('input')
      if (inputElement) {
        observer = new MutationObserver((mutations) => {
          mutations.forEach((mutation) => {
            if (mutation.type === 'attributes' && mutation.attributeName === 'type') {
              const target = mutation.target as HTMLInputElement
              showPasswordState.value = target.type === 'text'
            }
          })
        })
        observer.observe(inputElement, { attributes: true })
      }
    }
  }, 100)
})

onUnmounted(() => {
  if (typingTimeout) clearTimeout(typingTimeout)
  if (observer) observer.disconnect()
})
</script>

<style scoped lang="scss">
// Styles are in global index.scss
</style>