<template>
  <div class="crud-table">
    <div class="toolbar">
      <el-radio-group
        v-if="categoryConfig"
        v-model="categoryValue"
        style="margin-right: 12px"
      >
        <el-radio-button
          v-for="option in categoryConfig.options"
          :key="String(option.value)"
          :label="option.value"
        >
          {{ option.label }}
        </el-radio-button>
      </el-radio-group>

      <el-input
        v-if="searchConfig"
        v-model="searchKey"
        :placeholder="searchConfig.placeholder || '搜索'"
        clearable
        style="width: 220px; margin-right: 12px"
      />

      <el-select
        v-for="filter in filterConfigs"
        :key="filter.prop"
        v-model="filterValues[filter.prop]"
        :placeholder="filter.label"
        clearable
        style="width: 140px; margin-right: 12px"
      >
        <el-option
          v-for="option in filter.options"
          :key="String(option.value)"
          :label="option.label"
          :value="option.value"
        />
      </el-select>

      <el-button v-if="!readonly" type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>
        新增
      </el-button>
    </div>

    <el-table :data="filteredData" border stripe v-loading="loading" style="width: 100%">
      <el-table-column
        v-for="column in tableColumns"
        :key="column.prop"
        :prop="column.prop"
        :label="column.label"
        :width="column.width"
      >
        <template #default="{ row }">
          {{ formatCell(row, column) }}
        </template>
      </el-table-column>

      <el-table-column v-if="!readonly" label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" text size="small" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除吗？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button type="danger" text size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑' : '新增'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="resolvedRules" label-width="100px">
        <el-form-item
          v-for="column in formColumns"
          :key="column.prop"
          :label="column.label"
          :prop="column.prop"
        >
          <el-select
            v-if="column.type === 'select'"
            v-model="(formData as any)[column.prop]"
            style="width: 100%"
          >
            <el-option
              v-for="option in column.options"
              :key="String(option.value)"
              :label="option.label"
              :value="option.value"
            />
          </el-select>

          <el-date-picker
            v-else-if="column.type === 'date'"
            v-model="(formData as any)[column.prop]"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />

          <el-input-number
            v-else-if="column.type === 'number'"
            v-model="(formData as any)[column.prop]"
            :precision="column.precision"
            style="width: 100%"
          />

          <el-input
            v-else
            v-model="(formData as any)[column.prop]"
            :type="column.type === 'textarea' ? 'textarea' : column.type === 'password' ? 'password' : 'text'"
            :show-password="column.type === 'password'"
          />
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
import { computed, onMounted, reactive, ref } from 'vue'
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
  formatter?: (row: any, value: any) => any
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

export interface CategoryConfig {
  prop: string
  options: { label: string; value: any }[]
  defaultValue?: any
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
  categoryConfig?: CategoryConfig
  readonly?: boolean
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
const categoryValue = ref(props.categoryConfig?.defaultValue ?? '')

const tableColumns = computed(() => props.columns.filter((column) => !column.formOnly))
const formColumns = computed(() => props.columns.filter((column) => !column.tableOnly))
const resolvedRules = computed(() => props.rules || {})

const filteredData = computed(() => {
  let data = tableData.value

  if (props.categoryConfig && categoryValue.value !== '' && categoryValue.value != null) {
    data = data.filter((row: any) => row[props.categoryConfig!.prop] === categoryValue.value)
  }

  if (searchKey.value && props.searchConfig) {
    const keyword = searchKey.value.toLowerCase()
    data = data.filter((row: any) =>
      props.searchConfig!.fields.some((field) => {
        const value = row[field]
        return value != null && String(value).toLowerCase().includes(keyword)
      })
    )
  }

  if (props.filterConfigs) {
    for (const filter of props.filterConfigs) {
      const selected = filterValues[filter.prop]
      if (selected != null && selected !== '') {
        data = data.filter((row: any) => row[filter.prop] === selected)
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
    formData.value = buildFormModel(row)
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
  const payload = buildSubmitPayload()
  try {
    if (isEdit.value && editId.value !== null) {
      await props.api.update(editId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await props.api.create(payload)
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

function buildFormModel(row: any) {
  const model = props.defaultForm()
  formColumns.value.forEach((column) => {
    if (row[column.prop] !== undefined) {
      model[column.prop] = row[column.prop]
    }
  })
  return model
}

function buildSubmitPayload() {
  const payload: Record<string, any> = {}
  formColumns.value.forEach((column) => {
    payload[column.prop] = formData.value[column.prop]
  })
  return payload
}

function formatCell(row: any, column: Column) {
  const value = row[column.prop]
  if (column.formatter) {
    return column.formatter(row, value)
  }
  if (column.type === 'select' && column.options?.length) {
    const matched = column.options.find((option) => option.value === value)
    return matched?.label ?? value ?? ''
  }
  return value ?? ''
}

onMounted(fetchData)
</script>

<style scoped>
.crud-table {
  padding: 20px;
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 0;
}
</style>
