<template>
  <CrudTable :columns="columns" :api="api" :default-form="defaultForm" :rules="rules" />
</template>

<script setup lang="ts">
import CrudTable from '@/components/CrudTable.vue'
import type { Column } from '@/components/CrudTable.vue'
import { createCrudApi } from '@/api/crud'
import type { Student } from '@/types'

const api = createCrudApi<Student>('/api/students')

const columns: Column[] = [
  { prop: 'id', label: 'ID', width: 60, tableOnly: true },
  { prop: 'userId', label: '用户ID', type: 'number' },
  { prop: 'name', label: '姓名' },
  { prop: 'grade', label: '年级', type: 'number' },
  { prop: 'major', label: '专业' },
  { prop: 'className', label: '班级' },
  { prop: 'dormitory', label: '宿舍' },
  { prop: 'guardianPhone', label: '监护人电话' }
]

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }]
}

const defaultForm = () => ({ userId: undefined, name: '', grade: 2024, major: '', className: '', dormitory: '', guardianPhone: '' })
</script>
