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

      <!-- 页脚信息 -->
      <div class="page-footer">
        <span>{{ systemConfigStore.systemFooter }}</span>
      </div>
    </div>

    <!-- 验证码弹窗 -->
    <el-dialog
      v-model="captchaDialogVisible"
      :title="t('login.captchaTitle')"
      width="360px"
      :close-on-click-modal="false"
      class="captcha-dialog"
      @closed="handleCaptchaDialogClosed"
    >
      <div class="captcha-container">
        <!-- 滑块验证码 -->
        <div v-if="captchaType === 'blockPuzzle'" class="block-puzzle-captcha">
          <div class="captcha-image-wrapper">
            <img v-if="captchaData.originalImageBase64"
                 :src="captchaData.originalImageBase64.startsWith('data:') ? captchaData.originalImageBase64 : 'data:image/png;base64,' + captchaData.originalImageBase64"
                 class="captcha-bg-image"
                 alt="验证码背景"
                 draggable="false" />
            <img v-if="captchaData.jigsawImageBase64"
                 :src="captchaData.jigsawImageBase64.startsWith('data:') ? captchaData.jigsawImageBase64 : 'data:image/png;base64,' + captchaData.jigsawImageBase64"
                 class="captcha-jigsaw-image"
                 :style="{ left: jigsawLeft + 'px' }"
                 alt="滑块"
                 draggable="false" />
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
          <div class="captcha-actions">
            <el-button link type="primary" @click="refreshCaptcha">
              <el-icon><Refresh /></el-icon>
              {{ t('login.refresh') }}
            </el-button>
          </div>
        </div>

        <!-- 点选文字验证码 -->
        <div v-else-if="captchaType === 'clickWord'" class="click-word-captcha">
          <div class="word-hint">
            <el-icon class="hint-icon"><Pointer /></el-icon>
            <span>{{ t('login.clickWordHint') }}:</span>
            <span class="words">{{ captchaData.wordList?.join('、') }}</span>
          </div>
          <div class="captcha-image-wrapper" @click="handleWordClick">
            <img v-if="captchaData.originalImageBase64"
                 :src="'data:image/png;base64,' + captchaData.originalImageBase64"
                 class="captcha-bg-image"
                 alt="验证码"
                 draggable="false" />
            <!-- 点击标记点 -->
            <div
              v-for="(point, index) in clickedPoints"
              :key="index"
              class="click-point"
              :style="{ left: point.x + 'px', top: point.y + 'px' }"
            >
              {{ index + 1 }}
            </div>
          </div>
          <div class="captcha-footer">
            <div class="click-progress">
              {{ t('login.clickedCount') }}: {{ clickedPoints.length }} / {{ captchaData.wordList?.length || 0 }}
            </div>
            <div class="click-actions">
              <el-button link type="primary" @click="refreshCaptcha">
                <el-icon><Refresh /></el-icon>
                {{ t('login.refresh') }}
              </el-button>
              <el-button
                type="primary"
                size="small"
                :disabled="clickedPoints.length === 0"
                @click="submitWordCaptcha"
              >
                {{ t('login.confirm') }}
              </el-button>
            </div>
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
import { User, Lock, Moon, Sunny, ArrowRight, Check, Loading, Refresh, Pointer } from '@element-plus/icons-vue'
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

// 验证码弹窗关闭时重置状态
const handleCaptchaDialogClosed = () => {
  clickedPoints.value = []
  jigsawLeft.value = 0
  sliderLeft.value = 0
}

// 刷新验证码
const refreshCaptcha = async () => {
  try {
    // 使用后端配置的验证码类型
    const data = await getCaptcha({
      captchaType: captchaType.value || 'default',
      clientUid: generateUUID(),
      ts: Date.now(),
    })
    captchaData.value = data || {}

    // 根据返回的类型设置验证码类型
    if (data?.captchaType) {
      captchaType.value = data.captchaType
    }
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

    // 使用captchaId或token作为验证码ID，确保captchaType有值
    const captchaId = captchaData.value.captchaId || captchaData.value.token
    const captchaTypeValue = captchaData.value.captchaType || captchaType.value || 'clickWord'

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

    // 设置验证码类型（优先使用后端配置）
    if (config.captchaType) {
      captchaType.value = config.captchaType
    }

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
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', stopDrag)
})
</script>

<style scoped lang="scss">
// Styles are in global index.scss
</style>