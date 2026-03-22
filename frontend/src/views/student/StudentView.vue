<template>
  <CrudTable
    :columns="columns"
    :api="api"
    :default-form="defaultForm"
    :rules="rules"
    :search-config="searchConfig"
    :filter-configs="filterConfigs"
    :readonly="userStore.role === 'STUDENT'"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import CrudTable from '@/components/CrudTable.vue'
import type { Column, FilterConfig, SearchConfig } from '@/components/CrudTable.vue'
import { createCrudApi } from '@/api/crud'
import { useUserStore } from '@/stores/user'
import type { Student } from '@/types'

const userStore = useUserStore()
const api = createCrudApi<Student>('/api/students')

const genderOptions = [
  { label: '男', value: '男' },
  { label: '女', value: '女' }
]

const gradeOptions = [2021, 2022, 2023, 2024, 2025, 2026].map((grade) => ({
  label: String(grade),
  value: grade
}))

const columns = computed<Column[]>(() => {
  const isStudent = userStore.role === 'STUDENT'
  return [
    { prop: 'id', label: 'ID', width: 60, tableOnly: true },
    { prop: 'userId', label: '用户ID', type: 'number', formOnly: true },
    { prop: 'name', label: '姓名' },
    { prop: 'gender', label: '性别', type: 'select', options: genderOptions },
    { prop: 'grade', label: '年级', type: 'select', options: gradeOptions },
    { prop: 'major', label: '专业' },
    { prop: 'className', label: '班级' },
    { prop: 'dormitory', label: '宿舍', formOnly: isStudent },
    { prop: 'guardianPhone', label: '电话', formOnly: isStudent }
  ]
})

const searchConfig: SearchConfig = {
  fields: ['name', 'major', 'className'],
  placeholder: '按姓名、专业、班级搜索'
}

const filterConfigs: FilterConfig[] = [
  { prop: 'gender', label: '性别', options: genderOptions },
  { prop: 'grade', label: '年级', options: gradeOptions }
]

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }]
}

const defaultForm = () => ({
  userId: undefined,
  name: '',
  gender: '',
  grade: 2024,
  major: '',
  className: '',
  dormitory: '',
  guardianPhone: ''
})
</script>
