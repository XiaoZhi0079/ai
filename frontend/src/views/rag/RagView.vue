<template>
  <div class="rag-container">
    <h3>知识库文档上传</h3>
    <p class="desc">上传文档到知识库，AI 对话时可基于文档内容进行回答。</p>
    <el-upload
      ref="uploadRef"
      drag
      :auto-upload="false"
      :on-change="handleChange"
      :limit="1"
      accept=".txt,.pdf,.doc,.docx,.md"
    >
      <el-icon class="el-icon--upload" size="40"><UploadFilled /></el-icon>
      <div class="el-upload__text">拖拽文件到此处，或 <em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip">支持 txt、pdf、doc、docx、md 格式</div>
      </template>
    </el-upload>
    <el-button type="primary" style="margin-top: 16px" :loading="uploading" :disabled="!selectedFile" @click="handleUpload">
      上传到知识库
    </el-button>

    <h3 style="margin-top: 32px">已上传文档</h3>
    <el-input v-model="searchKey" placeholder="搜索文件名" clearable style="margin-bottom: 12px; width: 300px" />
    <el-table :data="filteredDocuments" v-loading="loadingDocs" style="width: 100%">
      <el-table-column prop="fileName" label="文件名">
        <template #default="{ row }">
          <el-link type="primary" :href="row.ossUrl" target="_blank">{{ row.fileName }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="上传时间" width="180" />
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-popconfirm title="确定删除此文档？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button type="danger" link size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, type UploadFile, type UploadInstance } from 'element-plus'
import { uploadRagDocument, getRagDocuments, deleteRagDocument } from '@/api/rag'

const uploadRef = ref<UploadInstance>()
const selectedFile = ref<File | null>(null)
const uploading = ref(false)
const documents = ref<any[]>([])
const loadingDocs = ref(false)
const searchKey = ref('')

const filteredDocuments = computed(() => {
  if (!searchKey.value) return documents.value
  const keyword = searchKey.value.toLowerCase()
  return documents.value.filter((doc: any) => doc.fileName?.toLowerCase().includes(keyword))
})

function handleChange(file: UploadFile) {
  selectedFile.value = file.raw || null
}

async function fetchDocuments() {
  loadingDocs.value = true
  try {
    documents.value = await getRagDocuments()
  } catch {
    // 错误已由全局拦截器处理
  } finally {
    loadingDocs.value = false
  }
}

async function handleUpload() {
  if (!selectedFile.value) return
  uploading.value = true
  try {
    await uploadRagDocument(selectedFile.value)
    ElMessage.success('文档上传成功')
    selectedFile.value = null
    uploadRef.value?.clearFiles()
    await fetchDocuments()
  } catch {
    // 错误已由全局拦截器处理
  } finally {
    uploading.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await deleteRagDocument(id)
    ElMessage.success('删除成功')
    await fetchDocuments()
  } catch {
    // 错误已由全局拦截器处理
  }
}

onMounted(fetchDocuments)
</script>

<style scoped>
.rag-container { padding: 20px; max-width: 800px; }
.desc { color: #909399; margin-bottom: 20px; font-size: 14px; }
</style>
