<!--
  BlinkTaskDialog 组件示例

  展示任务进度弹窗组件的多种使用方式：
  1. Composable 模式 - 使用 useTaskRunner 进行精细控制
  2. 便捷函数模式 - 使用 runTaskDialog 快速调用

  支持三种进度类型：
  - 百分比进度（percent）：适用于可量化的任务
  - 步骤进度（steps）：适用于多步骤任务
  - 不确定时长（indeterminate）：适用于无法预估时间的任务
-->
<template>
  <div class="demo-page">
    <div class="demo-header">
      <h2>任务进度弹窗 (BlinkTaskDialog)</h2>
      <p>任务执行进度展示组件，支持三种进度模式和多种交互方式</p>
    </div>

    <!-- Composable 模式示例 -->
    <el-card class="demo-card">
      <template #header>
        <div class="card-header">
          <span>Composable 模式 (useTaskRunner)</span>
          <el-tag type="info" size="small">精细控制</el-tag>
        </div>
      </template>

      <el-space wrap>
        <el-button type="primary" @click="handleExport">
          导出数据（百分比进度）
        </el-button>
        <el-button type="success" @click="handleBatchProcess">
          批量处理（步骤进度）
        </el-button>
        <el-button type="warning" @click="handleWaitApproval">
          等待审批（不确定时长）
        </el-button>
      </el-space>

      <BlinkTaskDialog
        v-model="taskState.visible"
        :status="taskState.status"
        :progress="taskState.progress"
        :title="taskState.title"
        :message="taskState.message"
        :result="taskState.result"
        :error="taskState.error"
        :elapsed-time="taskState.elapsedTime"
        :estimated-time="taskState.estimatedTime"
        :cancellable="true"
        :backgroundable="true"
        @cancel="handleCancelTask"
        @background="handleBackground"
      />
    </el-card>

    <!-- 便捷函数模式示例 -->
    <el-card class="demo-card">
      <template #header>
        <div class="card-header">
          <span>便捷函数模式 (runTaskDialog)</span>
          <el-tag type="success" size="small">快速调用</el-tag>
        </div>
      </template>

      <el-space wrap>
        <el-button type="primary" @click="handleQuickExport">
          快速导出
        </el-button>
        <el-button type="success" @click="handleQuickBatch">
          快速批量处理
        </el-button>
        <el-button type="warning" @click="handleQuickWait">
          快速等待任务
        </el-button>
        <el-button type="danger" @click="handleQuickWithError">
          模拟失败任务
        </el-button>
      </el-space>
    </el-card>

    <!-- 自定义配置示例 -->
    <el-card class="demo-card">
      <template #header>
        <div class="card-header">
          <span>高级配置示例</span>
          <el-tag type="warning" size="small">自定义结果</el-tag>
        </div>
      </template>

      <el-space wrap>
        <el-button type="primary" @click="handleExportWithActions">
          导出并显示操作按钮
        </el-button>
        <el-button type="success" @click="handleImportWithSteps">
          数据导入（详细步骤）
        </el-button>
      </el-space>
    </el-card>

    <!-- Props 说明 -->
    <el-card class="demo-card">
      <template #header>Props 说明</template>
      <el-table :data="propsData" border>
        <el-table-column prop="name" label="参数" width="150" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="type" label="类型" width="150" />
        <el-table-column prop="default" label="默认值" width="100" />
      </el-table>
    </el-card>

    <!-- 进度类型说明 -->
    <el-card class="demo-card">
      <template #header>进度类型说明</template>
      <el-table :data="progressTypes" border>
        <el-table-column prop="type" label="类型" width="150" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="usage" label="适用场景" width="200" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
/**
 * BlinkTaskDialog 组件使用示例
 *
 * 展示两种使用模式：
 * 1. Composable 模式 - 使用 useTaskRunner 进行精细控制
 * 2. 便捷函数模式 - 使用 runTaskDialog 快速调用
 *
 * @author binblink
 * @since 2026-04-25
 */
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  BlinkTaskDialog,
  useTaskRunner,
  runTaskDialog,
  showTaskDialog,
  TaskStatus,
} from '@blink/components'

// ============================================
// Composable 模式
// ============================================

const { state: taskState, start, cancel, pause, resume } = useTaskRunner({
  onComplete: (result) => {
    ElMessage.success(`任务完成，结果: ${JSON.stringify(result)}`)
  },
  onCancel: () => {
    ElMessage.warning('任务已取消')
  },
  onError: (error) => {
    ElMessage.error(`任务失败: ${error.message}`)
  },
})

/**
 * 处理取消任务
 */
function handleCancelTask() {
  cancel()
}

/**
 * 处理后台执行
 */
function handleBackground() {
  taskState.value.visible = false
  ElMessage.info('任务转入后台执行')
}

/**
 * 导出数据示例（百分比进度）
 */
async function handleExport() {
  await start({
    task: async (onProgress, signal) => {
      // 模拟数据导出过程
      const totalSteps = 10
      for (let i = 0; i <= totalSteps; i++) {
        // 检查取消信号
        if (signal?.aborted) {
          return null
        }

        // 更新进度
        const percent = Math.round((i / totalSteps) * 100)
        onProgress({
          percent,
          message: `正在导出数据... ${percent}%`,
          estimatedTime: (totalSteps - i) * 500,
        })

        // 模拟处理延迟
        await delay(500)
      }

      return { count: 1000, fileName: 'export_data.xlsx' }
    },
    title: '导出数据',
    message: '正在准备导出...',
    progressType: 'percent',
    cancellable: true,
    backgroundable: true,
  })
}

/**
 * 批量处理示例（步骤进度）
 */
async function handleBatchProcess() {
  await start({
    task: async (onProgress, signal) => {
      const steps = ['解析数据', '验证格式', '处理记录', '生成报告', '保存结果']

      for (let i = 0; i < steps.length; i++) {
        // 检查取消信号
        if (signal?.aborted) {
          return null
        }

        // 更新步骤进度
        onProgress({
          step: i,
          stepMessage: `正在${steps[i]}...`,
          message: `执行步骤 ${i + 1}/${steps.length}`,
        })

        // 模拟处理延迟
        await delay(1000)
      }

      return { processed: 500, skipped: 10, failed: 0 }
    },
    title: '批量处理',
    message: '开始批量处理...',
    steps: ['解析数据', '验证格式', '处理记录', '生成报告', '保存结果'],
    cancellable: true,
    backgroundable: true,
  })
}

/**
 * 等待审批示例（不确定时长）
 */
async function handleWaitApproval() {
  await start({
    task: async (onProgress, signal) => {
      onProgress({ message: '等待审批处理中...' })

      // 模拟等待审批（最长等待 30 秒）
      const maxWait = 30
      for (let i = 0; i < maxWait; i++) {
        if (signal?.aborted) {
          return null
        }

        onProgress({
          message: `等待审批处理中... (${i + 1}秒)`,
        })

        await delay(1000)

        // 模拟审批通过
        if (i === 5) {
          onProgress({ message: '审批已通过，正在处理...' })
          await delay(2000)
          return { approved: true, approver: '管理员' }
        }
      }

      return { approved: true, approver: '系统自动审批' }
    },
    title: '等待审批',
    message: '提交审批请求...',
    progressType: 'indeterminate',
    cancellable: true,
    backgroundable: true,
    onCompleteBehavior: 'show-result',
  })
}

// ============================================
// 便捷函数模式
// ============================================

/**
 * 快速导出示例
 */
async function handleQuickExport() {
  const result = await runTaskDialog({
    task: async (onProgress) => {
      for (let i = 0; i <= 100; i += 10) {
        onProgress({ percent: i, message: `导出进度: ${i}%` })
        await delay(300)
      }
      return { file: 'quick_export.xlsx', size: '2.5MB' }
    },
    title: '快速导出',
    progressType: 'percent',
  })

  if (result.success) {
    ElMessage.success(`导出成功: ${result.data?.file}`)
  } else if (result.cancelled) {
    ElMessage.warning('导出已取消')
  }
}

/**
 * 快速批量处理示例
 */
async function handleQuickBatch() {
  const result = await runTaskDialog({
    task: async (onProgress) => {
      const steps = ['读取数据', '处理数据', '写入结果']
      for (let i = 0; i < steps.length; i++) {
        onProgress({ step: i, stepMessage: `${steps[i]}中...` })
        await delay(800)
      }
      return { total: 100, success: 95 }
    },
    title: '批量处理',
    steps: ['读取数据', '处理数据', '写入结果'],
  })

  if (result.success) {
    ElMessage.success(`处理完成，成功 ${result.data?.success} 条`)
  }
}

/**
 * 快速等待任务示例
 */
async function handleQuickWait() {
  const result = await runTaskDialog({
    task: async (onProgress) => {
      onProgress({ message: '等待服务器响应...' })
      await delay(3000)
      return { status: 'completed' }
    },
    title: '等待任务',
  })

  if (result.success) {
    ElMessage.success('任务完成')
  }
}

/**
 * 模拟失败任务示例
 */
async function handleQuickWithError() {
  const result = await runTaskDialog({
    task: async (onProgress) => {
      onProgress({ message: '开始处理...' })
      await delay(1000)
      onProgress({ percent: 30, message: '处理中...' })
      await delay(1000)

      // 模拟失败
      throw new Error('服务器连接失败，请稍后重试')
    },
    title: '模拟任务',
    progressType: 'percent',
  })

  if (!result.success && result.error) {
    ElMessage.error(`任务失败: ${result.error.message}`)
  }
}

// ============================================
// 高级配置示例
// ============================================

/**
 * 导出并显示操作按钮
 */
async function handleExportWithActions() {
  const result = await showTaskDialog({
    task: async (onProgress) => {
      for (let i = 0; i <= 100; i += 5) {
        onProgress({ percent: i, message: `导出进度: ${i}%` })
        await delay(200)
      }
      return { fileName: 'export_result.xlsx', downloadUrl: '/api/download/export_result.xlsx' }
    },
    title: '数据导出',
    progressType: 'percent',
    onCompleteBehavior: 'show-actions',
    resultActions: [
      {
        label: '下载文件',
        type: 'primary',
        handler: () => {
          ElMessage.success('开始下载文件...')
          // 实际应用中这里会触发下载
        },
      },
      {
        label: '查看详情',
        type: 'default',
        handler: () => {
          ElMessageBox.alert('文件大小: 2.5MB\n记录数: 1000条', '导出详情')
        },
      },
    ],
  })

  if (result.success) {
    console.log('导出结果:', result.data)
  }
}

/**
 * 数据导入示例（详细步骤）
 */
async function handleImportWithSteps() {
  const result = await showTaskDialog({
    task: async (onProgress, signal) => {
      // 步骤 0: 解析文件
      onProgress({ step: 0, stepMessage: '解析 Excel 文件...' })
      await delay(1500)
      if (signal?.aborted) return null

      // 步骤 1: 验证数据
      onProgress({ step: 1, stepMessage: '验证数据格式...' })
      await delay(1000)
      if (signal?.aborted) return null

      // 步骤 2: 处理数据
      onProgress({ step: 2, stepMessage: '处理 500 条记录...' })
      await delay(2000)
      if (signal?.aborted) return null

      // 步骤 3: 写入数据库
      onProgress({ step: 3, stepMessage: '写入数据库...' })
      await delay(1500)
      if (signal?.aborted) return null

      // 步骤 4: 完成导入
      onProgress({ step: 4, stepMessage: '生成导入报告...' })
      await delay(500)

      return {
        total: 500,
        success: 498,
        failed: 2,
        reportUrl: '/api/download/import_report.xlsx',
      }
    },
    title: '数据导入',
    message: '开始导入数据...',
    steps: ['解析文件', '验证数据', '处理记录', '写入数据库', '生成报告'],
    cancellable: true,
    onCompleteBehavior: 'show-result',
  })

  if (result.success) {
    ElMessage.success(`导入完成: 成功 ${result.data?.success} 条，失败 ${result.data?.failed} 条`)
  }
}

// ============================================
// 工具函数
// ============================================

/**
 * 延迟函数
 */
function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

// ============================================
// Props 说明数据
// ============================================

const propsData = [
  { name: 'modelValue', desc: '弹窗显示状态', type: 'boolean', default: 'false' },
  { name: 'status', desc: '任务状态', type: 'TaskStatus', default: 'idle' },
  { name: 'progress', desc: '进度信息', type: 'TaskProgress', default: '-' },
  { name: 'title', desc: '任务标题', type: 'string', default: '任务执行' },
  { name: 'message', desc: '当前消息', type: 'string', default: '' },
  { name: 'elapsedTime', desc: '已耗时（毫秒）', type: 'number', default: '0' },
  { name: 'estimatedTime', desc: '预估剩余时间', type: 'number', default: 'null' },
  { name: 'result', desc: '任务结果', type: 'TaskResult', default: 'null' },
  { name: 'error', desc: '错误信息', type: 'Error', default: 'null' },
  { name: 'cancellable', desc: '是否可取消', type: 'boolean', default: 'false' },
  { name: 'backgroundable', desc: '是否可后台执行', type: 'boolean', default: 'false' },
  { name: 'width', desc: '弹窗宽度', type: 'string | number', default: '400px' },
]

const progressTypes = [
  {
    type: 'percent',
    desc: '百分比进度条，显示 0-100% 的进度值，支持预估剩余时间',
    usage: '数据导出、文件下载、批量处理',
  },
  {
    type: 'steps',
    desc: '步骤进度指示器，显示多个步骤的执行状态（待执行、执行中、已完成、失败）',
    usage: '数据导入、多步骤任务、流程审批',
  },
  {
    type: 'indeterminate',
    desc: '不确定进度动画，无法预估任务时长时使用旋转动画',
    usage: '等待审批、API 调用、不定时任务',
  },
]
</script>

<style scoped lang="scss">
.demo-page {
  .demo-header {
    margin-bottom: 24px;

    h2 {
      margin-bottom: 8px;
      color: var(--text-color-primary);
    }

    p {
      color: var(--text-color-secondary);
      font-size: 14px;
    }
  }

  .demo-card {
    margin-bottom: 24px;

    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
  }
}
</style>