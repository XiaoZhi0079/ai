<template>
  <CrudTable :columns="columns" :api="api" :default-form="defaultForm" :rules="rules" />
</template>

<script setup lang="ts">
import CrudTable from '@/components/CrudTable.vue'
import type { Column } from '@/components/CrudTable.vue'
import { createCrudApi } from '@/api/crud'
import type { Course } from '@/types'

const api = createCrudApi<Course>('/api/courses')

const columns: Column[] = [
  { prop: 'id', label: 'ID', width: 60, tableOnly: true },
  { prop: 'courseName', label: '课程名称' },
  { prop: 'teacherId', label: '教师ID', type: 'number' },
  { prop: 'credit', label: '学分', type: 'number', precision: 1 },
  { prop: 'beginDate', label: '开始日期', type: 'date' },
  { prop: 'endDate', label: '结束日期', type: 'date' },
  { prop: 'schedule', label: '上课时间' },
  { prop: 'description', label: '描述', type: 'textarea' }
]

const rules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  teacherId: [{ required: true, message: '请输入教师ID', trigger: 'blur' }]
}

const defaultForm = () => ({ courseName: '', teacherId: undefined, credit: 2, beginDate: '', endDate: '', schedule: '', description: '' })
</script>
