<template>
  <div class="alert-rule-page">
    <!-- Header -->
    <div class="page-header">
      <h3>{{ t('alert.ruleManagement') }}</h3>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        {{ t('alert.addRule') }}
      </el-button>
    </div>

    <!-- Rule Cards -->
    <div class="rule-list">
      <el-card v-for="rule in rules" :key="rule.id" class="rule-card" shadow="hover">
        <div class="card-header">
          <span class="rule-name">{{ rule.ruleName }}</span>
          <div class="actions">
            <el-button link type="primary" @click="handleEdit(rule)">
              <el-icon><Edit /></el-icon>
              {{ t('common.edit') }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(rule)">
              <el-icon><Delete /></el-icon>
              {{ t('common.delete') }}
            </el-button>
          </div>
        </div>

        <div class="card-content">
          <el-tag size="small" effect="plain">{{ getRuleTypeLabel(rule.ruleType) }}</el-tag>
          <div class="conditions">
            <div v-for="(cond, idx) in rule.conditions" :key="idx" class="condition-item">
              {{ getMetricLabel(cond.metricName) }}
              {{ getOperatorLabel(cond.operator) }} {{ cond.threshold }}
              ({{ t('alert.duration') }} {{ cond.durationMinutes }}{{ t('alert.minutes') }})
            </div>
          </div>
        </div>

        <div class="card-footer">
          <span class="notify">{{ getNotifyChannels(rule.notifyChannels) }}</span>
          <span class="suppress">{{ t('alert.suppressInterval') }} {{ rule.suppressMinutes }}{{ t('alert.minutes') }}</span>
          <el-switch
            v-model="rule.enabled"
            :active-value="1"
            :inactive-value="0"
            size="small"
            @change="handleToggle(rule)"
          />
        </div>
      </el-card>

      <!-- Empty State -->
      <el-empty v-if="rules.length === 0" :description="t('alert.noRules')" />
    </div>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingRule ? t('alert.editRule') : t('alert.addRule')"
      width="600px"
      :close-on-click-modal="false"
      :lock-scroll="false"
    >
      <el-form :model="ruleForm" label-position="top" class="rule-form">
        <!-- Rule Name -->
        <el-form-item :label="t('alert.ruleName')" required>
          <el-input v-model="ruleForm.ruleName" placeholder="如: CPU使用率告警" />
        </el-form-item>

        <!-- Rule Type -->
        <el-form-item :label="t('alert.ruleType')" required>
          <el-select v-model="ruleForm.ruleType">
            <el-option v-for="opt in RULE_TYPE_OPTIONS" :key="opt.value" :value="opt.value" :label="opt.label" />
          </el-select>
        </el-form-item>

        <!-- Conditions -->
        <el-form-item :label="t('alert.conditions')" required>
          <div class="condition-list">
            <div v-for="(cond, idx) in ruleForm.conditions" :key="idx" class="condition-row">
              <el-row :gutter="8">
                <el-col :span="6">
                  <el-select v-model="cond.metricName" placeholder="指标">
                    <el-option v-for="opt in METRIC_OPTIONS" :key="opt.value" :value="opt.value" :label="opt.label" />
                  </el-select>
                </el-col>
                <el-col :span="4">
                  <el-select v-model="cond.operator">
                    <el-option v-for="opt in OPERATOR_OPTIONS" :key="opt.value" :value="opt.value" :label="opt.label" />
                  </el-select>
                </el-col>
                <el-col :span="4">
                  <el-input-number v-model="cond.threshold" :min="0" :controls="false" />
                </el-col>
                <el-col :span="4">
                  <el-input-number v-model="cond.durationMinutes" :min="1" :max="60" :controls="false" />
                  <span class="unit">{{ t('alert.minutes') }}</span>
                </el-col>
                <el-col :span="4">
                  <el-button type="danger" link @click="removeCondition(idx)" v-if="ruleForm.conditions.length > 1">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-col>
              </el-row>
            </div>

            <el-button type="primary" link @click="addCondition">
              + {{ t('alert.addCondition') }}
            </el-button>
          </div>
        </el-form-item>

        <!-- Severity -->
        <el-form-item :label="t('alert.severity')" required>
          <el-radio-group v-model="ruleForm.severity">
            <el-radio-button v-for="opt in SEVERITY_OPTIONS" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- Notify Channels -->
        <el-form-item :label="t('alert.notifyChannels')">
          <el-checkbox-group v-model="ruleForm.notifyChannels">
            <el-checkbox v-for="opt in NOTIFY_CHANNEL_OPTIONS" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <!-- Notify Template -->
        <el-form-item :label="t('alert.notifyTemplate')">
          <el-input
            v-model="ruleForm.notifyTemplate"
            type="textarea"
            :rows="3"
            :placeholder="t('alert.templatePlaceholder')"
          />
          <div class="template-vars">{{ t('alert.availableVars') }}: &#123;&#123;rule_name&#125;&#125;, &#123;&#123;instance_id&#125;&#125;, &#123;&#123;metric_name&#125;&#125;, &#123;&#123;value&#125;&#125;, &#123;&#123;threshold&#125;&#125;</div>
        </el-form-item>

        <!-- Suppress Minutes -->
        <el-form-item :label="t('alert.suppressInterval')">
          <el-input-number v-model="ruleForm.suppressMinutes" :min="1" :max="60" />
          <span class="unit">{{ t('alert.minutesNoRepeat') }}</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSave">
          {{ t('common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  alertApi,
  type AlertRule,
  type AlertCondition,
  RULE_TYPE_OPTIONS,
  METRIC_OPTIONS,
  OPERATOR_OPTIONS,
  SEVERITY_OPTIONS,
  NOTIFY_CHANNEL_OPTIONS,
} from '@/api/alert'

defineOptions({ name: 'AlertRuleManagement' })

const { t } = useI18n()

// State
const rules = ref<AlertRule[]>([])
const dialogVisible = ref(false)
const editingRule = ref<AlertRule | null>(null)
const submitting = ref(false)

// Form
const ruleForm = reactive<{
  id?: number
  ruleName: string
  ruleType: string
  conditions: AlertCondition[]
  severity: string
  notifyChannels: string[]
  notifyTemplate: string
  suppressMinutes: number
}>({
  ruleName: '',
  ruleType: 'RESOURCE',
  conditions: [createEmptyCondition()],
  severity: 'WARNING',
  notifyChannels: ['IN_APP'],
  notifyTemplate: '',
  suppressMinutes: 5,
})

// Create empty condition
function createEmptyCondition(): AlertCondition {
  return {
    metricName: '',
    operator: 'gt',
    threshold: 0,
    durationMinutes: 3,
  }
}

// Add condition
function addCondition() {
  ruleForm.conditions.push(createEmptyCondition())
}

// Remove condition
function removeCondition(idx: number) {
  ruleForm.conditions.splice(idx, 1)
}

// Load rules
async function loadRules() {
  try {
    const res = await alertApi.getRules({ pageNum: 1, pageSize: 100 })
    rules.value = res.rules || []
  } catch (error) {
    console.error('[AlertRules] Load failed:', error)
    ElMessage.error(t('common.loadFailed'))
  }
}

// Handle add
function handleAdd() {
  editingRule.value = null
  resetForm()
  dialogVisible.value = true
}

// Handle edit
function handleEdit(rule: AlertRule) {
  editingRule.value = rule
  ruleForm.id = rule.id
  ruleForm.ruleName = rule.ruleName
  ruleForm.ruleType = rule.ruleType
  ruleForm.conditions = [...rule.conditions]
  ruleForm.severity = rule.severity
  ruleForm.notifyChannels = [...rule.notifyChannels]
  ruleForm.notifyTemplate = rule.notifyTemplate || ''
  ruleForm.suppressMinutes = rule.suppressMinutes
  dialogVisible.value = true
}

// Handle delete
async function handleDelete(rule: AlertRule) {
  try {
    await ElMessageBox.confirm(
      t('alert.deleteConfirm', { name: rule.ruleName }),
      t('common.confirm'),
      { type: 'warning', lockScroll: false }
    )
    await alertApi.deleteRule(rule.id)
    ElMessage.success(t('common.deleteSuccess'))
    loadRules()
  } catch {
    // User cancelled
  }
}

// Handle toggle
async function handleToggle(rule: AlertRule) {
  try {
    await alertApi.toggleRule(rule.id, rule.enabled)
    ElMessage.success(t('common.success'))
  } catch (error) {
    // Restore original state
    rule.enabled = rule.enabled === 1 ? 0 : 1
    ElMessage.error(t('common.failed'))
  }
}

// Handle save
async function handleSave() {
  // Validate
  if (!ruleForm.ruleName) {
    ElMessage.warning(t('alert.ruleNameRequired'))
    return
  }
  if (ruleForm.conditions.some(c => !c.metricName)) {
    ElMessage.warning(t('alert.metricRequired'))
    return
  }

  submitting.value = true
  try {
    if (editingRule.value) {
      await alertApi.updateRule(ruleForm as any)
    } else {
      await alertApi.addRule(ruleForm as any)
    }
    ElMessage.success(t('common.saveSuccess'))
    dialogVisible.value = false
    loadRules()
  } catch (error) {
    ElMessage.error(t('common.saveFailed'))
  } finally {
    submitting.value = false
  }
}

// Reset form
function resetForm() {
  ruleForm.id = undefined
  ruleForm.ruleName = ''
  ruleForm.ruleType = 'RESOURCE'
  ruleForm.conditions = [createEmptyCondition()]
  ruleForm.severity = 'WARNING'
  ruleForm.notifyChannels = ['IN_APP']
  ruleForm.notifyTemplate = ''
  ruleForm.suppressMinutes = 5
}

// Get labels
function getRuleTypeLabel(type: string): string {
  return RULE_TYPE_OPTIONS.find(o => o.value === type)?.label || type
}

function getMetricLabel(metric: string): string {
  return METRIC_OPTIONS.find(o => o.value === metric)?.label || metric
}

function getOperatorLabel(op: string): string {
  return OPERATOR_OPTIONS.find(o => o.value === op)?.label || op
}

function getNotifyChannels(channels: string[]): string {
  return channels.map(c => NOTIFY_CHANNEL_OPTIONS.find(o => o.value === c)?.label || c).join('+')
}

onMounted(() => {
  loadRules()
})
</script>

<style scoped lang="scss">
.alert-rule-page {
  padding: 24px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    h3 {
      font-size: 20px;
      font-weight: 600;
      margin: 0;
    }
  }

  .rule-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .rule-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .rule-name {
        font-size: 16px;
        font-weight: 500;
      }

      .actions {
        display: flex;
        gap: 8px;
      }
    }

    .card-content {
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin-bottom: 12px;

      .conditions {
        display: flex;
        flex-direction: column;
        gap: 4px;
        color: var(--text-color-secondary);
        font-size: 13px;
      }
    }

    .card-footer {
      display: flex;
      gap: 16px;
      align-items: center;
      font-size: 13px;
      color: var(--text-color-secondary);
    }
  }

  .rule-form {
    .condition-list {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .condition-row {
        display: flex;
        align-items: center;
      }
    }

    .unit {
      margin-left: 4px;
      color: var(--text-color-secondary);
    }

    .template-vars {
      margin-top: 4px;
      font-size: 12px;
      color: var(--text-color-secondary);
    }
  }
}
</style>