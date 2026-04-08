<template>
  <div class="config-container">
    <div class="config-sidebar">
      <div class="sidebar-header">
        <BlinkIcon icon="mdi:router-wireless" size="20" class="header-icon" />
        <span class="sidebar-title">{{ t('config.gatewayConfig') }}</span>
      </div>
      <div class="sidebar-search">
        <el-input
          v-model.trim="searchKeyword"
          :placeholder="t('common.search')"
          clearable
          size="small"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="sidebar-menu">
        <div
          v-for="group in configGroups"
          :key="group.key"
          class="menu-item"
          :class="{ 'is-active': activeGroup === group.key }"
          @click="handleGroupChange(group.key)"
        >
          <div class="menu-icon-wrapper">
            <BlinkIcon :icon="group.icon" size="18" />
          </div>
          <span class="menu-label">{{ group.label }}</span>
          <div class="menu-indicator"></div>
        </div>
      </div>
    </div>

    <div class="config-content">
      <div class="content-header">
        <div class="header-left">
          <BlinkIcon :icon="currentGroupIcon" size="22" class="group-icon" />
          <span class="content-title">{{ currentGroupLabel }}</span>
        </div>
        <div class="header-right">
          <span class="config-count">{{ filteredConfigs.length }} {{ t('config.items') }}</span>
          <el-tag v-if="hasChanges" type="warning" size="small" class="unsaved-tag">
            {{ t('config.unsavedChanges') }}
          </el-tag>
        </div>
      </div>

      <div v-loading="loading" class="content-body">
        <div v-if="filteredConfigs.length === 0" class="empty-state">
          <el-empty :description="t('config.noConfig')" />
        </div>

        <div v-else class="config-table">
          <div class="config-row header-row">
            <div class="col-name">{{ t('config.configName') }}</div>
            <div class="col-value">{{ t('config.configValue') }}</div>
            <div class="col-desc">{{ t('config.description') }}</div>
          </div>

          <div
            v-for="config in filteredConfigs"
            :key="config.id"
            class="config-row"
            :class="{ 'is-modified': isModified(config.id) }"
          >
            <div class="col-name">
              <span class="name-text">{{ config.configName }}</span>
              <span class="config-key">{{ config.configKey }}</span>
            </div>
            <div class="col-value">
              <template v-if="config.configType === 2">
                <el-switch
                  v-model="configStates[config.id]"
                  size="small"
                  @change="markAsModified(config.id)"
                />
              </template>

              <template v-else-if="config.configType === 1">
                <el-input-number
                  v-model="configStates[config.id]"
                  :min="0"
                  controls-position="right"
                  size="small"
                  style="width: 120px"
                  @change="markAsModified(config.id)"
                />
              </template>

              <template v-else-if="config.configType === 3 || config.configType === 4">
                <el-button type="primary" size="small" text @click="openJsonEditor(config)">
                  <el-icon><Edit /></el-icon>
                  {{ t('config.editJson') }}
                </el-button>
              </template>

              <template v-else>
                <el-input
                  v-model.trim="configStates[config.id]"
                  :placeholder="t('common.pleaseInput')"
                  size="small"
                  style="width: 180px"
                  @input="markAsModified(config.id)"
                />
              </template>
            </div>
            <div class="col-desc">{{ config.description }}</div>
          </div>

          <!-- 底部操作按钮 -->
          <div class="config-row action-row" v-if="hasChanges">
            <div class="col-name">
              <span class="changes-count">{{ t('config.modifiedCount', { count: modifiedIds.length }) }}</span>
            </div>
            <div class="col-value">
              <div class="action-buttons">
                <el-button @click="handleCancel">{{ t('common.cancel') }}</el-button>
                <AuthButton :has-permission="() => checkPermission(ButtonPerms.Config.Update)" type="primary" :loading="saving" @click="handleBatchSave">
                  <el-icon><Check /></el-icon>
                  {{ t('common.save') }}
                </AuthButton>
              </div>
            </div>
            <div class="col-desc"></div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="jsonEditorVisible"
      :title="t('config.editJson')"
      width="650px"
      :close-on-click-modal="false"
      class="json-dialog"
    >
      <div class="json-editor-wrapper">
        <el-input
          v-model="jsonEditorContent"
          type="textarea"
          :rows="18"
          :placeholder="t('config.jsonPlaceholder')"
          class="json-textarea"
        />
      </div>
      <template #footer>
        <el-button @click="jsonEditorVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveJsonConfig">
          <el-icon><Check /></el-icon>
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { Search, Check, Edit } from '@element-plus/icons-vue'
import { getConfigsByGroupKey, batchUpdateConfigs, type ConfigItem } from '@/api/config'
import { ButtonPerms, usePermission } from '@/composables/usePermission'

defineOptions({
  name: 'ConfigManagement',
})

const { t } = useI18n()
const { hasPermission: checkPermission } = usePermission()

const loading = ref(false)
const saving = ref(false)
const searchKeyword = ref('')
const activeGroup = ref('gateway-security')
const currentConfigs = ref<ConfigItem[]>([])
const configStates = ref<Record<number, any>>({})
const originalStates = ref<Record<number, any>>({})
const modifiedIds = ref<number[]>([])

const jsonEditorVisible = ref(false)
const jsonEditorContent = ref('')
const currentJsonConfig = ref<ConfigItem | null>(null)

// 网关配置分组（使用 gateway 分组下的配置）
const configGroups = computed(() => [
  { key: 'gateway-security', label: t('config.groupSecurity'), icon: 'mdi:shield-check' },
  { key: 'gateway-ip', label: t('config.groupIpFilter'), icon: 'mdi:filter' },
  { key: 'gateway-route', label: t('config.groupRoute'), icon: 'mdi:routes' },
  { key: 'gateway-system', label: t('config.groupSystem'), icon: 'mdi:cog' },
])

// 网关配置键前缀映射到子分组
const groupKeyMapping: Record<string, string[]> = {
  'gateway-security': ['gateway:signature', 'gateway:replay'],
  'gateway-ip': ['gateway:ip'],
  'gateway-route': ['gateway:route'],
  'gateway-system': ['gateway:event', 'gateway:local', 'gateway:api'],
}

const currentGroupLabel = computed(() => {
  const group = configGroups.value.find(g => g.key === activeGroup.value)
  return group?.label || ''
})

const currentGroupIcon = computed(() => {
  const group = configGroups.value.find(g => g.key === activeGroup.value)
  return group?.icon || 'mdi:cog'
})

const filteredConfigs = computed(() => {
  const prefixes = groupKeyMapping[activeGroup.value] || []
  let configs = currentConfigs.value

  // 按前缀过滤
  if (prefixes.length > 0) {
    configs = configs.filter(config =>
      prefixes.some(prefix => config.configKey.startsWith(prefix))
    )
  }

  // 按搜索关键词过滤
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    configs = configs.filter(config =>
      config.configName.toLowerCase().includes(keyword) ||
      config.description.toLowerCase().includes(keyword) ||
      config.configKey.toLowerCase().includes(keyword)
    )
  }

  return configs
})

const hasChanges = computed(() => modifiedIds.value.length > 0)

const isModified = (id: number) => modifiedIds.value.includes(id)

const markAsModified = (id: number) => {
  if (!modifiedIds.value.includes(id)) {
    modifiedIds.value.push(id)
  }
}

const fetchConfigs = async () => {
  loading.value = true
  modifiedIds.value = []
  try {
    // 获取 gateway 分组的所有配置
    const data = await getConfigsByGroupKey('gateway')
    currentConfigs.value = data?.configs || []

    const states: Record<number, any> = {}
    const originals: Record<number, any> = {}
    currentConfigs.value.forEach((config) => {
      if (config.configType === 2) {
        states[config.id] = config.configValue === 'true'
        originals[config.id] = config.configValue === 'true'
      } else if (config.configType === 1) {
        states[config.id] = parseInt(config.configValue) || 0
        originals[config.id] = parseInt(config.configValue) || 0
      } else {
        states[config.id] = config.configValue
        originals[config.id] = config.configValue
      }
    })
    configStates.value = states
    originalStates.value = originals
  } catch (error) {
    currentConfigs.value = []
  } finally {
    loading.value = false
  }
}

const handleGroupChange = (groupKey: string) => {
  if (hasChanges.value) {
    ElMessage.warning(t('config.pleaseSaveOrCancel'))
    return
  }
  activeGroup.value = groupKey
  searchKeyword.value = ''
}

const openJsonEditor = (config: ConfigItem) => {
  currentJsonConfig.value = config
  try {
    const parsed = JSON.parse(config.configValue)
    jsonEditorContent.value = JSON.stringify(parsed, null, 2)
  } catch {
    jsonEditorContent.value = config.configValue
  }
  jsonEditorVisible.value = true
}

const saveJsonConfig = () => {
  if (!currentJsonConfig.value) return

  try {
    JSON.parse(jsonEditorContent.value)
  } catch {
    ElMessage.error(t('config.invalidJson'))
    return
  }

  configStates.value[currentJsonConfig.value.id] = jsonEditorContent.value
  if (!modifiedIds.value.includes(currentJsonConfig.value.id)) {
    modifiedIds.value.push(currentJsonConfig.value.id)
  }
  jsonEditorVisible.value = false
}

const handleBatchSave = async () => {
  if (modifiedIds.value.length === 0) {
    ElMessage.warning(t('config.noChanges'))
    return
  }

  saving.value = true
  try {
    const configs = modifiedIds.value.map(id => {
      const config = currentConfigs.value.find(c => c.id === id)
      let value = configStates.value[id]
      if (config?.configType === 2) {
        value = value ? 'true' : 'false'
      } else if (config?.configType === 1) {
        value = String(value)
      }
      return {
        id,
        configKey: config!.configKey,
        configValue: value
      }
    })

    await batchUpdateConfigs(configs)
    ElMessage.success(t('message.saveSuccess'))
    modifiedIds.value = []

    // 更新原始状态
    currentConfigs.value.forEach((config) => {
      originalStates.value[config.id] = configStates.value[config.id]
    })
  } catch (error) {
  } finally {
    saving.value = false
  }
}

const handleCancel = () => {
  // 恢复到原始状态
  configStates.value = { ...originalStates.value }
  modifiedIds.value = []
}

onMounted(() => {
  fetchConfigs()
})
</script>

<style scoped lang="scss">
.config-container {
  display: flex;
  height: 100%;
  background: var(--bg-color-page);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.config-sidebar {
  width: 220px;
  background: var(--card-bg);
  border-right: 1px solid var(--border-color-light);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;

  .sidebar-header {
    padding: 16px;
    border-bottom: 1px solid var(--border-color-light);
    display: flex;
    align-items: center;
    gap: 10px;

    .header-icon {
      color: var(--primary-color);
    }

    .sidebar-title {
      font-size: 14px;
      font-weight: 600;
      color: var(--text-color-primary);
    }
  }

  .sidebar-search {
    padding: 12px;
    border-bottom: 1px solid var(--border-color-light);
  }

  .sidebar-menu {
    flex: 1;
    overflow-y: auto;
    padding: 8px;

    .menu-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px 12px;
      margin: 2px 0;
      cursor: pointer;
      transition: all 0.2s ease;
      color: var(--text-color-regular);
      font-size: 13px;
      border-radius: 6px;
      position: relative;

      &:hover {
        background: var(--bg-color);
        color: var(--primary-color);
      }

      &.is-active {
        background: var(--primary-color-light-9);
        color: var(--primary-color);
        font-weight: 500;
      }

      .menu-icon-wrapper {
        width: 28px;
        height: 28px;
        border-radius: 6px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--bg-color);
        transition: all 0.2s ease;
      }

      .menu-label {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
}

.config-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--bg-color);
  overflow: hidden;

  .content-header {
    padding: 16px 20px;
    border-bottom: 1px solid var(--border-color-light);
    background: var(--card-bg);
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-shrink: 0;

    .header-left {
      display: flex;
      align-items: center;
      gap: 10px;

      .group-icon {
        color: var(--primary-color);
      }

      .content-title {
        font-size: 16px;
        font-weight: 600;
        color: var(--text-color-primary);
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 12px;

      .config-count {
        font-size: 12px;
        color: var(--text-color-secondary);
        background: var(--bg-color);
        padding: 4px 10px;
        border-radius: 10px;
      }

      .unsaved-tag {
        animation: pulse 2s infinite;
      }
    }
  }

  .content-body {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    min-height: 0;
  }
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
}

.config-table {
  background: var(--card-bg);
  border-radius: 8px;
  border: 1px solid var(--border-color-light);
  overflow: hidden;

  .config-row {
    display: flex;
    align-items: center;
    border-bottom: 1px solid var(--border-color-lighter);
    transition: background 0.2s ease;

    &:last-child {
      border-bottom: none;
    }

    &:hover:not(.header-row) {
      background: var(--bg-color-page);
    }

    &.is-modified {
      background: var(--primary-color-light-9);
      border-left: 3px solid var(--primary-color);

      .col-name {
        padding-left: 9px;
      }
    }

    &.header-row {
      background: var(--bg-color);
      font-weight: 500;
      color: var(--text-color-secondary);
      font-size: 12px;

      .col-name,
      .col-desc,
      .col-value {
        padding: 10px 12px;
      }
    }

    &.action-row {
      background: var(--primary-color-light-9);
      border-top: 2px solid var(--primary-color-light-7);

      .col-name {
        .changes-count {
          font-size: 13px;
          color: var(--text-color-secondary);
        }
      }

      .col-value {
        .action-buttons {
          display: flex;
          align-items: center;
          gap: 8px;
        }
      }
    }

    .col-name {
      width: 200px;
      flex-shrink: 0;
      padding: 12px;
      display: flex;
      flex-direction: column;
      gap: 2px;

      .name-text {
        font-size: 13px;
        font-weight: 500;
        color: var(--text-color-primary);
      }

      .config-key {
        font-size: 11px;
        color: var(--text-color-placeholder);
        font-family: 'Monaco', 'Menlo', monospace;
      }
    }

    .col-value {
      width: 280px;
      flex-shrink: 0;
      padding: 12px;
      display: flex;
      align-items: center;
      justify-content: flex-start;
    }

    .col-desc {
      flex: 1;
      padding: 12px;
      font-size: 12px;
      color: var(--text-color-secondary);
      line-height: 1.5;
    }
  }
}

:deep(.el-input-number) {
  .el-input__wrapper {
    border-radius: 4px;
  }
}

:deep(.el-switch) {
  --el-switch-on-color: var(--primary-color);
}

:deep(.el-input) {
  .el-input__wrapper {
    border-radius: 4px;
  }
}

.json-dialog {
  .json-editor-wrapper {
    background: var(--bg-color-page);
    border-radius: 8px;
    padding: 4px;
  }

  .json-textarea {
    :deep(.el-textarea__inner) {
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      font-size: 13px;
      line-height: 1.6;
      background: transparent;
      border: none;
      box-shadow: none;
      border-radius: 8px;
    }
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}
</style>