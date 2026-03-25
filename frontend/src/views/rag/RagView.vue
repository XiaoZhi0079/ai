<template>
  <div class="rag-container">
    <h3>知识库管理</h3>
    <p class="desc">
      上传文件后，可先预览或编辑解析结果，再将内容写入知识库。
    </p>

    <div class="upload-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">上传文档</div>
          <div class="panel-subtitle">支持文本、Office、PDF 和图片文件</div>
        </div>
        <el-button text @click="ocrDialogVisible = true">OCR 设置</el-button>
      </div>

      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :on-change="handleChange"
        :limit="1"
        accept=".txt,.pdf,.doc,.docx,.md,.png,.jpg,.jpeg,.webp"
      >
        <el-icon class="el-icon--upload" size="40"><UploadFilled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或 <em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">支持：txt / pdf / doc / docx / md / png / jpg / jpeg / webp</div>
        </template>
      </el-upload>

      <el-radio-group v-if="userStore.role === 'ADMIN'" v-model="selectedScope" class="scope-switch">
        <el-radio-button value="PUBLIC">上传到公共知识库</el-radio-button>
        <el-radio-button value="PRIVATE">上传到私有知识库</el-radio-button>
      </el-radio-group>

      <p class="upload-note">
        {{
          userStore.role === 'ADMIN'
            ? '管理员可选择上传到公共知识库或私有知识库。'
            : '普通用户只能上传到自己的私有知识库。'
        }}
      </p>
    </div>

    <div class="process-panel">
      <div class="panel-title">入库操作</div>
      <div class="process-meta">
        <el-tag v-if="selectedFileName">{{ selectedFileName }}</el-tag>
        <el-tag v-if="selectedFileName" :type="isOcrRequired ? 'danger' : 'success'">
          {{ isOcrRequired ? '必须：先 OCR 预览' : '建议：直接入库' }}
        </el-tag>
      </div>
      <p class="process-hint">{{ processHint }}</p>
      <div class="primary-action-row">
        <el-button
          type="primary"
          :loading="isOcrRequired ? parsing : uploadingDirect"
          :disabled="!selectedFile"
          @click="handlePrimaryAction"
        >
          {{ isOcrRequired ? '先 OCR 预览' : '上传并入库' }}
        </el-button>
      </div>
      <div v-if="!isOcrRequired" class="secondary-action-row">
        <el-button link type="primary" :loading="parsing" :disabled="!selectedFile" @click="handleParse">
          需要先识别文本？试试 OCR 预览
        </el-button>
      </div>
    </div>

    <div class="docs-header">
      <h3>知识库文档</h3>
      <div class="docs-filters">
        <el-radio-group v-model="documentScopeFilter">
          <el-radio-button label="ALL">全部</el-radio-button>
          <el-radio-button label="PUBLIC">公共</el-radio-button>
          <el-radio-button label="PRIVATE">私有</el-radio-button>
        </el-radio-group>
        <el-input v-model="searchKey" placeholder="按文件名搜索" clearable style="width: 260px" />
      </div>
    </div>

    <el-table :data="filteredDocuments" v-loading="loadingDocs" style="width: 100%">
      <el-table-column prop="fileName" label="文件名" min-width="220">
        <template #default="{ row }">
          <el-link type="primary" :href="row.ossUrl" target="_blank">{{ row.fileName }}</el-link>
        </template>
      </el-table-column>

      <el-table-column prop="knowledgeScope" label="范围" width="100">
        <template #default="{ row }">
          <el-tag :type="row.knowledgeScope === 'PUBLIC' ? 'success' : 'info'">
            {{ row.knowledgeScope === 'PUBLIC' ? '公共' : '私有' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="uploadedByName" label="上传人" width="120" />
      <el-table-column prop="ownerUserName" label="归属用户" width="120">
        <template #default="{ row }">{{ row.ownerUserName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="chunkCount" label="切片数" width="90" />
      <el-table-column prop="createdAt" label="创建时间" width="180" />

      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openDocumentDetail(row.id)">查看</el-button>
          <el-button type="primary" link size="small" @click="handleRename(row)">重命名</el-button>
          <el-button type="warning" link size="small" :loading="reOcringId === row.id" @click="handleReOcr(row.id)">
            重新 OCR
          </el-button>
          <el-button type="primary" link size="small" @click="openFile(row.ossUrl)">下载</el-button>
          <el-popconfirm v-if="canDeleteRow(row)" title="确定要删除这个文档吗？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button type="danger" link size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="ocrDialogVisible" title="OCR 设置" width="560px">
      <el-form label-width="120px">
        <el-form-item label="使用自定义配置">
          <el-switch v-model="ocrSettings.useCustom" />
        </el-form-item>
        <el-form-item label="Base URL">
          <el-input
            v-model="ocrSettings.baseUrl"
            placeholder="例如：https://dashscope.aliyuncs.com/compatible-mode"
            :disabled="!ocrSettings.useCustom"
          />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input
            v-model="ocrSettings.apiKey"
            show-password
            placeholder="请输入 API Key"
            :disabled="!ocrSettings.useCustom"
          />
        </el-form-item>
        <el-form-item label="模型名称">
          <el-input
            v-model="ocrSettings.model"
            placeholder="请输入 OCR 模型名称"
            :disabled="!ocrSettings.useCustom"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetOcrSettings">重置</el-button>
        <el-button @click="ocrDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveOcrSettings">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewDialogVisible" title="文本预览与编辑" width="900px">
      <div v-if="previewMeta" class="preview-meta">
        <el-tag>{{ previewMeta.fileName }}</el-tag>
        <el-tag type="info">字数：{{ previewMeta.charCount }}</el-tag>
        <el-tag :type="previewMeta.ocrUsed ? 'warning' : 'success'">
          {{ previewMeta.ocrUsed ? 'OCR' : '直接解析' }}
        </el-tag>
        <el-tag :type="previewMeta.knowledgeScope === 'PUBLIC' ? 'success' : 'info'">
          {{ previewMeta.knowledgeScope === 'PUBLIC' ? '公共' : '私有' }}
        </el-tag>
      </div>
      <div v-if="showStructuredSection(previewMeta)" class="preview-section">
        <div class="preview-section-title">{{ showOcrSection(previewMeta) ? '正文文本' : '解析文本' }}</div>
        <el-input v-model="previewStructuredText" type="textarea" :rows="showOcrSection(previewMeta) ? 12 : 22" />
      </div>
      <div v-if="showOcrSection(previewMeta)" class="preview-section">
        <div class="preview-section-title">{{ showStructuredSection(previewMeta) ? '图片 OCR 文本' : 'OCR 文本' }}</div>
        <el-input v-model="previewOcrText" type="textarea" :rows="showStructuredSection(previewMeta) ? 10 : 22" />
      </div>
      <template #footer>
        <el-button @click="previewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="confirming" @click="handleConfirmUpload">确认入库</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="文档详情" width="900px">
      <div v-loading="detailLoading">
        <div v-if="documentDetail" class="detail-meta">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="文件名">{{ documentDetail.fileName }}</el-descriptions-item>
            <el-descriptions-item label="范围">{{ documentDetail.knowledgeScope === 'PUBLIC' ? '公共' : '私有' }}</el-descriptions-item>
            <el-descriptions-item label="上传人">{{ documentDetail.uploadedByName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="归属用户">{{ documentDetail.ownerUserName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="切片数">{{ documentDetail.chunkCount ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ documentDetail.createdAt || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="detail-text-title">入库文本</div>
          <el-input :model-value="documentDetail.extractedText || ''" type="textarea" :rows="20" readonly />
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reOcrPreviewDialogVisible" title="重新 OCR 预览" width="900px">
      <div v-if="reOcrPreviewMeta" class="preview-meta">
        <el-tag>{{ reOcrPreviewMeta.fileName }}</el-tag>
        <el-tag type="info">字数：{{ reOcrPreviewMeta.charCount }}</el-tag>
        <el-tag :type="reOcrPreviewMeta.ocrUsed ? 'warning' : 'success'">
          {{ reOcrPreviewMeta.ocrUsed ? 'OCR' : '直接解析' }}
        </el-tag>
      </div>
      <div v-if="showStructuredSection(reOcrPreviewMeta)" class="preview-section">
        <div class="preview-section-title">{{ showOcrSection(reOcrPreviewMeta) ? '正文文本' : '解析文本' }}</div>
        <el-input v-model="reOcrStructuredText" type="textarea" :rows="showOcrSection(reOcrPreviewMeta) ? 12 : 22" />
      </div>
      <div v-if="showOcrSection(reOcrPreviewMeta)" class="preview-section">
        <div class="preview-section-title">{{ showStructuredSection(reOcrPreviewMeta) ? '图片 OCR 文本' : 'OCR 文本' }}</div>
        <el-input v-model="reOcrOcrText" type="textarea" :rows="showStructuredSection(reOcrPreviewMeta) ? 10 : 22" />
      </div>
      <template #footer>
        <el-button @click="reOcrPreviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reOcrApplying" @click="handleApplyReOcr">覆盖原文档</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile, type UploadInstance } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import {
  confirmRagDocument,
  deleteRagDocument,
  getRagDocumentDetail,
  getRagDocuments,
  getRagOcrSettings,
  parseRagDocument,
  reOcrRagDocumentApply,
  reOcrRagDocumentPreview,
  renameRagDocument,
  saveRagOcrSettings,
  uploadRagDocument,
  type RagDocumentDetail,
  type RagDocumentInfo,
  type RagOcrConfig,
  type RagParsePreview
} from '@/api/rag'
import { useUserStore } from '@/stores/user'

type ScopeValue = 'PUBLIC' | 'PRIVATE'

const userStore = useUserStore()

const uploadRef = ref<UploadInstance>()
const selectedFile = ref<File | null>(null)
const selectedScope = ref<ScopeValue>('PRIVATE')
const parsing = ref(false)
const confirming = ref(false)
const uploadingDirect = ref(false)

const documents = ref<RagDocumentInfo[]>([])
const loadingDocs = ref(false)
const searchKey = ref('')
const documentScopeFilter = ref<'ALL' | ScopeValue>('ALL')

const ocrDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const previewStructuredText = ref('')
const previewOcrText = ref('')
const previewMeta = ref<RagParsePreview | null>(null)
const ocrSettings = ref({
  useCustom: false,
  baseUrl: '',
  apiKey: '',
  model: ''
})

const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const documentDetail = ref<RagDocumentDetail | null>(null)

const reOcringId = ref<number | null>(null)
const reOcrPreviewDialogVisible = ref(false)
const reOcrStructuredText = ref('')
const reOcrOcrText = ref('')
const reOcrPreviewMeta = ref<RagParsePreview | null>(null)
const reOcrTargetId = ref<number | null>(null)
const reOcrApplying = ref(false)

const selectedFileName = computed(() => selectedFile.value?.name || '')

const selectedFileExt = computed(() => {
  const fileName = selectedFileName.value
  const lastDotIndex = fileName.lastIndexOf('.')
  return lastDotIndex >= 0 ? fileName.substring(lastDotIndex + 1).toLowerCase() : ''
})

const isOcrRequired = computed(() => ['pdf', 'png', 'jpg', 'jpeg', 'webp', 'bmp'].includes(selectedFileExt.value))

const processHint = computed(() => {
  if (!selectedFile.value) {
    return '请先选择文件。普通文档可直接上传并入库；PDF 和图片文件必须先做 OCR 预览。docx 会自动识别内嵌图片文字，Markdown 仅解析文本内容。'
  }
  if (isOcrRequired.value) {
    return '当前文件属于 PDF 或图片类型，必须先进行 OCR 预览，确认识别结果后才能入库。'
  }
  if (selectedFileExt.value === 'docx') {
    return '当前是 docx 文档：正文会直接提取；若存在内嵌图片，会尝试 OCR 后一并入库，图片 OCR 失败不会影响正文入库。'
  }
  if (selectedFileExt.value === 'doc') {
    return '当前是 doc 文档：正文会直接提取；若存在内嵌图片，会尝试 OCR 后一并入库，图片 OCR 失败不会影响正文入库。'
  }
  if (selectedFileExt.value === 'md') {
    return '当前是 Markdown 文档：目前只解析 .md 文件中的文本内容，不会跟随图片链接自动 OCR。'
  }
  return '当前文件可直接上传并入库；如果想先检查抽取文本，也可以使用 OCR 预览。'
})

const filteredDocuments = computed(() => {
  const keyword = searchKey.value.trim().toLowerCase()
  return documents.value.filter((doc) => {
    const matchScope = documentScopeFilter.value === 'ALL' || doc.knowledgeScope === documentScopeFilter.value
    const matchKeyword = !keyword || doc.fileName?.toLowerCase().includes(keyword)
    return matchScope && matchKeyword
  })
})

function currentScope(): ScopeValue {
  return userStore.role === 'ADMIN' ? selectedScope.value : 'PRIVATE'
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
  previewDialogVisible.value = false
  previewMeta.value = null
  previewStructuredText.value = ''
  previewOcrText.value = ''
}

function handlePrimaryAction() {
  if (isOcrRequired.value) {
    return handleParse()
  }
  return handleDirectUpload()
}

function resetUploadState() {
  previewDialogVisible.value = false
  previewMeta.value = null
  previewStructuredText.value = ''
  previewOcrText.value = ''
  selectedFile.value = null
  uploadRef.value?.clearFiles()
}

function showStructuredSection(preview: RagParsePreview | null) {
  return !!preview && (!!preview.structuredText || !preview.ocrText)
}

function showOcrSection(preview: RagParsePreview | null) {
  return !!preview && !!preview.ocrText
}

function buildPreviewCombinedText(structuredText: string, ocrText: string) {
  const sections: string[] = []
  const normalizedStructuredText = structuredText.trim()
  const normalizedOcrText = ocrText.trim()

  if (normalizedStructuredText) {
    sections.push(normalizedStructuredText)
  }
  if (normalizedOcrText) {
    sections.push(normalizedStructuredText ? `[图片 OCR 文本]\n${normalizedOcrText}` : normalizedOcrText)
  }
  return sections.join('\n\n')
}

function canDeleteRow(row: RagDocumentInfo) {
  if (row.knowledgeScope === 'PUBLIC') {
    return userStore.role === 'ADMIN'
  }
  return row.ownerUserId === userStore.id
}

function openFile(url: string) {
  window.open(url, '_blank')
}

async function loadOcrSettings() {
  try {
    const parsed = await getRagOcrSettings()
    ocrSettings.value = {
      useCustom: !!(parsed?.baseUrl || parsed?.apiKey || parsed?.model),
      baseUrl: parsed?.baseUrl || '',
      apiKey: parsed?.apiKey || '',
      model: parsed?.model || ''
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
  ElMessage.success('OCR 设置已保存')
}

function resetOcrSettings() {
  ocrSettings.value = { useCustom: false, baseUrl: '', apiKey: '', model: '' }
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
    previewStructuredText.value = preview.structuredText || ''
    previewOcrText.value = preview.ocrText || ''
    previewDialogVisible.value = true
    ElMessage.success(preview.extractedText ? '文档解析完成，请确认后入库' : '未提取到文本，请检查文件或 OCR 配置')
  } finally {
    parsing.value = false
  }
}

async function handleDirectUpload() {
  if (!selectedFile.value) return
  if (isOcrRequired.value) {
    ElMessage.warning('PDF 和图片文件必须先进行 OCR 预览')
    return
  }
  uploadingDirect.value = true
  try {
    await uploadRagDocument(selectedFile.value, currentScope())
    ElMessage.success('文档已直接上传到知识库')
    resetUploadState()
    await fetchDocuments()
  } finally {
    uploadingDirect.value = false
  }
}

async function handleConfirmUpload() {
  if (!selectedFile.value) return
  confirming.value = true
  try {
    await confirmRagDocument(
      selectedFile.value,
      buildPreviewCombinedText(previewStructuredText.value, previewOcrText.value),
      currentScope()
    )
    ElMessage.success('文档入库成功')
    resetUploadState()
    await fetchDocuments()
  } finally {
    confirming.value = false
  }
}

async function openDocumentDetail(id: number) {
  detailDialogVisible.value = true
  detailLoading.value = true
  documentDetail.value = null
  try {
    documentDetail.value = await getRagDocumentDetail(id)
  } finally {
    detailLoading.value = false
  }
}

async function handleRename(row: RagDocumentInfo) {
  const result = await ElMessageBox.prompt('请输入新的文档名称', '重命名文档', {
    inputValue: row.fileName,
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).catch(() => null)
  if (!result?.value) return
  await renameRagDocument(row.id, result.value)
  ElMessage.success('文档名称已更新')
  await fetchDocuments()
  if (documentDetail.value?.id === row.id) {
    await openDocumentDetail(row.id)
  }
}

async function handleReOcr(id: number) {
  reOcringId.value = id
  try {
    const preview = await reOcrRagDocumentPreview(id)
    reOcrTargetId.value = id
    reOcrPreviewMeta.value = preview
    reOcrStructuredText.value = preview.structuredText || ''
    reOcrOcrText.value = preview.ocrText || ''
    reOcrPreviewDialogVisible.value = true
  } finally {
    reOcringId.value = null
  }
}

async function handleApplyReOcr() {
  if (reOcrTargetId.value == null) return
  reOcrApplying.value = true
  try {
    await reOcrRagDocumentApply(
      reOcrTargetId.value,
      buildPreviewCombinedText(reOcrStructuredText.value, reOcrOcrText.value)
    )
    ElMessage.success('重新 OCR 结果已覆盖原文档')
    reOcrPreviewDialogVisible.value = false
    await fetchDocuments()
    if (documentDetail.value?.id === reOcrTargetId.value) {
      await openDocumentDetail(reOcrTargetId.value)
    }
  } finally {
    reOcrApplying.value = false
  }
}

async function handleDelete(id: number) {
  await deleteRagDocument(id)
  ElMessage.success('文档已删除')
  await fetchDocuments()
}

onMounted(async () => {
  await loadOcrSettings()
  await fetchDocuments()
})
</script>

<style scoped>
.rag-container {
  padding: 20px;
  max-width: 1200px;
}

.desc {
  color: #909399;
  margin-bottom: 20px;
  font-size: 14px;
}

.upload-panel,
.process-panel {
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  padding: 16px;
  background: var(--el-bg-color);
}

.process-panel {
  margin-top: 16px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.panel-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.scope-switch {
  margin-top: 16px;
}

.upload-note {
  margin: 10px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.process-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.process-hint {
  margin: 12px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  line-height: 1.6;
}

.primary-action-row {
  margin-top: 14px;
}

.secondary-action-row {
  margin-top: 8px;
}

.secondary-action-row :deep(.el-button) {
  padding-left: 0;
}

.docs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 32px 0 12px;
  gap: 16px;
  flex-wrap: wrap;
}

.docs-filters {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.preview-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.preview-section + .preview-section {
  margin-top: 14px;
}

.preview-section-title {
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.detail-meta {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-text-title {
  font-weight: 600;
}
</style>
