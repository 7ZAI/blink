<template>
  <!-- 配置加载中显示加载状态 -->
  <div v-if="!configLoaded" class="config-loading-container">
    <div class="config-loading-spinner"></div>
    <p class="config-loading-text">{{ t('login.loading') }}</p>
  </div>

  <!-- 配置加载完成后显示登录页面 -->
  <div v-else class="login-container">
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
            <div class="logo-icon" v-html="systemConfigStore.systemLogo"></div>
          </div>
          <h1 class="brand-title">{{ systemConfigStore.systemTitle }}</h1>
        </div>
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

            <!-- 验证码滑块组件 -->
            <el-form-item v-if="configLoaded && captchaEnabled" prop="captcha" class="captcha-item">
              <CaptchaSlider
                ref="captchaSliderRef"
                v-model:verified="captchaVerified"
                :get-captcha-api="handleGetCaptcha"
                :check-captcha-api="handleCheckCaptcha"
                :locale="captchaLocale"
                @success="handleCaptchaSuccess"
              />
            </el-form-item>

            <el-form-item>
              <el-checkbox v-model="loginForm.rememberMe">
                {{ t('login.rememberMe') }}
              </el-checkbox>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
                {{ t('login.loginBtn') }}
              </el-button>
            </el-form-item>
          </el-form>

          <div class="login-footer">
            <button
              class="nav-action theme-toggle"
              :title="
                themeStore.theme === 'light' ? t('settings.darkMode') : t('settings.lightMode')
              "
              @click="themeStore.toggleTheme"
            >
              <el-icon>
                <Moon v-if="themeStore.theme === 'light'" />
                <Sunny v-else />
              </el-icon>
              <span>
                {{
                  themeStore.theme === 'light' ? t('settings.darkMode') : t('settings.lightMode')
                }}
              </span>
            </button>

            <el-dropdown @command="handleLanguageChange" trigger="click">
              <button class="nav-action language-toggle">
                <svg
                  class="language-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="2" y1="12" x2="22" y2="12"></line>
                  <path
                    d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"
                  ></path>
                </svg>
                <span>{{ appStore.language === 'zh-cn' ? '中文' : 'EN' }}</span>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    command="zh-cn"
                    :class="{ 'is-active': appStore.language === 'zh-cn' }"
                  >
                    中文
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="en-us"
                    :class="{ 'is-active': appStore.language === 'en-us' }"
                  >
                    English
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>

      <!-- 页脚信息 -->
      <div class="page-footer">
        <span>{{ systemConfigStore.systemFooter }}</span>
      </div>
    </div>

    <!-- 全屏登录加载遮罩层 -->
    <transition name="fade">
      <div v-if="loginLoading" class="login-loading-overlay">
        <div class="login-loading-content">
          <div class="loading-logo-wrapper">
            <div class="loading-logo" v-html="systemConfigStore.systemLogo"></div>
          </div>
          <div class="loading-progress-container">
            <div class="loading-progress-bar">
              <div class="loading-progress-fill" :style="{ width: loadingProgress + '%' }"></div>
              <div class="loading-progress-stripes"></div>
            </div>
            <div class="loading-progress-info">
              <span class="loading-progress-text">{{ loadingText }}</span>
              <span class="loading-progress-percent">{{ Math.floor(loadingProgress) }}%</span>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { User, Lock, Moon, Sunny } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { useAppStore } from '@/stores/app'
import { useSystemConfigStore } from '@/stores/systemConfig'
import { getCaptcha, checkCaptcha, getLoginConfig } from '@/api/auth'
import { CaptchaSlider } from '@blink/components'
import type {
  CaptchaData,
  CaptchaCheckResult,
  CaptchaLocale,
  CaptchaRequestParams,
  CaptchaCheckParams,
} from '@blink/components'
import AnimatedCharacters from './components/AnimatedCharacters.vue'

defineOptions({ name: 'Login' })

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const userStore = useUserStore()
const themeStore = useThemeStore()
const appStore = useAppStore()
const systemConfigStore = useSystemConfigStore()

const formRef = ref()
const passwordInputRef = ref()
const captchaSliderRef = ref()
const loading = ref(false)
const isTyping = ref(false)
const showPasswordState = ref(false)
let typingTimeout: ReturnType<typeof setTimeout> | null = null
let observer: MutationObserver | null = null

const loginForm = reactive({
  loginName: '',
  password: '',
  rememberMe: false,
})

// 验证码相关状态
const captchaEnabled = ref(false)
const configLoaded = ref(false)
const captchaVerified = ref(false)
const loginLoading = ref(false)
const loadingText = ref('')
const loadingProgress = ref(0)
const isLoginClicked = ref(false)

// 验证码国际化配置
const captchaLocale = computed<CaptchaLocale>(() => ({
  clickToVerify: t('login.clickToVerify'),
  captchaVerified: t('login.captchaVerified'),
  captchaTitle: t('login.captchaTitle'),
  dragToVerify: t('login.dragToVerify'),
  clickWordHint: t('login.clickWordHint'),
  confirm: t('login.confirm'),
  refresh: t('login.refresh'),
  loading: t('login.loading'),
  captchaSuccess: t('login.captchaSuccess'),
  captchaFailed: t('login.captchaFailed'),
  captchaLoadFailed: t('login.captchaLoadFailed'),
  pleaseClickWords: t('login.pleaseClickWords'),
}))

// Watch password changes to trigger typing animation
const handlePasswordInput = () => {
  isTyping.value = true
  if (typingTimeout) clearTimeout(typingTimeout)
  typingTimeout = setTimeout(() => {
    isTyping.value = false
  }, 150)
}

const rules = {
  loginName: [{ required: true, message: t('login.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('login.passwordRequired'), trigger: 'blur' }],
}

// 生成UUID
const generateUUID = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

// 获取验证码 API 包装
const handleGetCaptcha = async (params: CaptchaRequestParams): Promise<CaptchaData> => {
  return await getCaptcha({
    captchaType: params.captchaType,
    clientUid: params.clientUid || generateUUID(),
    ts: params.ts || Date.now(),
  })
}

// 校验验证码 API 包装
const handleCheckCaptcha = async (params: CaptchaCheckParams): Promise<CaptchaCheckResult> => {
  return await checkCaptcha({
    captchaId: params.captchaId,
    captchaType: params.captchaType,
    pointJson: params.pointJson,
    clientUid: params.clientUid,
    ts: params.ts || Date.now(),
  })
}

// 验证码验证成功回调
const handleCaptchaSuccess = (result: CaptchaCheckResult) => {
  // 保存验证码校验结果
  if (result.captchaVerification) {
    userStore.setCaptchaVerification(result.captchaVerification)
  }
  // 只有点击了登录按钮才自动登录
  if (isLoginClicked.value) {
    autoLogin()
  }
}

// 自动登录（验证码验证通过后调用）
const autoLogin = async () => {
  if (!formRef.value) return

  loading.value = true
  loadingProgress.value = 0

  try {
    // 显示全屏加载遮罩
    loginLoading.value = true

    // 阶段1: 登录中
    loadingText.value = t('login.loggingIn')
    loadingProgress.value = 10

    // 模拟进度动画
    const progressInterval = setInterval(() => {
      if (loadingProgress.value < 90) {
        loadingProgress.value += Math.random() * 10
        if (loadingProgress.value > 90) loadingProgress.value = 90
      }
    }, 200)

    const rsp = await userStore.login(loginForm.loginName, loginForm.password, loginForm.rememberMe)
    loadingProgress.value = 50

    // 阶段2: 加载用户数据
    loadingText.value = t('login.loadingUserData')
    loadingProgress.value = 70

    // 阶段3: 加载菜单
    loadingText.value = t('login.loadingMenu')
    loadingProgress.value = 85

    // 等待一小段时间确保数据加载完成
    await new Promise((resolve) => setTimeout(resolve, 500))

    // 完成
    loadingProgress.value = 100
    loadingText.value = t('login.loginSuccess')

    clearInterval(progressInterval)

    await new Promise((resolve) => setTimeout(resolve, 300))

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
    captchaVerified.value = false
    userStore.setCaptchaVerification('')
    loginLoading.value = false
    loadingProgress.value = 0
    // 重置验证码组件
    captchaSliderRef.value?.reset()
    console.error('Login failed:', error)
  } finally {
    loading.value = false
  }
}

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  // 标记已点击登录按钮
  isLoginClicked.value = true

  // 如果未开启验证码，直接登录
  if (!captchaEnabled.value) {
    await autoLogin()
    return
  }

  if (!captchaVerified.value) {
    // 打开验证码弹窗
    captchaSliderRef.value?.open()
    return
  }

  await autoLogin()
}

// 加载登录配置
const loadLoginConfig = async () => {
  try {
    const config = await getLoginConfig()
    captchaEnabled.value = config.captchaEnabled ?? false

    // 更新系统配置
    if (config.systemTitle) {
      systemConfigStore.setSystemTitle(config.systemTitle)
    }
    if (config.systemLogo) {
      systemConfigStore.setSystemLogo(config.systemLogo)
    }
    if (config.systemFooter) {
      systemConfigStore.setSystemFooter(config.systemFooter)
    }
    if (config.defaultAvatar) {
      systemConfigStore.setDefaultAvatar(config.defaultAvatar)
    }

    // 如果未开启验证码，设置为已验证状态
    if (!captchaEnabled.value) {
      captchaVerified.value = true
    }
  } catch (error) {
    // 获取配置失败时使用默认配置
    systemConfigStore.resetToDefault()
    // 默认关闭验证码
    captchaEnabled.value = false
    captchaVerified.value = true
  } finally {
    configLoaded.value = true
  }
}

const handleLanguageChange = (lang: string) => {
  appStore.setLanguage(lang)
  locale.value = lang
}

// Monitor password visibility changes using MutationObserver
onMounted(() => {
  // 加载登录配置
  loadLoginConfig()

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
// 配置加载状态样式
.config-loading-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  z-index: 9999;
}

.config-loading-spinner {
  width: 48px;
  height: 48px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: #fff;
  animation: spin 1s linear infinite;
}

.config-loading-text {
  margin-top: 16px;
  color: #fff;
  font-size: 14px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
