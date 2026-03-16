<template>
  <div class="crud-table">
    <div class="toolbar">
      <el-input v-if="searchConfig" v-model="searchKey" :placeholder="searchConfig.placeholder || '搜索'" clearable style="width: 200px; margin-right: 12px" />
      <el-select v-for="fc in filterConfigs" :key="fc.prop" v-model="filterValues[fc.prop]" :placeholder="fc.label" clearable style="width: 120px; margin-right: 12px">
        <el-option v-for="opt in fc.options" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon> 新增
      </el-button>
    </div>
    <el-table :data="filteredData" border stripe v-loading="loading" style="width: 100%">
      <el-table-column v-for="col in tableColumns" :key="col.prop" :prop="col.prop" :label="col.label" :width="col.width" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" text size="small" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button type="danger" text size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑' : '新增'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item v-for="col in formColumns" :key="col.prop" :label="col.label" :prop="col.prop">
          <el-select v-if="col.type === 'select'" v-model="(formData as any)[col.prop]" style="width:100%">
            <el-option v-for="opt in col.options" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-date-picker v-else-if="col.type === 'date'" v-model="(formData as any)[col.prop]" type="date" value-format="YYYY-MM-DD" style="width:100%" />
          <el-input-number v-else-if="col.type === 'number'" v-model="(formData as any)[col.prop]" :precision="col.precision" style="width:100%" />
          <el-input v-else v-model="(formData as any)[col.prop]" :type="col.type === 'textarea' ? 'textarea' : 'text'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'

export interface Column {
  prop: string
  label: string
  width?: number
  type?: 'text' | 'number' | 'select' | 'date' | 'textarea' | 'password'
  options?: { label: string; value: any }[]
  precision?: number
  formOnly?: boolean
  tableOnly?: boolean
}

export interface SearchConfig {
  fields: string[]
  placeholder?: string
}

export interface FilterConfig {
  prop: string
  label: string
  options: { label: string; value: any }[]
}

export interface CrudApi {
  list: () => Promise<any[]>
  create: (data: any) => Promise<any>
  update: (id: number, data: any) => Promise<any>
  remove: (id: number) => Promise<void>
}

const props = defineProps<{
  columns: Column[]
  api: CrudApi
  defaultForm: () => Record<string, any>
  rules?: Record<string, any>
  searchConfig?: SearchConfig
  filterConfigs?: FilterConfig[]
}>()

const tableData = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formData = ref<Record<string, any>>({})
const formRef = ref<FormInstance>()
const submitting = ref(false)

const searchKey = ref('')
const filterValues = reactive<Record<string, any>>({})

const tableColumns = props.columns.filter((c) => !c.formOnly)
const formColumns = props.columns.filter((c) => !c.tableOnly)
const rules = props.rules || {}

const filteredData = computed(() => {
  let data = tableData.value
  if (searchKey.value && props.searchConfig) {
    const keyword = searchKey.value.toLowerCase()
    data = data.filter((row: any) =>
      props.searchConfig!.fields.some((field) => {
        const val = row[field]
        return val != null && String(val).toLowerCase().includes(keyword)
      })
    )
  }
  if (props.filterConfigs) {
    for (const fc of props.filterConfigs) {
      const selected = filterValues[fc.prop]
      if (selected != null && selected !== '') {
        data = data.filter((row: any) => row[fc.prop] === selected)
      }
    }
  }
  return data
})

async function fetchData() {
  loading.value = true
  try {
    tableData.value = await props.api.list()
  } finally {
    loading.value = false
  }
}

function openDialog(row?: any) {
  if (row) {
    isEdit.value = true
    editId.value = row.id
    formData.value = { ...row }
  } else {
    isEdit.value = false
    editId.value = null
    formData.value = props.defaultForm()
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value && editId.value !== null) {
      await props.api.update(editId.value, formData.value)
      ElMessage.success('更新成功')
    } else {
      await props.api.create(formData.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  await props.api.remove(id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.crud-table { padding: 20px; }
.toolbar { margin-bottom: 16px; display: flex; align-items: center; }
</style>
