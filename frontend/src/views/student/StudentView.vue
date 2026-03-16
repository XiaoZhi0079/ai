<template>
  <CrudTable :columns="columns" :api="api" :default-form="defaultForm" :rules="rules"
    :search-config="searchConfig" :filter-configs="filterConfigs" />
</template>

<script setup lang="ts">
import CrudTable from '@/components/CrudTable.vue'
import type { Column, SearchConfig, FilterConfig } from '@/components/CrudTable.vue'
import { createCrudApi } from '@/api/crud'
import type { Student } from '@/types'

const api = createCrudApi<Student>('/api/students')

const columns: Column[] = [
  { prop: 'id', label: 'ID', width: 60, tableOnly: true },
  { prop: 'userId', label: '用户ID', type: 'number' },
  { prop: 'name', label: '姓名' },
  { prop: 'gender', label: '性别', type: 'select', options: [
    { label: '男', value: '男' },
    { label: '女', value: '女' }
  ]},
  { prop: 'grade', label: '年级', type: 'number' },
  { prop: 'major', label: '专业' },
  { prop: 'className', label: '班级' },
  { prop: 'dormitory', label: '宿舍' },
  { prop: 'guardianPhone', label: '监护人电话' }
]

const searchConfig: SearchConfig = { fields: ['name'], placeholder: '搜索姓名' }

const filterConfigs: FilterConfig[] = [
  { prop: 'gender', label: '性别', options: [{ label: '男', value: '男' }, { label: '女', value: '女' }] },
  { prop: 'grade', label: '年级', options: [
    { label: '2021', value: 2021 }, { label: '2022', value: 2022 },
    { label: '2023', value: 2023 }, { label: '2024', value: 2024 },
    { label: '2025', value: 2025 }, { label: '2026', value: 2026 }
  ]}
]

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }]
}

const defaultForm = () => ({ userId: undefined, name: '', gender: '', grade: 2024, major: '', className: '', dormitory: '', guardianPhone: '' })
</script>
