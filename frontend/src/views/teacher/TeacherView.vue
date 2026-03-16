<template>
  <CrudTable :columns="columns" :api="api" :default-form="defaultForm" :rules="rules"
    :search-config="searchConfig" :filter-configs="filterConfigs" />
</template>

<script setup lang="ts">
import CrudTable from '@/components/CrudTable.vue'
import type { Column, SearchConfig, FilterConfig } from '@/components/CrudTable.vue'
import { createCrudApi } from '@/api/crud'
import type { Teacher } from '@/types'

const api = createCrudApi<Teacher>('/api/teachers')

const columns: Column[] = [
  { prop: 'id', label: 'ID', width: 60, tableOnly: true },
  { prop: 'userId', label: '用户ID', type: 'number' },
  { prop: 'name', label: '姓名' },
  { prop: 'gender', label: '性别', type: 'select', options: [
    { label: '男', value: '男' },
    { label: '女', value: '女' }
  ]},
  { prop: 'department', label: '院系' },
  { prop: 'title', label: '职称' },
  { prop: 'researchField', label: '研究方向' },
  { prop: 'officeAddress', label: '办公地址' }
]

const searchConfig: SearchConfig = { fields: ['name'], placeholder: '搜索姓名' }

const filterConfigs: FilterConfig[] = [
  { prop: 'gender', label: '性别', options: [{ label: '男', value: '男' }, { label: '女', value: '女' }] }
]

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  department: [{ required: true, message: '请输入院系', trigger: 'blur' }]
}

const defaultForm = () => ({ userId: undefined, name: '', gender: '', department: '', title: '', researchField: '', officeAddress: '' })
</script>
