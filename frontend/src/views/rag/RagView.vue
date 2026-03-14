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
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, type UploadFile, type UploadInstance } from 'element-plus'
import { uploadRagDocument } from '@/api/rag'

const uploadRef = ref<UploadInstance>()
const selectedFile = ref<File | null>(null)
const uploading = ref(false)

function handleChange(file: UploadFile) {
  selectedFile.value = file.raw || null
}

async function handleUpload() {
  if (!selectedFile.value) return
  uploading.value = true
  try {
    await uploadRagDocument(selectedFile.value)
    ElMessage.success('文档上传成功')
    selectedFile.value = null
    uploadRef.value?.clearFiles()
  } catch {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.rag-container { padding: 20px; max-width: 600px; }
.desc { color: #909399; margin-bottom: 20px; font-size: 14px; }
</style>
