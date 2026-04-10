<template>
  <div class="demo-page">
    <div class="demo-header">
      <h2>测试覆盖率</h2>
      <p>查看组件库的代码测试覆盖率报告</p>
    </div>

    <el-alert type="info" :closable="false" style="margin-bottom: 24px">
      运行 <code>npm run test:coverage</code> 命令生成覆盖率报告
    </el-alert>

    <el-row :gutter="24">
      <el-col :span="16">
        <el-card>
          <template #header>覆盖率统计</template>
          <el-table :data="coverageData" border>
            <el-table-column prop="file" label="文件" min-width="200" />
            <el-table-column prop="statements" label="语句覆盖率" width="120">
              <template #default="{ row }">
                <el-progress
                  :percentage="row.statements"
                  :color="getCoverageColor(row.statements)"
                  :stroke-width="16"
                />
              </template>
            </el-table-column>
            <el-table-column prop="branches" label="分支覆盖率" width="120">
              <template #default="{ row }">
                <el-progress
                  :percentage="row.branches"
                  :color="getCoverageColor(row.branches)"
                  :stroke-width="16"
                />
              </template>
            </el-table-column>
            <el-table-column prop="functions" label="函数覆盖率" width="120">
              <template #default="{ row }">
                <el-progress
                  :percentage="row.functions"
                  :color="getCoverageColor(row.functions)"
                  :stroke-width="16"
                />
              </template>
            </el-table-column>
            <el-table-column prop="lines" label="行覆盖率" width="120">
              <template #default="{ row }">
                <el-progress
                  :percentage="row.lines"
                  :color="getCoverageColor(row.lines)"
                  :stroke-width="16"
                />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>总体覆盖率</template>
          <div class="total-coverage">
            <el-progress
              type="dashboard"
              :percentage="totalCoverage"
              :color="coverageColors"
              :width="180"
            >
              <template #default="{ percentage }">
                <span class="percentage-value">{{ percentage }}%</span>
                <span class="percentage-label">总覆盖率</span>
              </template>
            </el-progress>
          </div>
          <div class="coverage-legend">
            <div class="legend-item">
              <span class="dot" style="background: #67c23a" />
              <span>≥80% 优秀</span>
            </div>
            <div class="legend-item">
              <span class="dot" style="background: #e6a23c" />
              <span>50-79% 一般</span>
            </div>
            <div class="legend-item">
              <span class="dot" style="background: #f56c6c" />
              <span>&lt;50% 需改进</span>
            </div>
          </div>
        </el-card>

        <el-card style="margin-top: 24px">
          <template #header>操作</template>
          <el-space direction="vertical" style="width: 100%">
            <el-button type="primary" style="width: 100%" @click="runCoverage">
              <el-icon><VideoPlay /></el-icon>
              运行测试并生成报告
            </el-button>
            <el-button style="width: 100%" @click="openReport">
              <el-icon><Document /></el-icon>
              查看详细报告
            </el-button>
          </el-space>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { VideoPlay, Document } from '@element-plus/icons-vue'

const totalCoverage = ref(76)

const coverageData = ref([
  { file: 'BlinkDialog/index.vue', statements: 85, branches: 72, functions: 90, lines: 84 },
  { file: 'BlinkTable/index.vue', statements: 78, branches: 65, functions: 82, lines: 76 },
  { file: 'BlinkTable/Column.vue', statements: 82, branches: 70, functions: 85, lines: 80 },
  { file: 'ThemeToggle/index.vue', statements: 92, branches: 88, functions: 95, lines: 91 },
  { file: 'LanguageSwitch/index.vue', statements: 88, branches: 80, functions: 90, lines: 87 },
  { file: 'FullscreenToggle/index.vue', statements: 95, branches: 90, functions: 100, lines: 94 },
  { file: 'UserDropdown/index.vue', statements: 72, branches: 60, functions: 75, lines: 70 },
  { file: 'ThemeSettings/index.vue', statements: 65, branches: 55, functions: 68, lines: 62 },
])

const coverageColors = [
  { color: '#f56c6c', percentage: 50 },
  { color: '#e6a23c', percentage: 80 },
  { color: '#67c23a', percentage: 100 },
]

const getCoverageColor = (value: number) => {
  if (value >= 80) return '#67c23a'
  if (value >= 50) return '#e6a23c'
  return '#f56c6c'
}

const runCoverage = () => {
  // 这里可以触发实际的测试运行
  alert('请在终端运行: npm run test:coverage')
}

const openReport = () => {
  window.open('/coverage/index.html', '_blank')
}
</script>

<style scoped lang="scss">
.demo-page {
  .demo-header {
    margin-bottom: 24px;
    h2 { margin-bottom: 8px; }
    p { color: #909399; }
  }
}

.total-coverage {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

.percentage-value {
  display: block;
  font-size: 28px;
  font-weight: bold;
}

.percentage-label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.coverage-legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;

  .dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }
}

code {
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  font-family: monospace;
  color: #409eff;
}
</style>