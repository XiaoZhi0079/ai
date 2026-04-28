<template>
  <div>
    <div style="margin-bottom: 16px">
      <el-button type="warning" @click="handleGenKey">生成教师注册码</el-button>
    </div>

    <CrudTable
      :columns="columns"
      :api="api"
      :default-form="defaultForm"
      :rules="rules"
      :search-config="searchConfig"
      :filter-configs="filterConfigs"
      :category-config="categoryConfig"
      :readonly="tableFlags.readonly"
      :allow-create="tableFlags.allowCreate"
      :allow-edit="tableFlags.allowEdit"
      :allow-delete="tableFlags.allowDelete"
    />

    <el-dialog v-model="keyDialogVisible" title="教师注册码" width="460px">
      <p style="margin-bottom: 12px">请复制下面的注册码发给教师，该注册码仅可使用一次：</p>
      <el-input :model-value="generatedKey" readonly>
        <template #append>
          <el-button @click="copyKey">复制</el-button>
        </template>
      </el-input>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import CrudTable from '@/components/CrudTable.vue'
import type { CategoryConfig, Column, FilterConfig, SearchConfig } from '@/components/CrudTable.vue'
import { createCrudApi } from '@/api/crud'
import { generateRegistrationKey } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import type { Role, UserView } from '@/types'
import { getCrudTableFlags } from '@/access/moduleRules'

const userStore = useUserStore()
const api = createCrudApi<UserView>('/api/users')
const tableFlags = computed(() => getCrudTableFlags('users', (userStore.role || 'STUDENT') as Role))

const roleOptions = [
  { label: '管理员', value: 'ADMIN' },
  { label: '教师', value: 'TEACHER' },
  { label: '学生', value: 'STUDENT' }
]

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

const columns: Column[] = [
  { prop: 'id', label: 'ID', width: 60, tableOnly: true },
  { prop: 'username', label: '用户名' },
  { prop: 'password', label: '密码', type: 'password', formOnly: true },
  { prop: 'role', label: '身份', type: 'select', options: roleOptions },
  { prop: 'email', label: '邮箱' },
  { prop: 'status', label: '状态', type: 'select', options: statusOptions },
  { prop: 'createdTime', label: '创建时间', tableOnly: true }
]

const searchConfig: SearchConfig = {
  fields: ['username', 'email'],
  placeholder: '按用户名或邮箱搜索'
}

const categoryConfig: CategoryConfig = {
  prop: 'role',
  defaultValue: '',
  options: [
    { label: '全部', value: '' },
    ...roleOptions
  ]
}

const filterConfigs: FilterConfig[] = [
  { prop: 'status', label: '状态', options: statusOptions }
]

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择身份', trigger: 'change' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }]
}

const defaultForm = () => ({ username: '', password: '', role: 'STUDENT', email: '', status: 1 })

const keyDialogVisible = ref(false)
const generatedKey = ref('')

async function handleGenKey() {
  try {
    const res = await generateRegistrationKey()
    generatedKey.value = res.key
    keyDialogVisible.value = true
  } catch {
    // 错误已由全局拦截处理
  }
}

function copyKey() {
  navigator.clipboard.writeText(generatedKey.value).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}
</script>
