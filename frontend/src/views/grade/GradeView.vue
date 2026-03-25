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
import { computed, onMounted, ref } from 'vue'
import CrudTable from '@/components/CrudTable.vue'
import type { Column, FilterConfig, SearchConfig } from '@/components/CrudTable.vue'
import { createCrudApi } from '@/api/crud'
import { useUserStore } from '@/stores/user'
import type { Course, Grade, Student } from '@/types'

type GradeRow = Grade & { studentName?: string; courseName?: string }

const userStore = useUserStore()
const gradeApi = createCrudApi<Grade>('/api/grades')
const studentApi = createCrudApi<Student>('/api/students')
const courseApi = createCrudApi<Course>('/api/courses')

const studentOptions = ref<Array<{ label: string; value: number }>>([])
const courseOptions = ref<Array<{ label: string; value: number }>>([])
const semesterOptions = ref<Array<{ label: string; value: number }>>(buildSemesterOptions())

function buildSemesterOptions(extraYears: number[] = []) {
  const currentYear = new Date().getFullYear()
  const defaultYears = Array.from({ length: 6 }, (_, index) => currentYear + 1 - index)
  return Array.from(new Set([...extraYears, ...defaultYears]))
    .sort((left, right) => right - left)
    .map((year) => ({
      label: String(year),
      value: year
    }))
}

const api = {
  ...gradeApi,
  async list(): Promise<GradeRow[]> {
    const [grades, students, courses] = await Promise.all([
      gradeApi.list(),
      studentApi.list(),
      courseApi.list()
    ])
    semesterOptions.value = buildSemesterOptions(
      grades
        .map((grade) => grade.semester)
        .filter((semester): semester is number => semester != null)
    )
    const studentMap = new Map(students.map((student) => [student.id, student.name]))
    const courseMap = new Map(courses.map((course) => [course.id, course.courseName]))
    return grades.map((grade) => ({
      ...grade,
      studentName: studentMap.get(grade.studentId) || '未匹配学生',
      courseName: courseMap.get(grade.courseId) || '未匹配课程'
    }))
  }
}

const columns = computed<Column[]>(() => [
  { prop: 'id', label: 'ID', width: 60, tableOnly: true },
  { prop: 'studentName', label: '学生姓名', tableOnly: true },
  { prop: 'studentId', label: '学生', type: 'select', options: studentOptions.value, formOnly: true },
  { prop: 'courseName', label: '课程名称', tableOnly: true },
  { prop: 'courseId', label: '课程', type: 'select', options: courseOptions.value, formOnly: true },
  { prop: 'score', label: '成绩', type: 'number', precision: 1 },
  { prop: 'semester', label: '学年', type: 'select', options: semesterOptions.value }
])

const searchConfig: SearchConfig = {
  fields: ['studentName', 'courseName'],
  placeholder: '按学生姓名或课程名称搜索'
}

const filterConfigs = computed<FilterConfig[]>(() => [
  { prop: 'semester', label: '学年', options: semesterOptions.value }
])

const rules = {
  studentId: [{ required: true, message: '请选择学生', trigger: 'change' }],
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  score: [{ required: true, message: '请输入成绩', trigger: 'blur' }]
}

const defaultForm = () => ({
  studentId: undefined,
  courseId: undefined,
  score: 0,
  semester: semesterOptions.value[0]?.value ?? new Date().getFullYear()
})

async function loadOptions() {
  const [students, courses] = await Promise.all([studentApi.list(), courseApi.list()])
  studentOptions.value = students
    .filter((student) => student.id != null)
    .map((student) => ({ label: student.name, value: student.id as number }))
  courseOptions.value = courses
    .filter((course) => course.id != null)
    .map((course) => ({ label: course.courseName, value: course.id as number }))
}

onMounted(loadOptions)
</script>
