<template>
  <div class="logicflow-designer" :class="{ 'dark-mode': isDark }">
    <div class="toolbar">
      <el-space>
        <el-button type="primary" @click="handleSave" :icon="DocumentChecked">
          保存流程
        </el-button>
        <el-button @click="handleDeploy" :icon="Upload">
          部署流程
        </el-button>
        <el-divider direction="vertical" />
        <el-button-group>
          <el-button @click="handleZoomIn" :icon="ZoomIn" title="放大" />
          <el-button @click="handleZoomOut" :icon="ZoomOut" title="缩小" />
          <el-button @click="handleResetZoom" :icon="Aim" title="适应屏幕" />
        </el-button-group>
        <el-divider direction="vertical" />
        <el-button-group>
          <el-button @click="handleUndo" :icon="RefreshLeft" title="撤销" />
          <el-button @click="handleRedo" :icon="RefreshRight" title="重做" />
        </el-button-group>
        <el-divider direction="vertical" />
        <el-button @click="handleDownloadXML" :icon="Download">
          导出BPMN
        </el-button>
        <el-button @click="handleDownloadImage" :icon="Picture">
          导出图片
        </el-button>
        <el-divider direction="vertical" />
        <el-button @click="handleClear" :icon="Delete" type="danger">
          清空画布
        </el-button>
      </el-space>
    </div>

    <div class="designer-content">
      <div class="node-panel">
        <div class="panel-title">流程节点</div>
        <div class="node-list">
          <div
            v-for="node in nodeTypes"
            :key="node.type"
            class="node-item"
            draggable="true"
            @dragstart="handleDragStart($event, node)"
            @dragend="handleDragEnd"
          >
            <div class="node-icon" :style="{ backgroundColor: node.color }">
              <el-icon><component :is="node.icon" /></el-icon>
            </div>
            <span class="node-name">{{ node.name }}</span>
          </div>
        </div>
      </div>

      <div 
        ref="containerRef" 
        class="canvas-container"
        @dragover.prevent
        @drop="handleDrop"
      ></div>

      <div class="properties-panel">
        <div class="panel-title">属性配置</div>
        <!-- 节点属性 -->
        <div v-if="selectedElementType === 'node' && selectedElement" class="properties-form">
          <el-form :model="nodeForm" label-width="80px" size="small">
            <el-form-item label="节点ID">
              <el-input v-model="nodeForm.id" disabled />
            </el-form-item>
            <el-form-item label="节点名称">
              <el-input v-model.trim="nodeForm.name" @change="updateNodeName" />
            </el-form-item>
            <el-form-item label="节点类型">
              <el-input :value="getNodeTypeName(selectedElement.type)" disabled />
            </el-form-item>
            <!-- 用户任务 -->
            <template v-if="selectedElement.type === 'bpmn:userTask'">
              <el-form-item label="受理人">
                <el-input v-model.trim="nodeForm.assignee" placeholder="请输入受理人" @change="updateNodeProperty('assignee', nodeForm.assignee)" />
              </el-form-item>
              <el-form-item label="候选组">
                <el-select v-model="nodeForm.candidateGroups" multiple placeholder="请选择候选组" @change="updateNodeProperty('candidateGroups', nodeForm.candidateGroups)">
                  <el-option label="部门经理" value="dept_manager" />
                  <el-option label="HR" value="hr" />
                  <el-option label="财务" value="finance" />
                </el-select>
              </el-form-item>
            </template>
            <!-- 服务任务 -->
            <template v-if="selectedElement.type === 'bpmn:serviceTask'">
              <el-form-item label="类名">
                <el-input v-model.trim="nodeForm.className" placeholder="例如: com.example.ServiceClass" @change="updateNodeProperty('className', nodeForm.className)" />
              </el-form-item>
            </template>
            <!-- 脚本任务 -->
            <template v-if="selectedElement.type === 'bpmn:scriptTask'">
              <el-form-item label="脚本格式">
                <el-input v-model.trim="nodeForm.scriptFormat" placeholder="例如: groovy, javascript" @change="updateNodeProperty('scriptFormat', nodeForm.scriptFormat)" />
              </el-form-item>
            </template>
            <!-- 定时事件 -->
            <template v-if="selectedElement.type === 'bpmn:timerEvent'">
              <el-form-item label="持续时间">
                <el-input v-model.trim="nodeForm.timeDuration" placeholder="例如: PT5M" @change="updateNodeProperty('timeDuration', nodeForm.timeDuration)" />
              </el-form-item>
            </template>
          </el-form>
        </div>
        <!-- 边属性 -->
        <div v-else-if="selectedElementType === 'edge' && selectedElement" class="properties-form">
          <el-form :model="edgeForm" label-width="80px" size="small">
            <el-form-item label="连线ID">
              <el-input v-model="edgeForm.id" disabled />
            </el-form-item>
            <el-form-item label="源节点">
              <el-input :value="selectedElement.sourceNodeId" disabled />
            </el-form-item>
            <el-form-item label="目标节点">
              <el-input :value="selectedElement.targetNodeId" disabled />
            </el-form-item>
            <el-form-item label="条件表达式">
              <el-input
                v-model="edgeForm.condition"
                type="textarea"
                :rows="3"
                placeholder="例如: ${leaveDays > 3}"
                @change="updateEdgeCondition"
              />
            </el-form-item>
          </el-form>
        </div>
        <div v-else class="empty-tip">
          <el-empty description="请选择节点或连线" :image-size="100" />
        </div>
      </div>
    </div>

    <el-dialog v-model="deployDialogVisible" title="部署流程" width="500px">
      <el-form :model="deployForm" label-width="100px">
        <el-form-item label="流程名称">
          <el-input v-model.trim="deployForm.name" placeholder="请输入流程名称" />
        </el-form-item>
        <el-form-item label="流程KEY">
          <el-input v-model.trim="deployForm.key" placeholder="请输入流程KEY" />
        </el-form-item>
        <el-form-item label="流程描述">
          <el-input
            v-model="deployForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入流程描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deployDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmDeploy" :loading="deployLoading">确定部署</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, onActivated, onDeactivated, markRaw, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DocumentChecked,
  Upload,
  ZoomIn,
  ZoomOut,
  Aim,
  RefreshLeft,
  RefreshRight,
  Download,
  Picture,
  CircleCheck,
  User,
  Setting,
  Timer,
  Document,
  Share,
  Delete
} from '@element-plus/icons-vue'
import LogicFlow, { RectNode, RectNodeModel, CircleNode, CircleNodeModel, DiamondNode, DiamondNodeModel } from '@logicflow/core'
import { BpmnElement, Menu, SelectionSelect, Snapshot } from '@logicflow/extension'
import '@logicflow/core/dist/style/index.css'
import '@logicflow/extension/lib/style/index.css'
import { useThemeStore } from '@/stores/theme'
import { deployProcess } from '@/api/workflow'

defineOptions({
  name: 'LogicFlowDesigner'
})

const containerRef = ref<HTMLDivElement>()
const themeStore = useThemeStore()
const isDark = computed(() => themeStore.theme === 'dark')

let lf: LogicFlow | null = null
let isInitialized = false

const selectedElement = ref<any>(null)
const selectedElementType = ref<'node' | 'edge' | null>(null)
const nodeForm = ref({
  id: '',
  name: '',
  assignee: '',
  candidateGroups: [] as string[],
  className: '',
  scriptFormat: '',
  timeDuration: ''
})

const edgeForm = ref({
  id: '',
  condition: ''
})

const nodeTypes = [
  { type: 'bpmn:startEvent', name: '开始事件', icon: markRaw(CircleCheck), color: '#67C23A' },
  { type: 'bpmn:endEvent', name: '结束事件', icon: markRaw(CircleCheck), color: '#F56C6C' },
  { type: 'bpmn:userTask', name: '用户任务', icon: markRaw(User), color: '#409EFF' },
  { type: 'bpmn:serviceTask', name: '服务任务', icon: markRaw(Setting), color: '#E6A23C' },
  { type: 'bpmn:exclusiveGateway', name: '排他网关', icon: markRaw(Share), color: '#909399' },
  { type: 'bpmn:parallelGateway', name: '并行网关', icon: markRaw(Share), color: '#909399' },
  { type: 'bpmn:timerEvent', name: '定时事件', icon: markRaw(Timer), color: '#67C23A' },
  { type: 'bpmn:scriptTask', name: '脚本任务', icon: markRaw(Document), color: '#E6A23C' }
]

// 自动保存相关的 key
const AUTOSAVE_KEY = 'logicflow_autosave_data'
const AUTOSAVE_INTERVAL = 30000 // 30秒自动保存一次

const deployDialogVisible = ref(false)
const deployLoading = ref(false)
const deployForm = ref({
  name: '',
  key: '',
  description: ''
})

// 自动保存相关
const autoSaveTimer = ref<number | null>(null)
const hasUnsavedChanges = ref(false)
const isRestoring = ref(false)

let draggedNodeType = ''
let draggedNodeName = ''

const getThemeColors = () => {
  return isDark.value ? {
    bgColor: '#1a1a2e',
    gridColor: '#2d2d44',
    gridDotColor: '#6366f1',
    textColor: '#e2e8f0',
    nodeTextColor: '#f1f5f9',
    borderColor: '#6366f1',
    menuBg: '#1a1a2e',
    menuHoverBg: '#2d2d44',
    menuTextColor: '#e2e8f0',
    menuHoverTextColor: '#818cf8'
  } : {
    bgColor: '#FFFFFF',
    gridColor: '#E2E8F0',
    gridDotColor: '#3B82F6',
    textColor: '#4A5568',
    nodeTextColor: '#1A202C',
    borderColor: '#E2E8F0',
    menuBg: '#FFFFFF',
    menuHoverBg: '#F1F5F9',
    menuTextColor: '#4A5568',
    menuHoverTextColor: '#3B82F6'
  }
}

const updateTheme = () => {
  if (!lf) return

  const colors = getThemeColors()

  lf.setTheme({
    rect: {
      rx: 5,
      ry: 5,
      strokeWidth: 2,
      stroke: colors.borderColor,
      fill: colors.bgColor
    },
    circle: {
      r: 20,
      strokeWidth: 2,
      stroke: colors.borderColor,
      fill: colors.bgColor
    },
    nodeText: {
      overflowMode: 'autoWrap',
      fontSize: 12,
      color: colors.nodeTextColor
    },
    edgeText: {
      fontSize: 12,
      color: colors.textColor
    }
  } as any)

  // 更新网格和背景
  lf.graphModel.gridModel.setGridOptions({
    type: 'dot',
    size: 20,
    visible: true,
    config: {
      color: colors.gridDotColor,
      thickness: 1.5
    }
  } as any)

  // 强制重绘网格
  lf.graphModel.gridModel.draw()
}

const initLogicFlow = () => {
  if (!containerRef.value || isInitialized) return

  const colors = getThemeColors()

  lf = new LogicFlow({
    container: containerRef.value,
    grid: {
      size: 20,
      visible: true,
      type: 'dot',
      config: {
        color: colors.gridDotColor,
        thickness: 1.5
      }
    },
    background: {
      backgroundColor: colors.bgColor
    },
    keyboard: {
      enabled: true
    },
    plugins: [BpmnElement, Menu, SelectionSelect, Snapshot],
    edgeType: 'bpmn:sequenceFlow',
    style: {
      rect: {
        rx: 5,
        ry: 5,
        strokeWidth: 2,
        stroke: colors.borderColor,
        fill: colors.bgColor
      },
      circle: {
        r: 20,
        strokeWidth: 2,
        stroke: colors.borderColor,
        fill: colors.bgColor
      },
      nodeText: {
        overflowMode: 'autoWrap',
        fontSize: 12,
        color: colors.nodeTextColor
      },
      edgeText: {
        fontSize: 12,
        color: colors.textColor
      }
    }
  })

  // 注册自定义节点类型（BpmnElement未内置的类型）
  // 并行网关 - 使用菱形节点
  class ParallelGatewayModel extends DiamondNodeModel {
    initNodeData(data: any) {
      super.initNodeData(data)
      this.text.value = data.text || '并行网关'
    }
  }
  class ParallelGatewayView extends DiamondNode {}

  lf.register({
    type: 'bpmn:parallelGateway',
    view: ParallelGatewayView,
    model: ParallelGatewayModel
  })

  // 定时事件 - 使用圆形节点
  class TimerEventModel extends CircleNodeModel {
    initNodeData(data: any) {
      super.initNodeData(data)
      this.r = 20
      this.text.value = data.text || '定时事件'
    }
  }
  class TimerEventView extends CircleNode {}

  lf.register({
    type: 'bpmn:timerEvent',
    view: TimerEventView,
    model: TimerEventModel
  })

  // 脚本任务 - 使用矩形节点
  class ScriptTaskModel extends RectNodeModel {
    initNodeData(data: any) {
      super.initNodeData(data)
      this.width = 100
      this.height = 60
      this.text.value = data.text || '脚本任务'
    }
  }
  class ScriptTaskView extends RectNode {}

  lf.register({
    type: 'bpmn:scriptTask',
    view: ScriptTaskView,
    model: ScriptTaskModel
  })

  lf.on('node:click', ({ data }) => {
    selectedElementType.value = 'node'
    selectedElement.value = data
    nodeForm.value = {
      id: data.id,
      name: data.text?.value || '',
      assignee: data.properties?.assignee || '',
      candidateGroups: data.properties?.candidateGroups || [],
      className: data.properties?.className || '',
      scriptFormat: data.properties?.scriptFormat || '',
      timeDuration: data.properties?.timeDuration || ''
    }
  })

  lf.on('edge:click', ({ data }) => {
    selectedElementType.value = 'edge'
    selectedElement.value = data
    edgeForm.value = {
      id: data.id,
      condition: data.properties?.condition || ''
    }
  })

  lf.on('blank:click', () => {
    selectedElementType.value = null
    selectedElement.value = null
  })

  lf.render({
    nodes: [],
    edges: []
  })

  isInitialized = true
  ElMessage.success('流程设计器初始化成功')
}

const destroyLogicFlow = () => {
  if (lf) {
    try {
      lf.destroy()
    } catch (e) {
      // ignore destroy error
    }
    lf = null
    isInitialized = false
  }
}

watch(isDark, () => {
  updateTheme()
})

onMounted(() => {
  initLogicFlow()
  startAutoSave()
})

onBeforeUnmount(() => {
  stopAutoSave()
  destroyLogicFlow()
})

onActivated(() => {
  if (!isInitialized) {
    initLogicFlow()
    startAutoSave()
  }
  // 激活时尝试恢复自动保存的数据
  setTimeout(() => {
    restoreFromAutoSave()
  }, 100)
})

onDeactivated(() => {
  selectedElementType.value = null
  selectedElement.value = null
  // 停用时立即执行一次自动保存
  autoSave()
})

const handleDragStart = (e: DragEvent, node: any) => {
  draggedNodeType = node.type
  draggedNodeName = node.name
  if (e.dataTransfer) {
    e.dataTransfer.setData('text/plain', node.type)
    e.dataTransfer.effectAllowed = 'move'
  }
}

const handleDragEnd = () => {
  draggedNodeType = ''
  draggedNodeName = ''
}

const handleDrop = (e: DragEvent) => {
  if (!lf || !draggedNodeType) return

  e.preventDefault()

  const containerRect = containerRef.value?.getBoundingClientRect()
  if (!containerRect) return

  const graphTransform = lf.getTransform()
  if (!graphTransform) return

  // 安全获取变换参数，使用默认值
  const translateX = graphTransform.TRANSLATE_X ?? 0
  const translateY = graphTransform.TRANSLATE_Y ?? 0
  const scaleX = graphTransform.SCALE_X ?? 1
  const scaleY = graphTransform.SCALE_Y ?? 1

  const x = (e.clientX - containerRect.left - translateX) / scaleX
  const y = (e.clientY - containerRect.top - translateY) / scaleY

  const nodeId = `node_${Date.now()}`

  lf.addNode({
    id: nodeId,
    type: draggedNodeType,
    x: x,
    y: y,
    text: draggedNodeName,
    properties: {
      name: draggedNodeName
    }
  })

  hasUnsavedChanges.value = true
  ElMessage.success(`已添加节点: ${draggedNodeName}`)
}

const getNodeTypeName = (type: string) => {
  const node = nodeTypes.find(n => n.type === type)
  return node?.name || type
}

const handleSave = () => {
  if (!lf) return

  const data = lf.getGraphData()
  localStorage.setItem('logicflow_data', JSON.stringify(data))
  hasUnsavedChanges.value = false
  ElMessage.success('流程已保存到本地')
}

// 自动保存到 localStorage
const autoSave = () => {
  if (!lf || isRestoring.value) return

  const data = lf.getGraphData()
  if (data.nodes.length > 0 || data.edges.length > 0) {
    localStorage.setItem(AUTOSAVE_KEY, JSON.stringify({
      data,
      timestamp: Date.now(),
      deployForm: deployForm.value
    }))
  }
}

// 从自动保存恢复数据
const restoreFromAutoSave = () => {
  if (!lf) return

  const saved = localStorage.getItem(AUTOSAVE_KEY)
  if (saved) {
    try {
      isRestoring.value = true
      const { data, deployForm: savedDeployForm } = JSON.parse(saved)
      if (data && (data.nodes.length > 0 || data.edges.length > 0)) {
        lf.clearData()
        lf.render(data)
        if (savedDeployForm) {
          deployForm.value = savedDeployForm
        }
        ElMessage.info('已恢复上次未保存的流程')
      }
    } catch (e) {
    } finally {
      isRestoring.value = false
    }
  }
}

// 启动自动保存定时器
const startAutoSave = () => {
  if (autoSaveTimer.value) {
    clearInterval(autoSaveTimer.value)
  }
  autoSaveTimer.value = window.setInterval(autoSave, AUTOSAVE_INTERVAL)
}

// 停止自动保存定时器
const stopAutoSave = () => {
  if (autoSaveTimer.value) {
    clearInterval(autoSaveTimer.value)
    autoSaveTimer.value = null
  }
}

const handleDeploy = () => {
  deployDialogVisible.value = true
}

const confirmDeploy = async () => {
  if (!deployForm.value.name || !deployForm.value.key) {
    ElMessage.warning('请填写流程名称和KEY')
    return
  }

  const bpmnXml = convertToBpmn()
  deployLoading.value = true

  try {
    await deployProcess({
      processName: deployForm.value.name,
      processKey: deployForm.value.key,
      bpmnXmlContent: bpmnXml,
      description: deployForm.value.description
    })
    ElMessage.success('流程部署成功')
    deployDialogVisible.value = false
    // 重置表单
    deployForm.value = {
      name: '',
      key: '',
      description: ''
    }
    hasUnsavedChanges.value = false
    // 清除自动保存的数据
    localStorage.removeItem(AUTOSAVE_KEY)
  } catch (error) {
    ElMessage.error('流程部署失败')
  } finally {
    deployLoading.value = false
  }
}

const convertToBpmn = (): string => {
  if (!lf) return ''

  const data = lf.getGraphData()
  let xml = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://flowable.org/demo">
  <process id="${deployForm.value.key}" name="${deployForm.value.name}" isExecutable="true">
`

  data.nodes.forEach((node: any) => {
    const nodeName = node.text?.value || node.id
    switch (node.type) {
      case 'bpmn:startEvent':
        xml += `    <startEvent id="${node.id}" name="${nodeName}"/>\n`
        break
      case 'bpmn:endEvent':
        xml += `    <endEvent id="${node.id}" name="${nodeName}"/>\n`
        break
      case 'bpmn:userTask': {
        const assignee = node.properties?.assignee || ''
        const groups = node.properties?.candidateGroups || []
        xml += `    <userTask id="${node.id}" name="${nodeName}"`
        if (assignee) {
          xml += ` flowable:assignee="${assignee}"`
        }
        if (groups.length > 0) {
          xml += ` flowable:candidateGroups="${groups.join(',')}"`
        }
        xml += `/>\n`
        break
      }
      case 'bpmn:serviceTask': {
        xml += `    <serviceTask id="${node.id}" name="${nodeName}"`
        if (node.properties?.className) {
          xml += ` flowable:class="${node.properties.className}"`
        }
        xml += `/>\n`
        break
      }
      case 'bpmn:scriptTask': {
        xml += `    <scriptTask id="${node.id}" name="${nodeName}"`
        if (node.properties?.scriptFormat) {
          xml += ` scriptFormat="${node.properties.scriptFormat}"`
        }
        xml += `/>\n`
        break
      }
      case 'bpmn:exclusiveGateway':
        xml += `    <exclusiveGateway id="${node.id}" name="${nodeName}"/>\n`
        break
      case 'bpmn:parallelGateway':
        xml += `    <parallelGateway id="${node.id}" name="${nodeName}"/>\n`
        break
      case 'bpmn:timerEvent': {
        xml += `    <intermediateCatchEvent id="${node.id}" name="${nodeName}">\n`
        xml += `      <timerEventDefinition>\n`
        if (node.properties?.timeDuration) {
          xml += `        <timeDuration>${node.properties.timeDuration}</timeDuration>\n`
        }
        xml += `      </timerEventDefinition>\n`
        xml += `    </intermediateCatchEvent>\n`
        break
      }
      default:
        xml += `    <task id="${node.id}" name="${nodeName}"/>\n`
    }
  })

  data.edges.forEach((edge: any, index: number) => {
    const edgeId = edge.id || `flow_${index}`
    const condition = edge.properties?.condition || ''
    xml += `    <sequenceFlow id="${edgeId}" sourceRef="${edge.sourceNodeId}" targetRef="${edge.targetNodeId}"`
    if (condition) {
      xml += `>\n      <conditionExpression xsi:type="tFormalExpression"><![CDATA[${condition}]]></conditionExpression>\n    </sequenceFlow>\n`
    } else {
      xml += `/>\n`
    }
  })

  xml += `  </process>
</definitions>`

  return xml
}

const handleZoomIn = () => {
  if (!lf) return
  lf.zoom(true)
}

const handleZoomOut = () => {
  if (!lf) return
  lf.zoom(false)
}

const handleResetZoom = () => {
  if (!lf) return
  lf.resetZoom()
}

const handleUndo = () => {
  if (!lf) return
  lf.undo()
}

const handleRedo = () => {
  if (!lf) return
  lf.redo()
}

const handleDownloadXML = () => {
  const bpmnXml = convertToBpmn()
  const blob = new Blob([bpmnXml], { type: 'application/xml' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${deployForm.value.name || 'process'}.bpmn20.xml`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('BPMN XML导出成功')
}

const handleDownloadImage = () => {
  if (!lf) return
  lf.getSnapshot()
  ElMessage.success('图片导出成功')
}

const handleClear = () => {
  if (!lf) return
  ElMessageBox.confirm(
    '确定要清空画布吗？未保存的内容将丢失。',
    '清空确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    lf?.clearData()
    hasUnsavedChanges.value = false
    localStorage.removeItem(AUTOSAVE_KEY)
    ElMessage.success('画布已清空')
  }).catch(() => {})
}

const updateNodeName = () => {
  if (!lf || selectedElementType.value !== 'node' || !selectedElement.value) return
  lf.setProperties(selectedElement.value.id, { name: nodeForm.value.name })
  lf.setText(selectedElement.value.id, { value: nodeForm.value.name })
  hasUnsavedChanges.value = true
}

const updateNodeProperty = (key: string, value: any) => {
  if (!lf || selectedElementType.value !== 'node' || !selectedElement.value) return
  lf.setProperties(selectedElement.value.id, { [key]: value })
  hasUnsavedChanges.value = true
}

const updateEdgeCondition = () => {
  if (!lf || selectedElementType.value !== 'edge' || !selectedElement.value) return
  lf.setProperties(selectedElement.value.id, { condition: edgeForm.value.condition })
  hasUnsavedChanges.value = true
}
</script>

<style scoped lang="scss">
.logicflow-designer {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-color-page);

  .toolbar {
    padding: 12px 20px;
    background: var(--card-bg);
    border-bottom: 1px solid var(--border-color-light);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  }

  .designer-content {
    flex: 1;
    display: flex;
    overflow: hidden;
    margin: 16px;
    gap: 16px;

    .node-panel {
      width: 200px;
      background: var(--card-bg);
      border-radius: 8px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
      overflow: hidden;

      .panel-title {
        padding: 12px 16px;
        font-weight: 600;
        border-bottom: 1px solid var(--border-color-light);
        background: var(--bg-color);
        color: var(--text-color-primary);
      }

      .node-list {
        padding: 12px;

        .node-item {
          display: flex;
          align-items: center;
          padding: 10px 12px;
          margin-bottom: 8px;
          background: var(--bg-color);
          border-radius: 6px;
          cursor: grab;
          transition: all 0.3s;

          &:hover {
            background: var(--primary-color-light-9);
            transform: translateX(4px);
          }

          &:active {
            cursor: grabbing;
          }

          .node-icon {
            width: 32px;
            height: 32px;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 6px;
            color: white;
            margin-right: 10px;
          }

          .node-name {
            font-size: 13px;
            color: var(--text-color-primary);
          }
        }
      }
    }

    .canvas-container {
      flex: 1;
      background: var(--card-bg);
      border-radius: 8px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
      overflow: hidden;
    }

    .properties-panel {
      width: 300px;
      background: var(--card-bg);
      border-radius: 8px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
      overflow: hidden;

      .panel-title {
        padding: 12px 16px;
        font-weight: 600;
        border-bottom: 1px solid var(--border-color-light);
        background: var(--bg-color);
        color: var(--text-color-primary);
      }

      .properties-form {
        padding: 16px;
      }

      .empty-tip {
        padding: 40px 20px;
      }
    }
  }
}

:deep(.lf-canvas-overlay) {
  background: var(--card-bg) !important;
}

:deep(.lf-graph) {
  background: var(--card-bg) !important;
}

:deep(.lf-node-content) {
  .lf-node-text {
    color: var(--text-color-primary) !important;
  }
}

:deep(.lf-menu) {
  background: var(--card-bg) !important;
  border: 1px solid var(--border-color-light) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
  border-radius: 8px !important;
  overflow: hidden !important;

  .lf-menu-item {
    color: var(--text-color-primary) !important;
    padding: 8px 16px !important;
    transition: all 0.2s !important;

    &:hover {
      background: var(--primary-color-light-9) !important;
      color: var(--primary-color) !important;
    }

    .lf-menu-item-icon {
      color: var(--text-color-secondary) !important;
    }
  }

  .lf-menu-item:first-child {
    border-radius: 8px 8px 0 0 !important;
  }

  .lf-menu-item:last-child {
    border-radius: 0 0 8px 8px !important;
  }
}

:deep(.lf-control) {
  background: var(--card-bg) !important;
  border: 1px solid var(--border-color-light) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1) !important;
  border-radius: 8px !important;
  overflow: hidden !important;

  .lf-control-item {
    color: var(--text-color-primary) !important;
    border-bottom: 1px solid var(--border-color-light) !important;

    &:hover {
      background: var(--primary-color-light-9) !important;
      color: var(--primary-color) !important;
    }

    &:last-child {
      border-bottom: none !important;
    }
  }
}

:deep(.lf-selection-select) {
  border: 1px solid var(--primary-color) !important;
  background: rgba(59, 130, 246, 0.1) !important;
}

:deep(.lf-edge) {
  .lf-edge-text {
    color: var(--text-color-secondary) !important;
  }
}
</style>
