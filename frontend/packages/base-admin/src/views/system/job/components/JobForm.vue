<template>
  <el-dialog :title="title" v-model="visible" width="600px" @close="resetForm">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="任务名称" prop="jobName">
        <el-input v-model="form.jobName" placeholder="请输入任务名称" :disabled="isEdit" />
      </el-form-item>
      <el-form-item label="任务分组" prop="jobGroup">
        <el-input v-model="form.jobGroup" placeholder="请输入任务分组" />
      </el-form-item>
      <el-form-item label="Cron表达式" prop="cronExpression">
        <el-input v-model="form.cronExpression" placeholder="请输入Cron表达式" />
      </el-form-item>
      <el-form-item label="任务描述" prop="jobDescription">
        <el-input v-model="form.jobDescription" type="textarea" placeholder="请输入任务描述" />
      </el-form-item>
      <el-form-item label="执行目标" prop="targetBean">
        <el-input v-model="form.targetBean" placeholder="Bean名称" />
      </el-form-item>
      <el-form-item label="执行方法" prop="targetMethod">
        <el-input v-model="form.targetMethod" placeholder="方法名称" />
      </el-form-item>
      <el-form-item label="是否启用" prop="enabled">
        <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="超时时间" prop="timeout">
        <el-input-number v-model="form.timeout" :min="-1" placeholder="-1表示不超时" />
        <span class="ml-2 text-gray-400 text-sm">毫秒</span>
      </el-form-item>
      <el-form-item label="重试次数" prop="retryCount">
        <el-input-number v-model="form.retryCount" :min="0" />
      </el-form-item>
      <el-form-item label="重试间隔" prop="retryInterval">
        <el-input-number v-model="form.retryInterval" :min="0" />
        <span class="ml-2 text-gray-400 text-sm">毫秒</span>
      </el-form-item>
      <el-form-item label="任务参数" prop="parameters">
        <el-input v-model="form.parameters" type="textarea" :rows="3" placeholder="JSON格式参数" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="submitForm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { addJob, updateJob } from '@/api/job'
import type { SysJobVO, AddSysJobReq, UpdateSysJobReq } from '@/api/job'

const emit = defineEmits(['success'])

const visible = ref(false)
const isEdit = ref(false)
const title = computed(() => (isEdit.value ? '编辑任务' : '新增任务'))

const formRef = ref()
const form = reactive({
  jobId: undefined as number | undefined,
  jobName: '',
  jobGroup: 'default',
  jobDescription: '',
  cronExpression: '',
  targetBean: '',
  targetMethod: '',
  enabled: 1,
  timeout: -1,
  retryCount: 0,
  retryInterval: 1000,
  parameters: ''
})

const rules = {
  jobName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  cronExpression: [{ required: true, message: '请输入Cron表达式', trigger: 'blur' }]
}

// 打开对话框
const open = (row?: SysJobVO) => {
  resetForm()
  if (row) {
    isEdit.value = true
    Object.assign(form, {
      jobId: row.jobId,
      jobName: row.jobName,
      jobGroup: row.jobGroup,
      jobDescription: row.jobDescription,
      cronExpression: row.cronExpression,
      targetBean: row.targetBean,
      targetMethod: row.targetMethod,
      enabled: row.enabled,
      timeout: row.timeout,
      retryCount: row.retryCount,
      retryInterval: row.retryInterval,
      parameters: row.parameters
    })
  } else {
    isEdit.value = false
  }
  visible.value = true
}

// 重置表单
const resetForm = () => {
  form.jobId = undefined
  form.jobName = ''
  form.jobGroup = 'default'
  form.jobDescription = ''
  form.cronExpression = ''
  form.targetBean = ''
  form.targetMethod = ''
  form.enabled = 1
  form.timeout = -1
  form.retryCount = 0
  form.retryInterval = 1000
  form.parameters = ''
  formRef.value?.resetFields()
}

// 提交表单
const submitForm = async () => {
  try {
    await formRef.value?.validate()
    if (isEdit.value) {
      await updateJob(form as UpdateSysJobReq)
      ElMessage.success('修改成功')
    } else {
      await addJob(form as AddSysJobReq)
      ElMessage.success('新增成功')
    }
    visible.value = false
    emit('success')
  } catch {
    // 验证失败
  }
}

defineExpose({ open })
</script>
