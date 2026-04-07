<template>
  <div class="login-container" :class="{ 'dark': themeStore.theme === 'dark' }">
    <!-- 背景特效 -->
    <BackgroundEffects />

    <div class="login-wrapper">
      <div class="login-card" :class="{ 'card-enter': showCard }">
        <div class="logo-section">
          <div class="logo-icon" v-html="systemConfigStore.systemLogo"></div>
          <h1 class="system-title">{{ systemConfigStore.systemTitle }}</h1>
          <p class="system-subtitle">{{ t('login.systemSubtitle') }}</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model.trim="loginForm.username"
              :placeholder="t('login.usernamePlaceholder')"
              size="large"
              clearable
            >
              <template #prefix>
                <el-icon class="lightning-icon"><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              :placeholder="t('login.passwordPlaceholder')"
              size="large"
              show-password
              clearable
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <!-- 验证码滑块组件 -->
          <el-form-item prop="captcha" class="captcha-item" v-if="configLoaded && captchaEnabled">
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
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              <el-icon v-if="!loading"><Right /></el-icon>
              <span>{{ t('login.loginBtn') }}</span>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <div class="footer-actions">
            <LanguageSwitch
              :current-language="currentLocale"
              :languages="[
                { code: 'zh_cn', label: '中文', nativeLabel: '简体中文' },
                { code: 'en_us', label: 'EN', nativeLabel: 'English' }
              ]"
              class="locale-switch"
              @change="handleLocaleChange"
            />
            <ThemeToggle
              :theme="themeStore.theme"
              class="theme-switch"
              :labels="{ dark: t('login.darkMode'), light: t('login.lightMode') }"
              @change="handleThemeToggleChange"
            />
          </div>
          <p class="copyright">© 2026 Blink Framework. All rights reserved.</p>
        </div>
      </div>
    </div>

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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { User, Lock, Right, ArrowRight, Check, Loading } from '@element-plus/icons-vue'
import BackgroundEffects from '@/components/BackgroundEffects/index.vue'
import ThemeToggle from '@/components/Layout/ThemeToggle/index.vue'
import LanguageSwitch from '@/components/Layout/LanguageSwitch/index.vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { useSystemConfigStore } from '@/stores/systemConfig'
import { getCaptcha, checkCaptcha, getLoginConfig } from '@/api/auth'
import { setLocale, getCurrentLocale } from '@/locales'
import type { CaptchaVO } from '@/types'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const systemConfigStore = useSystemConfigStore()
const loginFormRef = ref()
const loading = ref(false)
const showCard = ref(false)
const captchaVerified = ref(false)
const captchaDialogVisible = ref(false)
const captchaType = ref('blockPuzzle')
const captchaData = ref<CaptchaVO>({})
const jigsawLeft = ref(0)
const sliderLeft = ref(0)
const clickedPoints = ref<{x: number, y: number}[]>([])
const loginLoading = ref(false)
const loadingText = ref('正在登录...')
const loadingProgress = ref(0)
const isLoginClicked = ref(false)
const captchaEnabled = ref(false)
const configLoaded = ref(false)

const currentLocale = ref(getCurrentLocale())
const currentLocaleLabel = computed(() => {
  return currentLocale.value === 'zh_cn' ? t('login.chinese') : t('login.english')
})

const handleLocaleChange = (locale: string) => {
  if (locale !== currentLocale.value) {
    setLocale(locale)
    currentLocale.value = locale
  }
}

const loginForm = reactive({
  username: '',
  password: '',
})

const loginRules = {
  username: [
    { required: true, message: t('login.usernameRequired'), trigger: 'blur' },
    { min: 3, max: 20, message: t('login.usernameLength'), trigger: 'blur' },
  ],
  password: [
    { required: true, message: t('login.passwordRequired'), trigger: 'blur' },
    { min: 6, max: 20, message: t('login.passwordLength'), trigger: 'blur' },
  ],
}

const toggleTheme = () => {
  themeStore.toggleTheme()
}

// 兼容 ThemeToggle 组件的 change 事件
const handleThemeToggleChange = (theme: 'light' | 'dark') => {
  themeStore.setTheme(theme)
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
      captchaType: 'default',  // 使用default让后端随机选择
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

// 生成UUID
const generateUUID = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
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
  if (!loginFormRef.value) return

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

    const loginRsp = await userStore.login(loginForm.username, loginForm.password)
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

    router.push('/')
  } catch (error) {
    captchaVerified.value = false
    userStore.setCaptchaVerification('')
    loginLoading.value = false
    loadingProgress.value = 0
  } finally {
    loading.value = false
  }
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid: boolean) => {
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
  })
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

onMounted(() => {
  // 加载登录配置
  loadLoginConfig()
  setTimeout(() => {
    showCard.value = true
  }, 100)
})

onUnmounted(() => {
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', stopDrag)
})
</script>

<style scoped lang="scss">
.login-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: visible;
  background: transparent;
  transition: background 0.3s;

  &.dark {
    background: transparent;
  }
}

/* 浅色模式背景 */
.login-container:not(.dark) {
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 50%, #f8fafc 100%);
}

.login-wrapper {
  position: relative;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 20px;
  pointer-events: none;
}

.login-card {
  pointer-events: auto;
  width: 100%;
  max-width: 420px;
  padding: 40px;
  border-radius: 24px;
  border: 1px solid rgba(59, 130, 246, 0.2);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25), 0 0 30px rgba(59, 130, 246, 0.15);
  transform: translateY(30px);
  opacity: 0;
  transition: all 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  overflow: hidden;

  /* 边框光效 */
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    padding: 1px;
    background: var(--gradient-cyber);
    -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
    mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
    -webkit-mask-composite: xor;
    mask-composite: exclude;
    opacity: 0.6;
    animation: gradient-flow 3s ease infinite;
    background-size: 200% 200%;
  }
}

/* 浅色模式登录卡片 */
.login-container:not(.dark) .login-card {
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
}

/* 深色模式登录卡片 */
.login-container.dark .login-card {
  background: rgba(30, 41, 59, 0.9);
  backdrop-filter: blur(20px);
}

.card-enter {
  transform: translateY(0);
  opacity: 1;
}

.logo-section {
  text-align: center;
  margin-bottom: 30px;
}

.logo-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 90px;
  height: 90px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  border-radius: 24px;
  border: 1px solid rgba(59, 130, 246, 0.3);
  box-shadow: var(--glow-primary), 0 10px 30px rgba(59, 130, 246, 0.3);
  animation: pulse 2s infinite;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    inset: -2px;
    border-radius: inherit;
    background: var(--gradient-cyber);
    z-index: -1;
    opacity: 0;
    animation: border-pulse 3s infinite;
  }

  :deep(svg) {
    width: 32px;
    height: 62px;
    fill: var(--primary-color);
    filter: drop-shadow(0 0 15px var(--primary-color));
    flex-shrink: 0;
  }
}

@keyframes border-pulse {
  0%, 100% {
    opacity: 0;
  }
  50% {
    opacity: 0.5;
  }
}

.logo-svg {
  width: auto;
  height: 54px;
  fill: var(--primary-color);
  filter: drop-shadow(0 0 10px var(--primary-color));
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 10px 30px rgba(59, 130, 246, 0.4);
  }
  50% {
    box-shadow: 0 10px 40px rgba(59, 130, 246, 0.6);
  }
}

.system-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px;
  letter-spacing: 2px;
  position: relative;
  transition: all var(--duration-normal) var(--ease-out-expo);
}

/* 浅色模式标题 */
.login-container:not(.dark) .system-title {
  color: #1e293b;
  text-shadow: 0 0 10px rgba(59, 130, 246, 0.3);
}

/* 深色模式标题 */
.login-container.dark .system-title {
  color: #ffffff;
  text-shadow: 0 0 10px rgba(59, 130, 246, 0.5), 0 0 30px rgba(59, 130, 246, 0.3);
}

/* 标题发光动画 */
.system-title::after {
  content: '';
  position: absolute;
  inset: -4px;
  background: radial-gradient(ellipse at center, rgba(59, 130, 246, 0.2) 0%, transparent 70%);
  z-index: -1;
  animation: title-glow 3s ease-in-out infinite;
}

@keyframes title-glow {
  0%, 100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}

.system-subtitle {
  font-size: 14px;
  margin: 0;
  transition: all var(--duration-normal) var(--ease-out-expo);
}

/* 浅色模式副标题 */
.login-container:not(.dark) .system-subtitle {
  color: #64748b;
}

/* 深色模式副标题 */
.login-container.dark .system-subtitle {
  color: rgba(255, 255, 255, 0.7);
  text-shadow: 0 0 8px rgba(59, 130, 246, 0.3);
}

.login-form {
  :deep(.el-input__wrapper) {
    border-radius: 10px;
    box-shadow: var(--card-shadow);
    padding: 1px 11px;
    background-color: var(--input-bg);
    transition: all 0.3s;
    border: 2px solid var(--border-color-base);
    height: 42px;
    box-sizing: border-box;
    
    &:hover {
      border-color: var(--primary-color-light);
    }
    
    &.is-focus {
      border-color: var(--primary-color);
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
    }
  }

  :deep(.el-input__inner) {
    height: 36px;
    font-size: 14px;
    color: var(--text-color-primary);
    
    &::placeholder {
      color: var(--text-color-placeholder);
    }
  }

  :deep(.el-input__prefix) {
    color: var(--text-color-secondary);
  }
}

.lightning-icon {
  color: var(--primary-color) !important;
  animation: icon-glow 2s infinite;
}

@keyframes icon-glow {
  0%, 100% {
    filter: drop-shadow(0 0 2px var(--primary-color));
  }
  50% {
    filter: drop-shadow(0 0 8px var(--primary-color));
  }
}

// 验证码滑块样式
.captcha-slider-wrapper {
  width: 100%;
  cursor: pointer;
}

.captcha-slider {
  position: relative;
  height: 44px;
  background: var(--bg-color);
  border-radius: 22px;
  border: none;
  overflow: hidden;
  transition: all 0.3s;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);

  &.success {
    background: rgba(16, 185, 129, 0.15);
    box-shadow: inset 0 2px 4px rgba(16, 185, 129, 0.2);
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
  color: var(--text-color-secondary);
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
  background: var(--bg-color);
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
  background: var(--bg-color);
  border-radius: 22px;
  overflow: hidden;
}

.slider-track {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--border-color-base);
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
  color: var(--text-color-secondary);
  pointer-events: none;
}

// 点选验证码样式
.click-word-captcha {
  width: 100%;
}

.word-hint {
  margin-bottom: 15px;
  font-size: 14px;
  color: var(--text-color-primary);

  .words {
    color: var(--primary-color);
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
  
  :deep(.el-button) {
    border-radius: 22px;
    padding: 12px 24px;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.3s;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
    }
    
    &:active {
      transform: translateY(0);
    }
  }
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
    color: var(--primary-color);
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

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: var(--gradient-cyber);
  border: none;
  box-shadow: var(--glow-primary), 0 4px 20px rgba(102, 126, 234, 0.4);
  transition: all var(--duration-normal) var(--ease-out-expo);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  position: relative;
  overflow: hidden;

  /* 悬停光效 */
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
    transform: translateX(-100%);
    transition: transform 0.6s;
  }

  &:hover {
    transform: translateY(-3px);
    box-shadow: var(--glow-primary), 0 8px 30px rgba(102, 126, 234, 0.5);

    &::before {
      transform: translateX(100%);
    }
  }

  &:active {
    transform: translateY(-1px);
  }

  .el-icon {
    margin-right: 8px;
  }
}

.login-footer {
  margin-top: 24px;
  text-align: center;
}

/* 覆盖抽象组件样式以适应登录页面 */
.footer-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;

  :deep(.theme-toggle),
  :deep(.language-switch) {
    padding: 6px 12px;
    border-radius: 16px;
    font-size: 13px;

    .toggle-icon,
    .language-text {
      font-size: 14px;
    }
  }

  :deep(.language-switch .language-trigger) {
    padding: 6px 12px;
    border-radius: 16px;
  }

  /* 浅色模式 */
  .login-container:not(.dark) & {
    :deep(.theme-toggle),
    :deep(.language-switch) {
      color: #64748b;

      &:hover {
        color: #3b82f6;
      }
    }
  }

  /* 深色模式 */
  .login-container.dark & {
    :deep(.theme-toggle),
    :deep(.language-switch) {
      color: rgba(255, 255, 255, 0.6);

      &:hover {
        color: #ffffff;
      }
    }
  }
}

.copyright {
  font-size: 12px;
  color: var(--text-color-placeholder);
  margin: 0;
}

@media (max-width: 480px) {
  .login-card {
    padding: 30px 24px;
  }

  .system-title {
    font-size: 24px;
  }

  .logo-icon {
    width: 80px;
    height: 80px;
  }

  .logo-svg {
    height: 44px;
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
</style>