<template>
  <el-dialog
    :title="dialogTitle"
    v-model="visible"
    width="550px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    @closed="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      class="group-form"
    >
      <el-form-item :label="t('group.parentGroup')" prop="groupParentId">
        <el-tree-select
          v-model="form.groupParentId"
          :data="groupTreeData"
          :props="{ label: 'groupName', value: 'groupId', children: 'children' }"
          :placeholder="t('common.pleaseSelect')"
          clearable
          check-strictly
          :render-after-expand="false"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item :label="t('group.groupName')" prop="groupName">
        <el-input v-model.trim="form.groupName" :placeholder="t('common.pleaseInput')" />
      </el-form-item>

      <el-form-item :label="t('group.groupEnName')" prop="groupEnName">
        <el-input v-model.trim="form.groupEnName" :placeholder="t('common.pleaseInput')" />
      </el-form-item>

      <el-form-item :label="t('group.groupNo')" prop="groupNo">
        <el-input v-model.trim="form.groupNo" :placeholder="t('common.pleaseInput')" />
      </el-form-item>

      <el-form-item :label="t('group.groupLeader')" prop="groupLeader">
        <el-input v-model.trim="form.groupLeader" :placeholder="t('common.pleaseInput')" />
      </el-form-item>

      <el-form-item :label="t('group.phone')" prop="phone">
        <el-input v-model.trim="form.phone" :placeholder="t('common.pleaseInput')" />
      </el-form-item>

      <el-form-item :label="t('group.groupAddress')" prop="groupAddress">
        <el-input v-model="form.groupAddress" type="textarea" :rows="2" :placeholder="t('common.pleaseInput')" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="isSubmitting" @click="handleSubmit">
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { addGroup, updateGroup, getGroupTree, type GroupInfo } from '@/api/group'
import { useSubmitGuard } from '@/composables/useSubmitGuard'

interface Props {
  modelValue: boolean
  type: 'add' | 'edit'
  data: GroupInfo | null
  parentGroup: GroupInfo | null
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue', 'success'])

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const dialogTitle = computed(() => 
  props.type === 'add' ? t('group.addGroup') : t('group.editGroup')
)

const formRef = ref<FormInstance>()
const groupTreeData = ref<GroupInfo[]>([])
const { isSubmitting, submitGuard } = useSubmitGuard()

const form = reactive({
  groupId: undefined as number | undefined,
  groupName: '',
  groupEnName: '',
  groupNo: '',
  groupParentId: undefined as number | undefined,
  groupLeader: '',
  groupAddress: '',
  phone: '',
})

const rules: FormRules = {
  groupParentId: [
    { required: true, message: t('common.pleaseSelect') + t('group.parentGroup'), trigger: 'change' },
  ],
  groupName: [
    { required: true, message: t('common.pleaseInput') + t('group.groupName'), trigger: 'blur' },
  ],
}

const fetchGroupTree = async () => {
  groupTreeData.value = await getGroupTree()
}

/**
 * 判断节点是否为叶子节点
 * @param groupId 节点ID
 * @param treeData 树形数据
 * @returns 是否为叶子节点 (1: 是, 0: 否)
 */
const checkIsLeaf = (groupId: number | undefined, treeData: GroupInfo[]): number => {
  if (!groupId) return 1

  // 递归查找节点
  const findNode = (id: number, nodes: GroupInfo[]): GroupInfo | null => {
    for (const node of nodes) {
      if (node.groupId === id) return node
      if (node.children && node.children.length > 0) {
        const found = findNode(id, node.children)
        if (found) return found
      }
    }
    return null
  }

  const node = findNode(groupId, treeData)
  // 如果有子节点则不是叶子节点
  if (node && node.children && node.children.length > 0) {
    return 0
  }
  return 1
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    await submitGuard(async () => {
      if (props.type === 'add') {
        // 新增组织时，默认是叶子节点
        await addGroup({
          groupName: form.groupName,
          groupEnName: form.groupEnName || undefined,
          groupNo: form.groupNo || undefined,
          groupParentId: form.groupParentId || 0,
          groupLeader: form.groupLeader || undefined,
          groupAddress: form.groupAddress || undefined,
          phone: form.phone || undefined,
          isLeaf: 1,
        })
        ElMessage.success(t('message.success'))
      } else {
        // 编辑组织时，根据是否有子节点判断是否为叶子节点
        const isLeaf = checkIsLeaf(form.groupId, groupTreeData.value)
        await updateGroup({
          groupId: form.groupId!,
          groupName: form.groupName,
          groupEnName: form.groupEnName || undefined,
          groupNo: form.groupNo || undefined,
          groupParentId: form.groupParentId,
          groupLeader: form.groupLeader || undefined,
          groupAddress: form.groupAddress || undefined,
          phone: form.phone || undefined,
          isLeaf,
        })
        ElMessage.success(t('message.success'))
      }
      visible.value = false
      emit('success')
    })
  })
}

const handleClose = () => {
  formRef.value?.resetFields()
  form.groupId = undefined
  form.groupName = ''
  form.groupEnName = ''
  form.groupNo = ''
  form.groupParentId = undefined
  form.groupLeader = ''
  form.groupAddress = ''
  form.phone = ''
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      fetchGroupTree()
      if (props.type === 'edit' && props.data) {
        form.groupId = props.data.groupId
        form.groupName = props.data.groupName
        form.groupEnName = props.data.groupEnName || ''
        form.groupNo = props.data.groupNo || ''
        form.groupParentId = props.data.groupParentId || undefined
        form.groupLeader = props.data.groupLeader || ''
        form.groupAddress = props.data.groupAddress || ''
        form.phone = props.data.phone || ''
      } else if (props.type === 'add' && props.parentGroup) {
        form.groupParentId = props.parentGroup.groupId
      }
    }
  }
)
</script>

<style scoped lang="scss">
.group-form {
  padding: 20px 20px 0;
}
</style>
