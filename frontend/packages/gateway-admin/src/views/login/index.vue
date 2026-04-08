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
            <div class="captcha-slider-wrapper" @click="showCaptchaDialog">
              <div class="captcha-slider" :class="{ 'success': captchaVerified }">
                <div class="slider-track">
                  <span class="slider-text">
                    {{ captchaVerified ? t('login.captchaVerified') : t('login.clickToVerify') }}
                  </span>
                </div>
                <div class="slider-btn" :class="{ 'success': captchaVerified }">
                  <el-icon v-if="!captchaVerified"><ArrowRight /></el-icon>
                  <el-icon v-else><Check /></el-icon>
                </div>
              </div>
            </div>
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

    <!-- 验证码弹窗 -->
    <el-dialog
      v-model="captchaDialogVisible"
      :title="t('login.captchaTitle')"
      width="400px"
      :close-on-click-modal="false"
      class="captcha-dialog"
    >
      <div class="captcha-container">
        <!-- 滑块验证码 -->
        <div v-if="captchaType === 'blockPuzzle'" class="block-puzzle-captcha">
          <div class="captcha-image-wrapper">
            <img v-if="captchaData.originalImageBase64"
                 :src="captchaData.originalImageBase64.startsWith('data:') ? captchaData.originalImageBase64 : 'data:image/png;base64,' + captchaData.originalImageBase64"
                 class="captcha-bg-image"
                 alt="验证码背景" />
            <img v-if="captchaData.jigsawImageBase64"
                 :src="captchaData.jigsawImageBase64.startsWith('data:') ? captchaData.jigsawImageBase64 : 'data:image/png;base64,' + captchaData.jigsawImageBase64"
                 class="captcha-jigsaw-image"
                 :style="{ left: jigsawLeft + 'px' }"
                 alt="滑块" />
          </div>
          <div class="slider-container">
            <div class="slider-track">
              <div class="slider-fill" :style="{ width: (sliderLeft + 40) + 'px' }"></div>
            </div>
            <div
              class="slider-thumb"
              :style="{ transform: `translateX(${sliderLeft}px)` }"
              @mousedown="startDrag"
              @touchstart="startDrag"
            >
              <el-icon><ArrowRight /></el-icon>
            </div>
            <span class="slider-hint">{{ t('login.dragToVerify') }}</span>
          </div>
        </div>

        <!-- 点选验证码 -->
        <div v-else-if="captchaType === 'clickWord'" class="click-word-captcha">
          <div class="word-hint">
            {{ t('login.clickWordHint') }}: <span class="words">{{ captchaData.wordList?.join(', ') }}</span>
          </div>
          <div class="captcha-image-wrapper" @click="handleWordClick">
            <img v-if="captchaData.originalImageBase64"
                 :src="'data:image/png;base64,' + captchaData.originalImageBase64"
                 class="captcha-bg-image"
                 alt="验证码" />
            <div
              v-for="(point, index) in clickedPoints"
              :key="index"
              class="click-point"
              :style="{ left: point.x + 'px', top: point.y + 'px' }"
            >
              {{ index + 1 }}
            </div>
          </div>
          <div class="click-actions">
            <el-button type="primary" @click="submitWordCaptcha">{{ t('login.confirm') }}</el-button>
            <el-button @click="refreshCaptcha">{{ t('login.refresh') }}</el-button>
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-else class="captcha-loading">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <span>{{ t('login.loading') }}</span>
        </div>
      </div>
    </el-dialog>

    <!-- 全屏登录加载遮罩层 -->
    <transition name="fade">
      <div v-if="loginLoading" class="login-loading-overlay">
        <div class="login-loading-content">
          <div class="loading-logo-wrapper">
            <svg viewBox="0 0 12 32" class="loading-logo">
              <path d="M7 2L2 14h5l-2 12 9-16h-5l2-8z"/>
            </svg>
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
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { User, Lock, Moon, Sunny, ArrowRight, Check, Loading } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { useAppStore } from '@/stores/app'
import { useSystemConfigStore } from '@/stores/systemConfig'
import { getCaptcha, checkCaptcha, getLoginConfig } from '@/api/auth'
import AnimatedCharacters from './components/AnimatedCharacters.vue'
import type { CaptchaVO } from '@/types'

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

// 验证码相关状态
const captchaEnabled = ref(false)
const configLoaded = ref(false)
const captchaVerified = ref(false)
const captchaDialogVisible = ref(false)
const captchaType = ref('blockPuzzle')
const captchaData = ref<CaptchaVO>({})
const jigsawLeft = ref(0)
const sliderLeft = ref(0)
const clickedPoints = ref<{x: number, y: number}[]>([])
const loginLoading = ref(false)
const loadingText = ref('')
const loadingProgress = ref(0)
const isLoginClicked = ref(false)

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

// 生成UUID
const generateUUID = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

// 显示验证码弹窗
const showCaptchaDialog = async () => {
  if (captchaVerified.value) return
  captchaDialogVisible.value = true
  clickedPoints.value = []
  await refreshCaptcha()
}

// 刷新验证码
const refreshCaptcha = async () => {
  try {
    captchaType.value = 'loading'
    // 不指定验证码类型，由后端随机返回
    const data = await getCaptcha({
      captchaType: 'default',
      clientUid: generateUUID(),
      ts: Date.now(),
    })
    captchaData.value = data || {}

    // 根据返回的类型设置验证码类型
    captchaType.value = data?.captchaType || 'blockPuzzle'
    jigsawLeft.value = 0
    sliderLeft.value = 0
    clickedPoints.value = []
  } catch (error) {
    ElMessage.error(t('login.captchaLoadFailed'))
  }
}

// 滑块拖动相关
let isDragging = false
let startX = 0
let startLeft = 0
let animationId: number | null = null
let currentSliderLeft = 0

const startDrag = (e: MouseEvent | TouchEvent) => {
  if (captchaVerified.value) return
  isDragging = true
  startX = 'touches' in e ? (e.touches[0]?.clientX ?? 0) : e.clientX
  startLeft = sliderLeft.value
  currentSliderLeft = sliderLeft.value

  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  document.addEventListener('touchmove', onDrag)
  document.addEventListener('touchend', stopDrag)
}

const onDrag = (e: MouseEvent | TouchEvent) => {
  if (!isDragging) return
  e.preventDefault()

  const currentX = 'touches' in e ? (e.touches[0]?.clientX ?? 0) : e.clientX
  const diff = currentX - startX
  // 滑块最大移动距离：容器宽度(310) - 滑块宽度(40) - 边距(4) = 266
  const maxLeft = 266
  const newLeft = Math.max(0, Math.min(startLeft + diff, maxLeft))

  if (newLeft !== currentSliderLeft) {
    currentSliderLeft = newLeft
    if (animationId) {
      cancelAnimationFrame(animationId)
    }
    animationId = requestAnimationFrame(() => {
      sliderLeft.value = currentSliderLeft
      jigsawLeft.value = currentSliderLeft
    })
  }
}

const stopDrag = async () => {
  if (!isDragging) return
  isDragging = false

  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }

  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', stopDrag)

  // 提交验证
  await submitSliderCaptcha()
}

// 提交滑块验证码
const submitSliderCaptcha = async () => {
  try {
    // 确保captchaType有值
    const captchaTypeValue = captchaData.value.captchaType || 'blockPuzzle'

    const pointJson = JSON.stringify({ x: jigsawLeft.value, y: 5 })

    // 使用captchaId或token作为验证码ID
    const captchaId = captchaData.value.captchaId || captchaData.value.token

    const result = await checkCaptcha({
      captchaId: captchaId,
      captchaType: captchaTypeValue,
      pointJson: pointJson,
      clientUid: captchaData.value.token,
      ts: Date.now(),
    })

    if (result?.result) {
      captchaVerified.value = true
      captchaDialogVisible.value = false
      // 保存验证码校验结果
      if (result.captchaVerification) {
        userStore.setCaptchaVerification(result.captchaVerification)
      }
      ElMessage.success(t('login.captchaSuccess'))
      // 只有点击了登录按钮才自动登录
      if (isLoginClicked.value) {
        await autoLogin()
      }
    } else {
      ElMessage.error(result?.msg || t('login.captchaFailed'))
      await refreshCaptcha()
    }
  } catch (error) {
    ElMessage.error(t('login.captchaFailed'))
    await refreshCaptcha()
  }
}

// 处理文字点击
const handleWordClick = (e: MouseEvent) => {
  if (captchaType.value !== 'clickWord') return
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  clickedPoints.value.push({ x, y })
}

// 提交文字验证码
const submitWordCaptcha = async () => {
  if (clickedPoints.value.length === 0) {
    ElMessage.warning(t('login.pleaseClickWords'))
    return
  }

  try {
    const pointJson = JSON.stringify(clickedPoints.value.map(p => ({ x: p.x, y: p.y })))
    const result = await checkCaptcha({
      captchaId: captchaData.value.captchaId,
      captchaType: captchaData.value.captchaType,
      pointJson: pointJson,
      clientUid: captchaData.value.token,
      ts: Date.now(),
    })

    if (result?.result) {
      captchaVerified.value = true
      captchaDialogVisible.value = false
      if (result.captchaVerification) {
        userStore.setCaptchaVerification(result.captchaVerification)
      }
      ElMessage.success(t('login.captchaSuccess'))
      // 只有点击了登录按钮才自动登录
      if (isLoginClicked.value) {
        await autoLogin()
      }
    } else {
      ElMessage.error(result?.msg || t('login.captchaFailed'))
      clickedPoints.value = []
      await refreshCaptcha()
    }
  } catch (error) {
    ElMessage.error(t('login.captchaFailed'))
    clickedPoints.value = []
    await refreshCaptcha()
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

    const rsp = await userStore.login(
      loginForm.loginName,
      loginForm.password,
      loginForm.rememberMe
    )
    loadingProgress.value = 50

    // 阶段2: 加载用户数据
    loadingText.value = t('login.loadingUserData')
    loadingProgress.value = 70

    // 阶段3: 加载菜单
    loadingText.value = t('login.loadingMenu')
    loadingProgress.value = 85

    // 等待一小段时间确保数据加载完成
    await new Promise(resolve => setTimeout(resolve, 500))

    // 完成
    loadingProgress.value = 100
    loadingText.value = t('login.loginSuccess')

    clearInterval(progressInterval)

    await new Promise(resolve => setTimeout(resolve, 300))

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
    // Error message already shown by request interceptor
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
    showCaptchaDialog()
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
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', stopDrag)
})
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
}

.login-left {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: radial-gradient(circle at 30% 50%, rgba(59, 130, 246, 0.15) 0%, transparent 50%);
  }
}

.characters-wrapper {
  position: relative;
  z-index: 1;
}

.login-right {
  width: 480px;
  display: flex;
  flex-direction: column;
  background: rgba(30, 41, 59, 0.8);
  backdrop-filter: blur(20px);
  border-left: 1px solid rgba(59, 130, 246, 0.2);
  box-shadow: -20px 0 60px rgba(0, 0, 0, 0.3);
}

.brand-content {
  padding: 40px 50px 20px;
  text-align: center;
}

.brand-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.logo-wrapper {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  border-radius: 20px;
  border: 1px solid rgba(59, 130, 246, 0.3);
  box-shadow: 0 0 30px rgba(59, 130, 246, 0.3);
}

.logo-icon {
  :deep(svg) {
    width: 40px;
    height: 40px;
    fill: var(--primary-color, #3b82f6);
    filter: drop-shadow(0 0 10px var(--primary-color, #3b82f6));
  }
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 2px;
  text-shadow: 0 0 20px rgba(59, 130, 246, 0.5);
}

.login-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 50px 40px;
}

.login-form-wrapper {
  .form-title {
    font-size: 24px;
    font-weight: 600;
    color: #ffffff;
    margin-bottom: 32px;
    text-align: center;
  }

  :deep(.el-input__wrapper) {
    background: rgba(15, 23, 42, 0.6);
    border: 1px solid rgba(59, 130, 246, 0.2);
    box-shadow: none;
    border-radius: 12px;
    padding: 0 16px;
    height: 48px;
    transition: all 0.3s;

    &:hover {
      border-color: rgba(59, 130, 246, 0.4);
    }

    &.is-focus {
      border-color: var(--primary-color, #3b82f6);
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
    }
  }

  :deep(.el-input__inner) {
    color: #e2e8f0;
    font-size: 15px;

    &::placeholder {
      color: #64748b;
    }
  }

  :deep(.el-input__prefix) {
    color: #64748b;
  }

  :deep(.el-checkbox__label) {
    color: #94a3b8;
  }

  :deep(.el-checkbox__inner) {
    background: transparent;
    border-color: #475569;
  }

  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    background: var(--primary-color, #3b82f6);
    border-color: var(--primary-color, #3b82f6);
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  border: none;
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.4);
  transition: all 0.3s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 30px rgba(59, 130, 246, 0.5);
  }

  &:active {
    transform: translateY(0);
  }
}

.login-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
}

.nav-action {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 20px;
  color: #94a3b8;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background: rgba(59, 130, 246, 0.2);
    color: #ffffff;
  }

  .el-icon {
    font-size: 16px;
  }
}

.language-icon {
  width: 16px;
  height: 16px;
}

// 验证码滑块样式
.captcha-item {
  margin-bottom: 16px;
}

.captcha-slider-wrapper {
  width: 100%;
  cursor: pointer;
}

.captcha-slider {
  position: relative;
  height: 44px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 22px;
  overflow: hidden;
  transition: all 0.3s;

  &.success {
    background: rgba(16, 185, 129, 0.15);
    border-color: rgba(16, 185, 129, 0.3);
  }
}

.slider-track {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.slider-text {
  font-size: 14px;
  color: #94a3b8;
  user-select: none;
}

.slider-btn {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.4);

  &:hover {
    box-shadow: 0 4px 12px rgba(16, 185, 129, 0.5);
    transform: scale(1.05);
  }

  &.success {
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    left: calc(100% - 42px);
    box-shadow: 0 2px 8px rgba(16, 185, 129, 0.4);
  }
}

// 验证码弹窗样式
.captcha-dialog {
  :deep(.el-dialog__body) {
    padding: 20px;
  }
}

.captcha-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.block-puzzle-captcha {
  width: 100%;
}

.captcha-image-wrapper {
  position: relative;
  width: 100%;
  height: 155px;
  background: #1e293b;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 20px;
}

.captcha-bg-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.captcha-jigsaw-image {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: 60px;
  object-fit: contain;
}

.slider-container {
  position: relative;
  width: 100%;
  height: 44px;
  background: #1e293b;
  border-radius: 22px;
  overflow: hidden;
}

.slider-track {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #334155;
  border-radius: 22px;
}

.slider-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: linear-gradient(90deg, #10b981 0%, #34d399 100%);
  border-radius: 22px;
  transition: width 0.1s;
}

.slider-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 2px 10px rgba(16, 185, 129, 0.4);
  cursor: grab;
  z-index: 10;
  transition: transform 0.05s ease-out;

  &:active {
    cursor: grabbing;
    box-shadow: 0 4px 15px rgba(16, 185, 129, 0.5);
  }
}

.slider-hint {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 14px;
  color: #94a3b8;
  pointer-events: none;
}

// 点选验证码样式
.click-word-captcha {
  width: 100%;
}

.word-hint {
  margin-bottom: 15px;
  font-size: 14px;
  color: #e2e8f0;

  .words {
    color: var(--primary-color, #3b82f6);
    font-weight: bold;
  }
}

.click-point {
  position: absolute;
  width: 24px;
  height: 24px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  font-weight: bold;
  transform: translate(-50%, -50%);
  animation: point-appear 0.3s ease;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.4);
}

@keyframes point-appear {
  0% {
    transform: translate(-50%, -50%) scale(0);
  }
  50% {
    transform: translate(-50%, -50%) scale(1.2);
  }
  100% {
    transform: translate(-50%, -50%) scale(1);
  }
}

.click-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

// 加载状态
.captcha-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 40px;

  .loading-icon {
    font-size: 32px;
    color: var(--primary-color, #3b82f6);
    animation: rotate 1s linear infinite;
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

// 全屏登录加载遮罩层
.login-loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 1, 20, 0.95);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.login-loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 30px;
  width: 320px;
}

.loading-logo-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-logo {
  width: 60px;
  height: 60px;
  fill: #3b82f6;
  animation: logo-pulse 1.5s ease-in-out infinite;
  filter: drop-shadow(0 0 20px rgba(59, 130, 246, 0.8));
}

@keyframes logo-pulse {
  0%, 100% {
    transform: scale(1);
    filter: drop-shadow(0 0 20px rgba(59, 130, 246, 0.8));
  }
  50% {
    transform: scale(1.1);
    filter: drop-shadow(0 0 30px rgba(59, 130, 246, 1));
  }
}

.loading-progress-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.loading-progress-bar {
  position: relative;
  width: 100%;
  height: 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  overflow: hidden;
}

.loading-progress-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6, #3b82f6);
  background-size: 200% 100%;
  border-radius: 4px;
  transition: width 0.3s ease;
  animation: gradient-shift 1.5s linear infinite;
}

@keyframes gradient-shift {
  0% {
    background-position: 0% 0%;
  }
  100% {
    background-position: 200% 0%;
  }
}

.loading-progress-stripes {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: repeating-linear-gradient(
    45deg,
    transparent,
    transparent 10px,
    rgba(255, 255, 255, 0.1) 10px,
    rgba(255, 255, 255, 0.1) 20px
  );
  animation: stripes-move 1s linear infinite;
}

@keyframes stripes-move {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: 40px 0;
  }
}

.loading-progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.loading-progress-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  letter-spacing: 0.5px;
}

.loading-progress-percent {
  font-size: 14px;
  font-weight: 600;
  color: #3b82f6;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 900px) {
  .login-container {
    flex-direction: column;
  }

  .login-left {
    display: none;
  }

  .login-right {
    width: 100%;
    border-left: none;
  }
}
</style>