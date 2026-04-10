<template>
  <div class="demo-page">
    <div class="demo-header">
      <h2>对话框 (BlinkDialog)</h2>
      <p>封装的对话框组件，支持异步确认、自定义内容等</p>
    </div>

    <el-card>
      <template #header>组件预览</template>
      <el-space wrap>
        <el-button type="primary" @click="showBasic = true">基础对话框</el-button>
        <el-button type="success" @click="showForm = true">表单对话框</el-button>
        <el-button type="warning" @click="showConfirm">确认对话框</el-button>
        <el-button type="danger" @click="showCustom = true">自定义内容</el-button>
      </el-space>
    </el-card>

    <!-- 基础对话框 -->
    <el-dialog v-model="showBasic" title="基础对话框" width="500px">
      <p>这是一个基础对话框的内容区域</p>
      <template #footer>
        <el-button @click="showBasic = false">取消</el-button>
        <el-button type="primary" @click="showBasic = false">确定</el-button>
      </template>
    </el-dialog>

    <!-- 表单对话框 -->
    <el-dialog v-model="showForm" title="用户信息" width="500px">
      <el-form :model="formData" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="formData.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" @click="showForm = false">提交</el-button>
      </template>
    </el-dialog>

    <!-- 自定义内容对话框 -->
    <el-dialog v-model="showCustom" title="自定义内容" width="600px">
      <div class="custom-content">
        <el-result icon="success" title="操作成功" sub-title="您的数据已成功提交">
          <template #extra>
            <el-button type="primary">返回列表</el-button>
          </template>
        </el-result>
      </div>
    </el-dialog>

    <el-card style="margin-top: 24px">
      <template #header>Props</template>
      <el-table :data="propsData" border>
        <el-table-column prop="name" label="参数" width="150" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="type" label="类型" width="150" />
        <el-table-column prop="default" label="默认值" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessageBox } from 'element-plus'

const showBasic = ref(false)
const showForm = ref(false)
const showCustom = ref(false)

const formData = reactive({
  username: '',
  email: '',
  phone: '',
})

const showConfirm = () => {
  ElMessageBox.confirm('确定要执行此操作吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    // 确认操作
  }).catch(() => {
    // 取消操作
  })
}

const propsData = [
  { name: 'modelValue', desc: '是否显示', type: 'boolean', default: 'false' },
  { name: 'title', desc: '对话框标题', type: 'string', default: "''" },
  { name: 'width', desc: '对话框宽度', type: 'string | number', default: '50%' },
  { name: 'beforeClose', desc: '关闭前回调', type: 'Function', default: '-' },
]
</script>

<style scoped lang="scss">
.demo-page {
  .demo-header {
    margin-bottom: 24px;
    h2 { margin-bottom: 8px; }
    p { color: #909399; }
  }
}

.custom-content {
  padding: 24px 0;
}
</style>