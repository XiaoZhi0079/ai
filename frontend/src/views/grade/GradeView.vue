<template>
  <CrudTable :columns="columns" :api="api" :default-form="defaultForm" :rules="rules" />
</template>

<script setup lang="ts">
import CrudTable from '@/components/CrudTable.vue'
import type { Column } from '@/components/CrudTable.vue'
import { createCrudApi } from '@/api/crud'
import type { Grade } from '@/types'

const api = createCrudApi<Grade>('/api/grades')

const columns: Column[] = [
  { prop: 'id', label: 'ID', width: 60, tableOnly: true },
  { prop: 'studentId', label: '学生ID', type: 'number' },
  { prop: 'courseId', label: '课程ID', type: 'number' },
  { prop: 'score', label: '成绩', type: 'number', precision: 1 },
  { prop: 'semester', label: '学期', type: 'number' }
]

const rules = {
  studentId: [{ required: true, message: '请输入学生ID', trigger: 'blur' }],
  courseId: [{ required: true, message: '请输入课程ID', trigger: 'blur' }],
  score: [{ required: true, message: '请输入成绩', trigger: 'blur' }]
}

const defaultForm = () => ({ studentId: undefined, courseId: undefined, score: 0, semester: 1 })
</script>
