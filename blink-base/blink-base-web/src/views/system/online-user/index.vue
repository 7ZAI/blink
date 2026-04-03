<template>
  <div class="online-user-page table-page-container">
    <el-card class="simple-table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <el-button type="primary" @click="fetchUserList">
            <el-icon><Refresh /></el-icon>
            {{ t('common.refresh') }}
          </el-button>
        </div>
      </template>

      <div class="table-wrapper">
        <el-table
          v-loading="loading"
          :data="userList"
          height="100%"
          stripe
          border
        >
          <el-table-column prop="userId" :label="t('onlineUser.userId')" width="100" align="center" />
          <el-table-column prop="loginName" :label="t('onlineUser.loginName')" min-width="120" />
          <el-table-column prop="username" :label="t('onlineUser.username')" min-width="120" />
          <el-table-column prop="loginTime" :label="t('onlineUser.loginTime')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.loginTime) }}
            </template>
          </el-table-column>
          <el-table-column :label="t('common.operation')" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <div class="operation-buttons">
                <AuthButton :perm="ButtonPerms.OnlineUser.Kickout" type="danger" link size="small" @click="handleKickout(row)">
                  <el-icon><SwitchButton /></el-icon>
                  {{ t('onlineUser.kickout') }}
                </AuthButton>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onActivated } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, SwitchButton } from '@element-plus/icons-vue'
import { getOnlineUserList, kickoutUser, type OnlineUser } from '@/api/online-user'
import { ButtonPerms } from '@/composables/usePermission'
import AuthButton from '@/components/AuthButton.vue'

defineOptions({
  name: 'SystemOnlineUser',
})

const { t } = useI18n()

const loading = ref(false)
const userList = ref<OnlineUser[]>([])
const allUsers = ref<OnlineUser[]>([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ')
}

const fetchUserList = async () => {
  loading.value = true
  try {
    const res = await getOnlineUserList()
    allUsers.value = res.rows || []
    pagination.total = allUsers.value.length
    updatePagedList()
  } catch (error) {
    allUsers.value = []
    userList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const updatePagedList = () => {
  const start = (pagination.pageNum - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  userList.value = allUsers.value.slice(start, end)
}

const handleKickout = async (row: OnlineUser) => {
  try {
    await ElMessageBox.confirm(
      t('onlineUser.kickoutConfirm', { name: row.loginName }),
      t('message.tips'),
      { type: 'warning' }
    )
    await kickoutUser(row.token)
    ElMessage.success(t('message.success'))
    fetchUserList()
  } catch {
    // 用户取消操作
  }
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  updatePagedList()
}

const handleCurrentChange = (page: number) => {
  pagination.pageNum = page
  updatePagedList()
}

// 组件挂载时加载数据
onMounted(() => {
  fetchUserList()
})

// 从 keep-alive 缓存恢复时重新加载数据
onActivated(() => {
  fetchUserList()
})
</script>

<style scoped lang="scss">
/* 在线用户页面样式 - 继承全局 table-page-container 和 simple-table-card 样式 */
.online-user-page {
  /* 可添加页面特定的样式覆盖 */
}
</style>