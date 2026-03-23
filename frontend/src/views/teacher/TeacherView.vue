<template>
  <CrudTable
    :columns="columns"
    :api="api"
    :default-form="defaultForm"
    :rules="rules"
    :search-config="searchConfig"
    :filter-configs="filterConfigs"
    :readonly="userStore.role !== 'ADMIN'"
  />
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import CrudTable from '@/components/CrudTable.vue'
import type { Column, FilterConfig, SearchConfig } from '@/components/CrudTable.vue'
import { createCrudApi } from '@/api/crud'
import { getUserOptions } from '@/api/user'
import { useUserStore } from '@/stores/user'
import type { Teacher } from '@/types'

const userStore = useUserStore()
const api = createCrudApi<Teacher>('/api/teachers')
const teacherUserOptions = ref<Array<{ label: string; value: number }>>([])

const genderOptions = [
  { label: '男', value: '男' },
  { label: '女', value: '女' }
]

const columns = computed<Column[]>(() => [
  { prop: 'id', label: 'ID', width: 60, tableOnly: true },
  { prop: 'userId', label: '关联教师用户', type: 'select', options: teacherUserOptions.value, formOnly: true },
  { prop: 'name', label: '姓名' },
  { prop: 'gender', label: '性别', type: 'select', options: genderOptions },
  { prop: 'department', label: '院系' },
  { prop: 'title', label: '职称' },
  { prop: 'researchField', label: '研究方向' },
  { prop: 'officeAddress', label: '办公地点', formOnly: true }
])

const searchConfig: SearchConfig = {
  fields: ['name', 'department', 'title', 'researchField'],
  placeholder: '按姓名、院系、职称搜索'
}

const filterConfigs: FilterConfig[] = [
  { prop: 'gender', label: '性别', options: genderOptions }
]

const rules = {
  userId: [{ required: true, message: '请选择教师用户', trigger: 'change' }],
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

async function loadTeacherUsers() {
  if (userStore.role !== 'ADMIN') return
  const users = await getUserOptions('TEACHER')
  teacherUserOptions.value = users.map((user) => ({
    label: `${user.username} (#${user.id})`,
    value: user.id
  }))
}

onMounted(loadTeacherUsers)
</script>
