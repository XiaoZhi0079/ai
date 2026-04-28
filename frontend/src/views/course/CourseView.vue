<template>
  <CrudTable
    :columns="columns"
    :api="api"
    :default-form="defaultForm"
    :rules="rules"
    :search-config="searchConfig"
    :readonly="permissions.isReadOnly"
    :allow-create="permissions.canCreate"
    :allow-edit="permissions.canEdit"
    :allow-delete="permissions.canDelete"
  />
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import CrudTable from '@/components/CrudTable.vue'
import type { Column, SearchConfig } from '@/components/CrudTable.vue'
import { createCrudApi } from '@/api/crud'
import { useUserStore } from '@/stores/user'
import type { Course, Role, Teacher } from '@/types'
import { getCrudPermissions } from '@/access/moduleRules'

type CourseRow = Course & { teacherName?: string }

const userStore = useUserStore()
const courseApi = createCrudApi<Course>('/api/courses')
const teacherApi = createCrudApi<Teacher>('/api/teachers')
const teacherOptions = ref<Array<{ label: string; value: number }>>([])
const permissions = computed(() => getCrudPermissions('courses', (userStore.role || 'STUDENT') as Role))

const api = {
  ...courseApi,
  async list(): Promise<CourseRow[]> {
    const [courses, teachers] = await Promise.all([courseApi.list(), teacherApi.list()])
    const teacherMap = new Map(teachers.map((teacher) => [teacher.id, teacher.name]))
    return courses.map((course) => ({
      ...course,
      teacherName: teacherMap.get(course.teacherId) || '未匹配教师'
    }))
  }
}

const columns = computed<Column[]>(() => [
  { prop: 'id', label: 'ID', width: 60, tableOnly: true },
  { prop: 'courseName', label: '课程名称' },
  { prop: 'teacherName', label: '任课教师', tableOnly: true },
  { prop: 'teacherId', label: '任课教师', type: 'select', options: teacherOptions.value, formOnly: true },
  { prop: 'credit', label: '学分', type: 'number', precision: 1 },
  { prop: 'schedule', label: '上课时间' },
  { prop: 'beginDate', label: '开始日期', type: 'date' },
  { prop: 'endDate', label: '结束日期', type: 'date' },
  { prop: 'description', label: '课程简介', type: 'textarea', formOnly: true }
])

const searchConfig: SearchConfig = {
  fields: ['courseName', 'teacherName', 'schedule'],
  placeholder: '按课程名、教师名、上课时间搜索'
}

const rules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  teacherId: [{ required: true, message: '请选择任课教师', trigger: 'change' }]
}

const defaultForm = () => ({
  courseName: '',
  teacherId: undefined,
  credit: 2,
  beginDate: '',
  endDate: '',
  schedule: '',
  description: ''
})

async function loadTeachers() {
  const teachers = await teacherApi.list()
  teacherOptions.value = teachers
    .filter((teacher) => teacher.id != null)
    .map((teacher) => ({ label: teacher.name, value: teacher.id as number }))
}

onMounted(loadTeachers)
</script>
