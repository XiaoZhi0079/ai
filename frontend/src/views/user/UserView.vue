<template>
  <div>
    <div style="margin-bottom: 16px">
      <el-button type="warning" @click="handleGenKey">生成教师注册密钥</el-button>
    </div>
    <CrudTable :columns="columns" :api="api" :default-form="defaultForm" :rules="rules" />

    <el-dialog v-model="keyDialogVisible" title="教师注册密钥" width="460px">
      <p style="margin-bottom: 12px">请复制以下密钥发给教师，密钥仅可使用一次：</p>
      <el-input :model-value="generatedKey" readonly>
        <template #append>
          <el-button @click="copyKey">复制</el-button>
        </template>
      </el-input>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import CrudTable from '@/components/CrudTable.vue'
import type { Column } from '@/components/CrudTable.vue'
import { createCrudApi } from '@/api/crud'
import { generateRegistrationKey } from '@/api/auth'
import type { UserView } from '@/types'

const api = createCrudApi<UserView>('/api/users')

const columns: Column[] = [
  { prop: 'id', label: 'ID', width: 60, tableOnly: true },
  { prop: 'username', label: '用户名' },
  { prop: 'password', label: '密码', type: 'password', formOnly: true },
  { prop: 'role', label: '角色', type: 'select', options: [
    { label: '管理员', value: 'ADMIN' },
    { label: '教师', value: 'TEACHER' },
    { label: '学生', value: 'STUDENT' }
  ]},
  { prop: 'email', label: '邮箱' },
  { prop: 'status', label: '状态', type: 'number' },
  { prop: 'createdTime', label: '创建时间', tableOnly: true }
]

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
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
    // 错误已由全局拦截器处理
  }
}

function copyKey() {
  navigator.clipboard.writeText(generatedKey.value).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}
</script>
