<template>
  <CrudTable
    :columns="columns"
    :api="api"
    :default-form="defaultForm"
    :rules="rules"
    :search-config="searchConfig"
    :filter-configs="filterConfigs"
    :readonly="permissions.isReadOnly"
    :allow-create="permissions.canCreate"
    :allow-edit="permissions.canEdit"
    :allow-delete="permissions.canDelete"
  />
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import CrudTable from '@/components/CrudTable.vue'
import type { Column, FilterConfig, SearchConfig } from '@/components/CrudTable.vue'
import { createCrudApi } from '@/api/crud'
import { getUserOptions } from '@/api/user'
import { useUserStore } from '@/stores/user'
import type { Role, Student } from '@/types'
import { getCrudPermissions } from '@/access/moduleRules'

const userStore = useUserStore()
const api = createCrudApi<Student>('/api/students')
const studentUserOptions = ref<Array<{ label: string; value: number }>>([])
const permissions = computed(() => getCrudPermissions('students', (userStore.role || 'STUDENT') as Role))

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
    { prop: 'userId', label: '用户id', type: 'select', options: studentUserOptions.value, formOnly: true },
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
  userId: [{ required: true, message: '请选择学生用户', trigger: 'change' }],
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

async function loadStudentUsers() {
  if (userStore.role === 'STUDENT') return
  const users = await getUserOptions('STUDENT')
  studentUserOptions.value = users.map((user) => ({
    label: `${user.username} (#${user.id})`,
    value: user.id
  }))
}

onMounted(loadStudentUsers)
</script>
