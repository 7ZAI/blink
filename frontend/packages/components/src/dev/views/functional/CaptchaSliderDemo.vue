<template>
  <div class="captcha-demo-page">
    <div class="demo-header">
      <h2>滑块验证码 (CaptchaSlider)</h2>
      <p>支持滑块拼图验证和点选文字验证两种方式，连接真实后端接口测试</p>
    </div>

    <!-- API 配置提示 -->
    <el-alert
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 24px"
    >
      <template #title>
        <span>接口配置说明</span>
      </template>
      <div>
        <p><strong>滑块拼图验证 (blockPuzzle):</strong> base-app 接口 - http://localhost:8001/base/captcha</p>
        <p><strong>点选文字验证 (clickWord):</strong> gateway-admin 接口 - http://localhost:8008/gateway-admin/captcha</p>
        <p style="margin-top: 8px">请确保已启动对应的后端服务</p>
      </div>
    </el-alert>

    <!-- 组件预览区域 -->
    <el-card class="preview-card">
      <template #header>
        <div class="card-header">
          <span>组件预览</span>
          <el-radio-group v-model="captchaType" size="small">
            <el-radio-button value="blockPuzzle">滑块拼图验证 (base-app)</el-radio-button>
            <el-radio-button value="clickWord">点选文字验证 (gateway-admin)</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-row :gutter="24">
        <el-col :span="8">
          <h4 style="margin-bottom: 12px">基本用法</h4>
          <CaptchaSlider
            ref="captchaRef1"
            :captcha-type="captchaType"
            :get-captcha-api="getCaptchaApi"
            :check-captcha-api="checkCaptchaApi"
            :verified="verified1"
            @update:verified="verified1 = $event"
            @success="handleSuccess"
            @fail="handleFail"
            @refresh="handleRefresh"
            @open="handleOpen"
            @close="handleClose"
          />
          <div class="status-info">
            <el-tag :type="verified1 ? 'success' : 'info'" size="small">
              {{ verified1 ? '已验证' : '未验证' }}
            </el-tag>
            <el-button v-if="verified1" type="warning" size="small" @click="resetCaptcha1">
              重置
            </el-button>
          </div>
        </el-col>

        <el-col :span="8">
          <h4 style="margin-bottom: 12px">自定义配置</h4>
          <CaptchaSlider
            ref="captchaRef2"
            :captcha-type="captchaType"
            :get-captcha-api="getCaptchaApi"
            :check-captcha-api="checkCaptchaApi"
            :verified="verified2"
            dialog-title="身份验证"
            trigger-text="请完成安全验证"
            :auto-close-on-success="autoCloseOnSuccess"
            :show-trigger="showTrigger"
            :disabled="disabled"
            @update:verified="verified2 = $event"
            @success="handleSuccess"
            @fail="handleFail"
          />
          <div class="status-info">
            <el-tag :type="verified2 ? 'success' : 'info'" size="small">
              {{ verified2 ? '已验证' : '未验证' }}
            </el-tag>
            <el-button v-if="verified2" type="warning" size="small" @click="resetCaptcha2">
              重置
            </el-button>
          </div>
        </el-col>

        <el-col :span="8">
          <h4 style="margin-bottom: 12px">禁用状态</h4>
          <CaptchaSlider
            :captcha-type="captchaType"
            :get-captcha-api="getCaptchaApi"
            :check-captcha-api="checkCaptchaApi"
            :disabled="true"
            trigger-text="验证码已禁用"
          />
          <div class="status-info">
            <el-tag type="danger" size="small">禁用状态</el-tag>
          </div>
        </el-col>
      </el-row>

      <!-- 控制面板 -->
      <div class="control-panel">
        <el-divider>配置控制</el-divider>
        <el-row :gutter="16">
          <el-col :span="6">
            <el-checkbox v-model="showTrigger">显示触发器</el-checkbox>
          </el-col>
          <el-col :span="6">
            <el-checkbox v-model="autoCloseOnSuccess">成功后自动关闭</el-checkbox>
          </el-col>
          <el-col :span="6">
            <el-checkbox v-model="disabled">禁用组件</el-checkbox>
          </el-col>
          <el-col :span="6">
            <el-button type="primary" size="small" @click="resetAll">重置所有状态</el-button>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 事件日志 -->
    <el-card class="event-log-card">
      <template #header>
        <div class="card-header">
          <span>事件日志</span>
          <el-button type="danger" size="small" @click="clearLogs">清空日志</el-button>
        </div>
      </template>
      <div class="event-log-container">
        <div v-if="eventLogs.length === 0" class="empty-log">
          暂无事件日志，请与验证码组件交互
        </div>
        <div v-else class="event-log-list">
          <div v-for="(log, index) in eventLogs" :key="index" class="log-item" :class="log.type">
            <span class="log-time">{{ log.time }}</span>
            <span class="log-event">{{ log.event }}</span>
            <span class="log-data">{{ log.data }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- Props 文档 -->
    <el-card class="props-card">
      <template #header>Props 参数</template>
      <el-table :data="propsData" border stripe>
        <el-table-column prop="name" label="参数名" width="180" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="type" label="类型" width="200" />
        <el-table-column prop="default" label="默认值" width="120" />
      </el-table>
    </el-card>

    <!-- Events 文档 -->
    <el-card class="events-card">
      <template #header>Events 事件</template>
      <el-table :data="eventsData" border stripe>
        <el-table-column prop="name" label="事件名" width="180" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="params" label="回调参数" width="250" />
      </el-table>
    </el-card>

    <!-- Methods 文档 -->
    <el-card class="methods-card">
      <template #header>暴露方法 (Expose)</template>
      <el-table :data="methodsData" border stripe>
        <el-table-column prop="name" label="方法名" width="180" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="params" label="参数" width="200" />
        <el-table-column prop="return" label="返回值" width="150" />
      </el-table>
    </el-card>

    <!-- 验证类型说明 -->
    <el-card class="types-card">
      <template #header>验证类型说明</template>
      <el-row :gutter="24">
        <el-col :span="12">
          <div class="type-desc">
            <h4>滑块拼图验证 (blockPuzzle)</h4>
            <ul>
              <li>用户拖动滑块，使拼图块与背景图中的缺口位置对齐</li>
              <li>拖动结束后自动提交验证</li>
              <li>适合需要快速验证的场景</li>
              <li>支持鼠标和触摸操作</li>
            </ul>
            <div class="api-info">
              <el-tag type="success" size="small">base-app: http://localhost:8001/base/captcha</el-tag>
            </div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="type-desc">
            <h4>点选文字验证 (clickWord)</h4>
            <ul>
              <li>用户按顺序点击图片中显示的文字</li>
              <li>点击完成后自动提交验证</li>
              <li>适合需要更高安全性的场景</li>
              <li>支持多个文字点选</li>
            </ul>
            <div class="api-info">
              <el-tag type="primary" size="small">gateway-admin: http://localhost:8008/gateway-admin/captcha</el-tag>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import CaptchaSlider from '@/components/CaptchaSlider/index.vue'
import type {
  CaptchaType,
  CaptchaData,
  CaptchaCheckResult,
  CaptchaRequestParams,
  CaptchaCheckParams,
} from '@/components/CaptchaSlider/types'

/**
 * 验证码组件预览和测试页面
 *
 * 提供滑块拼图验证和点选文字验证两种方式的交互测试，
 * 连接真实后端 API 接口。
 *
 * @author binblink
 * @since 2024-04-10
 */

// ========== API 配置 ==========

const API_CONFIG = {
  // base-app 服务配置 (滑块拼图验证)
  baseApp: {
    baseUrl: 'http://localhost:8001/base',
    captchaPath: '/captcha',
  },
  // gateway-admin 服务配置 (点选文字验证)
  gatewayAdmin: {
    baseUrl: 'http://localhost:8008/gateway-admin',
    captchaPath: '/captcha',
  },
}

// ========== 组件引用 ==========

const captchaRef1 = ref()
const captchaRef2 = ref()

// ========== 状态管理 ==========

const captchaType = ref<CaptchaType>('blockPuzzle')
const verified1 = ref(false)
const verified2 = ref(false)
const showTrigger = ref(true)
const autoCloseOnSuccess = ref(true)
const disabled = ref(false)

// ========== 计算当前 API Base URL ==========

const currentApiConfig = computed(() => {
  return captchaType.value === 'clickWord' ? API_CONFIG.gatewayAdmin : API_CONFIG.baseApp
})

// ========== 事件日志 ==========

interface EventLog {
  time: string
  event: string
  data: string
  type: 'info' | 'success' | 'warning' | 'error'
}

const eventLogs = ref<EventLog[]>([])

const addLog = (event: string, data: string, type: EventLog['type'] = 'info') => {
  const now = new Date()
  const time = now.toLocaleTimeString('zh-CN', { hour12: false })
  eventLogs.value.unshift({ time, event, data, type })
  if (eventLogs.value.length > 20) {
    eventLogs.value.pop()
  }
}

const clearLogs = () => {
  eventLogs.value = []
}

// ========== 事件处理 ==========

const handleSuccess = (result: CaptchaCheckResult) => {
  addLog('success', `验证成功! captchaVerification: ${result.captchaVerification}`, 'success')
  ElMessage.success('验证成功!')
}

const handleFail = (result: CaptchaCheckResult) => {
  addLog('fail', `验证失败: ${result?.msg || '未知错误'}`, 'error')
}

const handleRefresh = () => {
  addLog('refresh', '用户刷新验证码', 'info')
}

const handleOpen = () => {
  addLog('open', `弹窗打开 - 接口: ${currentApiConfig.value.baseUrl}`, 'info')
}

const handleClose = () => {
  addLog('close', '弹窗关闭', 'info')
}

// ========== 控制操作 ==========

const resetCaptcha1 = () => {
  verified1.value = false
  if (captchaRef1.value) {
    captchaRef1.value.reset()
  }
  addLog('reset', '重置验证码 1', 'warning')
}

const resetCaptcha2 = () => {
  verified2.value = false
  if (captchaRef2.value) {
    captchaRef2.value.reset()
  }
  addLog('reset', '重置验证码 2', 'warning')
}

const resetAll = () => {
  verified1.value = false
  verified2.value = false
  showTrigger.value = true
  autoCloseOnSuccess.value = true
  disabled.value = false
  clearLogs()
  addLog('reset', '重置所有状态', 'warning')
}

// ========== 真实 API 调用 ==========

/**
 * 获取验证码 API
 * 根据验证类型调用不同的后端服务
 */
const getCaptchaApi = async (params: CaptchaRequestParams): Promise<CaptchaData> => {
  const config = currentApiConfig.value
  const url = `${config.baseUrl}${config.captchaPath}/get`

  addLog('getCaptchaApi', `请求: ${url}, 类型: ${params.captchaType}`, 'info')

  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        requestId: generateUUID(),
        body: {
          captchaType: params.captchaType,
          clientUid: params.clientUid,
          ts: params.ts,
        },
      }),
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const result = await response.json()

    // 后端响应格式: msgCode="BLINK0000" 表示成功, msgType="S" 表示成功
    if (result.msgCode !== 'BLINK0000' && result.msgType !== 'S') {
      throw new Error(result.msg || '获取验证码失败')
    }

    const captchaData: CaptchaData = {
      captchaId: result.body?.captchaId || result.body?.token,
      captchaType: result.body?.captchaType,
      originalImageBase64: result.body?.originalImageBase64,
      jigsawImageBase64: result.body?.jigsawImageBase64,
      wordList: result.body?.wordList,
      pointJson: result.body?.pointJson,
      token: result.body?.token,
    }

    addLog('getCaptchaApi', `响应: captchaId=${captchaData.captchaId}, type=${captchaData.captchaType}`, 'info')

    return captchaData
  } catch (error: unknown) {
    const errMsg = error instanceof Error ? error.message : String(error)
    addLog('getCaptchaApi', `错误: ${errMsg}`, 'error')
    ElMessage.error(`获取验证码失败: ${errMsg}`)
    throw error
  }
}

/**
 * 校验验证码 API
 */
const checkCaptchaApi = async (params: CaptchaCheckParams): Promise<CaptchaCheckResult> => {
  const config = currentApiConfig.value
  const url = `${config.baseUrl}${config.captchaPath}/check`

  addLog('checkCaptchaApi', `请求: ${url}, captchaId=${params.captchaId}`, 'info')

  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        requestId: generateUUID(),
        body: {
          captchaId: params.captchaId,
          captchaType: params.captchaType,
          pointJson: params.pointJson,
          clientUid: params.clientUid,
          ts: params.ts,
        },
      }),
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const result = await response.json()

    const checkResult: CaptchaCheckResult = {
      result: result.body?.result ?? (result.msgCode === 'BLINK0000' || result.msgType === 'S'),
      msg: result.body?.msg || result.msg || '',
      captchaId: result.body?.captchaId || params.captchaId,
      captchaVerification: result.body?.captchaVerification,
    }

    if (checkResult.result) {
      addLog('checkCaptchaApi', `验证成功! verification=${checkResult.captchaVerification}`, 'success')
    } else {
      addLog('checkCaptchaApi', `验证失败: ${checkResult.msg}`, 'error')
    }

    return checkResult
  } catch (error: unknown) {
    const errMsg = error instanceof Error ? error.message : String(error)
    addLog('checkCaptchaApi', `错误: ${errMsg}`, 'error')
    return {
      result: false,
      msg: errMsg,
    }
  }
}

// ========== 工具函数 ==========

/**
 * 生成 UUID
 */
function generateUUID(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

// ========== 文档数据 ==========

const propsData = [
  { name: 'captchaType', desc: '验证码类型', type: "'blockPuzzle' | 'clickWord' | 'default'", default: "'default'" },
  { name: 'enabled', desc: '是否启用验证码', type: 'boolean', default: 'true' },
  { name: 'verified', desc: '是否已验证通过（支持 v-model）', type: 'boolean', default: 'false' },
  { name: 'dialogTitle', desc: '弹窗标题', type: 'string', default: "'安全验证'" },
  { name: 'dialogWidth', desc: '弹窗宽度', type: 'string | number', default: "'400px'" },
  { name: 'getCaptchaApi', desc: '获取验证码 API 函数', type: '(params) => Promise<CaptchaData>', default: '-' },
  { name: 'checkCaptchaApi', desc: '校验验证码 API 函数', type: '(params) => Promise<CaptchaCheckResult>', default: '-' },
  { name: 'sliderMaxDistance', desc: '滑块最大移动距离', type: 'number', default: '266' },
  { name: 'imageWidth', desc: '图片容器宽度', type: 'number', default: '310' },
  { name: 'imageHeight', desc: '图片容器高度', type: 'number', default: '155' },
  { name: 'autoCloseOnSuccess', desc: '验证成功后是否自动关闭弹窗', type: 'boolean', default: 'true' },
  { name: 'showTrigger', desc: '是否显示触发器（滑块入口）', type: 'boolean', default: 'true' },
  { name: 'triggerText', desc: '触发器文字（未验证时）', type: 'string', default: "'点击完成验证'" },
  { name: 'triggerVerifiedText', desc: '触发器文字（已验证时）', type: 'string', default: "'验证通过'" },
  { name: 'disabled', desc: '是否禁用', type: 'boolean', default: 'false' },
  { name: 'locale', desc: '国际化文本配置', type: 'CaptchaLocale', default: 'defaultZhCnLocale' },
]

const eventsData = [
  { name: 'update:verified', desc: '验证状态更新（用于 v-model）', params: '(value: boolean)' },
  { name: 'success', desc: '验证成功', params: '(result: CaptchaCheckResult)' },
  { name: 'fail', desc: '验证失败', params: '(result: CaptchaCheckResult)' },
  { name: 'refresh', desc: '刷新验证码', params: '-' },
  { name: 'open', desc: '弹窗打开', params: '-' },
  { name: 'close', desc: '弹窗关闭', params: '-' },
  { name: 'trigger-click', desc: '点击触发器', params: '-' },
]

const methodsData = [
  { name: 'open', desc: '打开弹窗', params: '-', return: 'void' },
  { name: 'close', desc: '关闭弹窗', params: '-', return: 'void' },
  { name: 'refresh', desc: '刷新验证码', params: '-', return: 'Promise<void>' },
  { name: 'reset', desc: '重置验证状态', params: '-', return: 'void' },
  { name: 'getVerificationData', desc: '获取验证结果数据', params: '-', return: 'CaptchaData' },
]
</script>

<style scoped lang="scss">
.captcha-demo-page {
  .demo-header {
    margin-bottom: 24px;
    h2 {
      margin-bottom: 8px;
      font-size: 24px;
      color: #303133;
    }
    p {
      color: #909399;
      font-size: 14px;
    }
  }

  .preview-card {
    margin-bottom: 24px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .status-info {
    margin-top: 12px;
    display: flex;
    gap: 8px;
    align-items: center;
  }

  .control-panel {
    margin-top: 24px;
  }

  .event-log-card {
    margin-bottom: 24px;
  }

  .event-log-container {
    max-height: 300px;
    overflow-y: auto;
    background: #f5f7fa;
    border-radius: 8px;
    padding: 12px;
  }

  .empty-log {
    color: #909399;
    font-size: 14px;
    text-align: center;
    padding: 20px;
  }

  .event-log-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .log-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 12px;
    background: #fff;
    border-radius: 4px;
    border-left: 3px solid #409eff;
    font-size: 13px;

    &.success {
      border-left-color: #67c23a;
    }

    &.warning {
      border-left-color: #e6a23c;
    }

    &.error {
      border-left-color: #f56c6c;
    }

    .log-time {
      color: #909399;
      font-size: 12px;
      min-width: 80px;
    }

    .log-event {
      color: #303133;
      font-weight: 500;
      min-width: 120px;
    }

    .log-data {
      color: #606266;
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .props-card,
  .events-card,
  .methods-card {
    margin-bottom: 24px;
  }

  .types-card {
    .type-desc {
      h4 {
        margin-bottom: 12px;
        color: #303133;
        font-size: 16px;
      }

      ul {
        margin-bottom: 16px;
        padding-left: 20px;
        color: #606266;

        li {
          margin-bottom: 8px;
          line-height: 1.6;
        }
      }

      .api-info {
        margin-top: 8px;
      }
    }
  }
}
</style>