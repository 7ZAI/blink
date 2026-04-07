<template>
  <div class="workflow-designer">
    <div class="designer-header">
      <div class="header-left">
        <span class="title">流程设计器</span>
      </div>
      <div class="header-right">
        <!-- 所有功能按钮已移至 LogicFlowDesigner 组件工具栏 -->
      </div>
    </div>
    <div class="designer-content">
      <LogicFlowDesigner ref="designerRef" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import LogicFlowDesigner from '@/components/LogicFlowDesigner/index.vue'

defineOptions({
  name: 'WorkflowDesigner'
})

const designerRef = ref()
</script>

<style scoped lang="scss">
.workflow-designer {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--card-bg);
  border-radius: 8px;
  overflow: hidden;
}

.designer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color-light);
  background: var(--card-bg);

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .title {
      font-size: 16px;
      font-weight: 500;
      color: var(--text-color-primary);
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.designer-content {
  flex: 1;
  overflow: hidden;
}
</style>
