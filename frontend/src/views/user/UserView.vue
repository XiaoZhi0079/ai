<template>
  <CrudTable :columns="columns" :api="api" :default-form="defaultForm" :rules="rules" />
</template>

<script setup lang="ts">
import CrudTable from '@/components/CrudTable.vue'
import type { Column } from '@/components/CrudTable.vue'
import { createCrudApi } from '@/api/crud'
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
</script>
