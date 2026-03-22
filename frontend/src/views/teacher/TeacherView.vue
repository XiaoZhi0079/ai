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
import CrudTable from '@/components/CrudTable.vue'
import type { Column, FilterConfig, SearchConfig } from '@/components/CrudTable.vue'
import { createCrudApi } from '@/api/crud'
import { useUserStore } from '@/stores/user'
import type { Teacher } from '@/types'

const userStore = useUserStore()
const api = createCrudApi<Teacher>('/api/teachers')

const genderOptions = [
  { label: '男', value: '男' },
  { label: '女', value: '女' }
]

const columns: Column[] = [
  { prop: 'id', label: 'ID', width: 60, tableOnly: true },
  { prop: 'userId', label: '用户ID', type: 'number', formOnly: true },
  { prop: 'name', label: '姓名' },
  { prop: 'gender', label: '性别', type: 'select', options: genderOptions },
  { prop: 'department', label: '院系' },
  { prop: 'title', label: '职称' },
  { prop: 'researchField', label: '研究方向' },
  { prop: 'officeAddress', label: '办公地点', formOnly: true }
]

const searchConfig: SearchConfig = {
  fields: ['name', 'department', 'title', 'researchField'],
  placeholder: '按姓名、院系、职称搜索'
}

const filterConfigs: FilterConfig[] = [
  { prop: 'gender', label: '性别', options: genderOptions }
]

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  department: [{ required: true, message: '请输入院系', trigger: 'blur' }]
}

const defaultForm = () => ({
  userId: undefined,
  name: '',
  gender: '',
  department: '',
  title: '',
  researchField: '',
  officeAddress: ''
})
</script>
