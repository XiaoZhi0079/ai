<template>
  <div class="rag-container">
    <h3>{{ texts.title }}</h3>
    <p class="desc">{{ texts.description }}</p>

    <el-upload
      ref="uploadRef"
      drag
      :auto-upload="false"
      :on-change="handleChange"
      :limit="1"
      accept=".txt,.pdf,.doc,.docx,.md,.png,.jpg,.jpeg,.webp"
    >
      <el-icon class="el-icon--upload" size="40"><UploadFilled /></el-icon>
      <div class="el-upload__text">{{ texts.dropPrefix }} <em>{{ texts.dropAction }}</em></div>
      <template #tip>
        <div class="el-upload__tip">{{ texts.supportedFormats }}</div>
      </template>
    </el-upload>

    <el-radio-group v-if="userStore.role === 'ADMIN'" v-model="selectedScope" style="margin-top: 16px">
      <el-radio-button value="PUBLIC">{{ texts.publicScope }}</el-radio-button>
      <el-radio-button value="PRIVATE">{{ texts.privateScope }}</el-radio-button>
    </el-radio-group>

    <p class="desc" style="margin-top: 8px">
      {{ userStore.role === 'ADMIN' ? texts.adminHint : texts.userHint }}
    </p>

    <div class="actions-row">
      <el-button @click="ocrDialogVisible = true">{{ texts.ocrSettings }}</el-button>
      <el-button type="primary" :loading="parsing" :disabled="!selectedFile" @click="handleParse">
        {{ texts.parseButton }}
      </el-button>
    </div>

    <h3 style="margin-top: 32px">{{ texts.visibleTitle }}</h3>
    <el-input v-model="searchKey" :placeholder="texts.searchPlaceholder" clearable style="margin-bottom: 12px; width: 300px" />

    <el-table :data="filteredDocuments" v-loading="loadingDocs" style="width: 100%">
      <el-table-column prop="fileName" :label="texts.fileName">
        <template #default="{ row }">
          <el-link type="primary" :href="row.ossUrl" target="_blank">{{ row.fileName }}</el-link>
        </template>
      </el-table-column>

      <el-table-column prop="knowledgeScope" :label="texts.scopeLabel" width="120">
        <template #default="{ row }">
          <el-tag :type="row.knowledgeScope === 'PUBLIC' ? 'success' : 'info'">
            {{ row.knowledgeScope === 'PUBLIC' ? texts.publicTag : texts.privateTag }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="createdAt" :label="texts.createdAt" width="180" />

      <el-table-column :label="texts.actionLabel" width="80">
        <template #default="{ row }">
          <el-popconfirm v-if="canDeleteRow(row)" :title="texts.deleteConfirm" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button type="danger" link size="small">{{ texts.deleteButton }}</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="ocrDialogVisible" :title="texts.ocrDialogTitle" width="560px">
      <el-form label-width="120px">
        <el-form-item :label="texts.useCustomOcr">
          <el-switch v-model="ocrSettings.useCustom" />
        </el-form-item>
        <el-form-item :label="texts.ocrBaseUrl">
          <el-input v-model="ocrSettings.baseUrl" :placeholder="texts.ocrBaseUrlPlaceholder" :disabled="!ocrSettings.useCustom" />
        </el-form-item>
        <el-form-item :label="texts.ocrApiKey">
          <el-input v-model="ocrSettings.apiKey" :placeholder="texts.ocrApiKeyPlaceholder" show-password :disabled="!ocrSettings.useCustom" />
        </el-form-item>
        <el-form-item :label="texts.ocrModel">
          <el-input v-model="ocrSettings.model" :placeholder="texts.ocrModelPlaceholder" :disabled="!ocrSettings.useCustom" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetOcrSettings">{{ texts.resetButton }}</el-button>
        <el-button @click="ocrDialogVisible = false">{{ texts.cancelButton }}</el-button>
        <el-button type="primary" @click="saveOcrSettings">{{ texts.saveButton }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewDialogVisible" :title="texts.previewDialogTitle" width="900px">
      <div v-if="previewMeta" class="preview-meta">
        <el-tag>{{ previewMeta.fileName }}</el-tag>
        <el-tag type="info">{{ texts.charCount }}: {{ previewMeta.charCount }}</el-tag>
        <el-tag :type="previewMeta.ocrUsed ? 'warning' : 'success'">
          {{ previewMeta.ocrUsed ? texts.ocrUsedTag : texts.directTextTag }}
        </el-tag>
        <el-tag :type="previewMeta.knowledgeScope === 'PUBLIC' ? 'success' : 'info'">
          {{ previewMeta.knowledgeScope === 'PUBLIC' ? texts.publicTag : texts.privateTag }}
        </el-tag>
      </div>
      <el-input v-model="previewText" type="textarea" :rows="22" />
      <template #footer>
        <el-button @click="previewDialogVisible = false">{{ texts.cancelButton }}</el-button>
        <el-button type="primary" :loading="confirming" @click="handleConfirmUpload">{{ texts.confirmUpload }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, type UploadFile, type UploadInstance } from 'element-plus'
import {
  confirmRagDocument,
  deleteRagDocument,
  getRagDocuments,
  getRagOcrSettings,
  parseRagDocument,
  saveRagOcrSettings,
  type RagOcrConfig,
  type RagParsePreview
} from '@/api/rag'
import { useUserStore } from '@/stores/user'

const texts = {
  title: '\u77e5\u8bc6\u5e93\u6587\u6863\u4e0a\u4f20',
  description: '\u5bf9 PDF \u6216\u56fe\u7247\u7c7b\u6587\u6863\uff0c\u7cfb\u7edf\u4f1a\u5148\u505a OCR \u63d0\u53d6\u9884\u89c8\uff0c\u4f60\u53ef\u4ee5\u7f16\u8f91\u786e\u8ba4\u540e\u518d\u5165\u5e93\u3002',
  dropPrefix: '\u62d6\u62fd\u6587\u4ef6\u5230\u6b64\u5904\uff0c\u6216',
  dropAction: '\u70b9\u51fb\u9009\u62e9',
  supportedFormats: '\u652f\u6301 txt\u3001md\u3001pdf\u3001doc\u3001docx\u3001png\u3001jpg\u3001jpeg\u3001webp',
  publicScope: '\u516c\u6709\u77e5\u8bc6\u5e93',
  privateScope: '\u6211\u7684\u79c1\u6709\u77e5\u8bc6\u5e93',
  adminHint: '\u7ba1\u7406\u5458\u53ef\u4e0a\u4f20\u5230\u516c\u6709\u5e93\u6216\u81ea\u5df1\u7684\u79c1\u6709\u5e93\uff1b\u666e\u901a\u7528\u6237\u4ec5\u4e0a\u4f20\u5230\u81ea\u5df1\u7684\u79c1\u6709\u5e93\u3002',
  userHint: '\u4f60\u4e0a\u4f20\u7684\u6587\u6863\u53ea\u4f1a\u8fdb\u5165\u81ea\u5df1\u7684\u79c1\u6709\u77e5\u8bc6\u5e93\u3002',
  ocrSettings: 'OCR \u8bbe\u7f6e',
  parseButton: '\u89e3\u6790\u5e76\u9884\u89c8',
  visibleTitle: '\u53ef\u89c1\u6587\u6863',
  searchPlaceholder: '\u641c\u7d22\u6587\u4ef6\u540d',
  fileName: '\u6587\u4ef6\u540d',
  scopeLabel: '\u8303\u56f4',
  publicTag: '\u516c\u6709',
  privateTag: '\u79c1\u6709',
  createdAt: '\u4e0a\u4f20\u65f6\u95f4',
  actionLabel: '\u64cd\u4f5c',
  deleteConfirm: '\u786e\u5b9a\u5220\u9664\u6b64\u6587\u6863\uff1f',
  deleteButton: '\u5220\u9664',
  ocrDialogTitle: 'OCR \u6a21\u578b\u8bbe\u7f6e',
  useCustomOcr: '\u4f7f\u7528\u81ea\u5b9a\u4e49 OCR',
  ocrBaseUrl: 'Base URL',
  ocrApiKey: 'API Key',
  ocrModel: '\u6a21\u578b\u540d',
  ocrBaseUrlPlaceholder: '\u4ec5\u5f53\u524d\u7528\u6237\u751f\u6548\uff0c\u4e0d\u586b\u5219\u4f7f\u7528\u540e\u7aef\u9ed8\u8ba4 OCR',
  ocrApiKeyPlaceholder: '\u4ec5\u5f53\u524d\u7528\u6237\u751f\u6548',
  ocrModelPlaceholder: '\u4f8b\u5982 qwen3-vl-plus-2025-12-19',
  resetButton: '\u91cd\u7f6e',
  cancelButton: '\u53d6\u6d88',
  saveButton: '\u4fdd\u5b58',
  previewDialogTitle: '\u6587\u672c\u9884\u89c8\u4e0e\u7f16\u8f91',
  charCount: '\u5b57\u6570',
  ocrUsedTag: 'OCR',
  directTextTag: '\u76f4\u63a5\u62bd\u53d6',
  confirmUpload: '\u786e\u8ba4\u5165\u5e93',
  parseSuccess: '\u6587\u6863\u89e3\u6790\u5b8c\u6210\uff0c\u8bf7\u68c0\u67e5\u6587\u672c\u540e\u786e\u8ba4\u5165\u5e93',
  saveSettingsSuccess: 'OCR \u8bbe\u7f6e\u5df2\u4fdd\u5b58',
  uploadSuccess: '\u6587\u6863\u5165\u5e93\u6210\u529f',
  deleteSuccess: '\u5220\u9664\u6210\u529f',
  previewEmpty: '\u6682\u672a\u89e3\u6790\u51fa\u5185\u5bb9\uff0c\u8bf7\u5148\u68c0\u67e5\u6587\u4ef6\u6216 OCR \u914d\u7f6e'
} as const

type ScopeValue = 'PUBLIC' | 'PRIVATE'

const userStore = useUserStore()

const uploadRef = ref<UploadInstance>()
const selectedFile = ref<File | null>(null)
const selectedScope = ref<ScopeValue>('PRIVATE')
const parsing = ref(false)
const confirming = ref(false)
const documents = ref<any[]>([])
const loadingDocs = ref(false)
const searchKey = ref('')
const ocrDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const previewText = ref('')
const previewMeta = ref<RagParsePreview | null>(null)
const ocrSettings = ref({
  useCustom: false,
  baseUrl: '',
  apiKey: '',
  model: ''
})

const filteredDocuments = computed(() => {
  if (!searchKey.value) return documents.value
  const keyword = searchKey.value.toLowerCase()
  return documents.value.filter((doc: any) => doc.fileName?.toLowerCase().includes(keyword))
})

function currentScope(): ScopeValue {
  return userStore.role === 'ADMIN' ? selectedScope.value : 'PRIVATE'
}

async function loadOcrSettings() {
  try {
    const parsed = await getRagOcrSettings()
    ocrSettings.value = {
      useCustom: !!(parsed?.baseUrl || parsed?.apiKey || parsed?.model),
      baseUrl: parsed.baseUrl || '',
      apiKey: parsed.apiKey || '',
      model: parsed.model || ''
    }
  } catch {
    ocrSettings.value = { useCustom: false, baseUrl: '', apiKey: '', model: '' }
  }
}

async function saveOcrSettings() {
  const payload = ocrSettings.value.useCustom
    ? {
        baseUrl: ocrSettings.value.baseUrl.trim(),
        apiKey: ocrSettings.value.apiKey.trim(),
        model: ocrSettings.value.model.trim()
      }
    : { baseUrl: '', apiKey: '', model: '' }

  await saveRagOcrSettings(payload)
  ocrDialogVisible.value = false
  ElMessage.success(texts.saveSettingsSuccess)
}

function resetOcrSettings() {
  ocrSettings.value = { useCustom: false, baseUrl: '', apiKey: '', model: '' }
}

function buildCustomOcrConfig(): RagOcrConfig | undefined {
  if (!ocrSettings.value.useCustom) return undefined
  if (!ocrSettings.value.baseUrl || !ocrSettings.value.apiKey || !ocrSettings.value.model) return undefined
  return {
    baseUrl: ocrSettings.value.baseUrl.trim(),
    apiKey: ocrSettings.value.apiKey.trim(),
    model: ocrSettings.value.model.trim()
  }
}

function handleChange(file: UploadFile) {
  selectedFile.value = file.raw || null
  previewMeta.value = null
  previewText.value = ''
}

function canDeleteRow(row: any) {
  if (row.knowledgeScope === 'PUBLIC') {
    return userStore.role === 'ADMIN'
  }
  return row.ownerUserId === userStore.id
}

async function fetchDocuments() {
  loadingDocs.value = true
  try {
    documents.value = await getRagDocuments()
  } finally {
    loadingDocs.value = false
  }
}

async function handleParse() {
  if (!selectedFile.value) return
  parsing.value = true
  try {
    const preview = await parseRagDocument(selectedFile.value, currentScope(), buildCustomOcrConfig())
    previewMeta.value = preview
    previewText.value = preview.extractedText || ''
    previewDialogVisible.value = true
    ElMessage.success(preview.extractedText ? texts.parseSuccess : texts.previewEmpty)
  } finally {
    parsing.value = false
  }
}

async function handleConfirmUpload() {
  if (!selectedFile.value) return
  confirming.value = true
  try {
    await confirmRagDocument(selectedFile.value, previewText.value, currentScope())
    ElMessage.success(texts.uploadSuccess)
    previewDialogVisible.value = false
    previewMeta.value = null
    previewText.value = ''
    selectedFile.value = null
    uploadRef.value?.clearFiles()
    await fetchDocuments()
  } finally {
    confirming.value = false
  }
}

async function handleDelete(id: number) {
  await deleteRagDocument(id)
  ElMessage.success(texts.deleteSuccess)
  await fetchDocuments()
}

onMounted(async () => {
  await loadOcrSettings()
  await fetchDocuments()
})
</script>

<style scoped>
.rag-container { padding: 20px; max-width: 1000px; }
.desc { color: #909399; margin-bottom: 20px; font-size: 14px; }
.actions-row { display: flex; gap: 12px; margin-top: 16px; }
.preview-meta { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
</style>
