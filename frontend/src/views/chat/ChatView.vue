<template>
  <div class="chat-container">
    <!-- Left sidebar: history -->
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <el-button type="primary" size="small" @click="newChat">
          <el-icon><Plus /></el-icon> 新对话
        </el-button>
      </div>
      <div class="history-list">
        <div
          v-for="id in historyIds"
          :key="id"
          :class="['history-item', { active: id === currentChatId }]"
          @click="loadChat(id)"
        >
          <el-icon><ChatLineSquare /></el-icon>
          <span class="history-id">{{ id.substring(0, 16) }}...</span>
        </div>
        <el-empty v-if="historyIds.length === 0" description="暂无历史" :image-size="60" />
      </div>
    </div>

    <!-- Right: chat area -->
    <div class="chat-main">
      <!-- Top bar: mode & model -->
      <div class="chat-topbar">
        <el-radio-group v-model="chatMode" size="small">
          <el-radio-button value="DIRECT">直接对话</el-radio-button>
          <el-radio-button value="KNOWLEDGE_BASE">知识库</el-radio-button>
          <el-radio-button value="INTERNET_SEARCH">联网搜索</el-radio-button>
        </el-radio-group>
        <el-select v-model="model" size="small" style="width: 200px" placeholder="选择模型">
          <el-option label="Qwen3-235B (DashScope)" value="qwen3-235b-a22b" />
          <el-option label="Qwen3-30B (DashScope)" value="qwen3-30b-a3b" />
          <el-option label="Qwen2.5-VL (DashScope)" value="qwen2.5-vl-72b-instruct" />
          <el-option label="DeepSeek-R1 (iFlow)" value="deepseek-ai/DeepSeek-R1" />
          <el-option label="DeepSeek-V3 (iFlow)" value="deepseek-ai/DeepSeek-V3-0324" />
        </el-select>
      </div>

      <!-- Messages -->
      <div class="messages-area" ref="messagesRef">
        <ChatMessage v-for="(msg, i) in messages" :key="i" :msg="msg" />
        <div v-if="sending" class="typing-indicator">
          <el-icon class="is-loading"><Loading /></el-icon> AI 正在思考...
        </div>
      </div>

      <!-- Image preview -->
      <div v-if="uploadedImages.length" class="image-preview">
        <div v-for="(img, i) in uploadedImages" :key="i" class="preview-item">
          <el-image :src="img.previewUrl" fit="cover" style="width:60px;height:60px;border-radius:6px" />
          <el-icon class="remove-img" @click="uploadedImages.splice(i, 1)"><Close /></el-icon>
        </div>
      </div>

      <!-- Input area -->
      <div class="chat-input">
        <el-upload
          :show-file-list="false"
          :before-upload="handleImageUpload"
          accept="image/*"
          multiple
        >
          <el-button :icon="Picture" circle size="small" />
        </el-upload>
        <el-input
          v-model="userInput"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          placeholder="输入消息..."
          @keydown.enter.exact.prevent="sendMessage"
        />
        <el-button type="primary" :icon="Promotion" circle @click="sendMessage" :disabled="!userInput.trim() || sending" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { Picture, Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ChatMessage from '@/components/ChatMessage.vue'
import { sendChat, getHistoryTypes, getChatHistory } from '@/api/chat'
import { uploadImages } from '@/api/upload'
import { useUserStore } from '@/stores/user'
import type { MessageVO, ChatMode, ImagesResponse } from '@/types'

const userStore = useUserStore()
const messagesRef = ref<HTMLElement>()
const messages = ref<MessageVO[]>([])
const userInput = ref('')
const sending = ref(false)
const chatMode = ref<ChatMode>('DIRECT')
const model = ref('qwen3-235b-a22b')
const currentChatId = ref('')
const historyIds = ref<string[]>([])
const uploadedImages = ref<ImagesResponse[]>([])

function generateChatId() {
  return 'chat_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8)
}

function newChat() {
  currentChatId.value = generateChatId()
  messages.value = []
  uploadedImages.value = []
}

async function loadHistoryList() {
  try {
    const types = ['DIRECT', 'KNOWLEDGE_BASE', 'INTERNET_SEARCH']
    const allIds = new Set<string>()
    for (const t of types) {
      const ids = await getHistoryTypes(t)
      if (ids) ids.forEach((id) => allIds.add(id))
    }
    historyIds.value = Array.from(allIds)
  } catch { /* ignore */ }
}

async function loadChat(chatId: string) {
  currentChatId.value = chatId
  try {
    const history = await getChatHistory(chatId)
    messages.value = history || []
    scrollToBottom()
  } catch { /* ignore */ }
}

async function handleImageUpload(file: File) {
  try {
    const res = await uploadImages([file])
    if (res && res.length) {
      uploadedImages.value.push(...res)
    }
  } catch {
    ElMessage.error('图片上传失败')
  }
  return false // prevent default upload
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

async function sendMessage() {
  const input = userInput.value.trim()
  if (!input || sending.value) return

  if (!currentChatId.value) currentChatId.value = generateChatId()

  messages.value.push({ role: 'USER', content: input })
  userInput.value = ''
  sending.value = true
  scrollToBottom()

  try {
    const reply = await sendChat({
      userName: userStore.username,
      chatId: currentChatId.value,
      userInput: input,
      chatMode: chatMode.value,
      model: model.value,
      imageFiles: uploadedImages.value.length ? uploadedImages.value : null
    })
    messages.value.push({ role: 'ASSISTANT', content: reply || '(无回复)' })
    uploadedImages.value = []
    // Refresh history list
    if (!historyIds.value.includes(currentChatId.value)) {
      historyIds.value.unshift(currentChatId.value)
    }
  } catch {
    messages.value.push({ role: 'ASSISTANT', content: '请求失败，请重试。' })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

onMounted(() => {
  newChat()
  loadHistoryList()
})
</script>

<style scoped>
.chat-container { display: flex; height: calc(100vh - 120px); background: #fff; border-radius: 8px; overflow: hidden; }
.chat-sidebar { width: 240px; border-right: 1px solid #e4e7ed; display: flex; flex-direction: column; background: #fafafa; }
.sidebar-header { padding: 12px; border-bottom: 1px solid #e4e7ed; }
.history-list { flex: 1; overflow-y: auto; padding: 8px; }
.history-item {
  display: flex; align-items: center; gap: 8px; padding: 10px 12px;
  border-radius: 6px; cursor: pointer; font-size: 13px; color: #606266;
  margin-bottom: 4px; transition: background 0.2s;
}
.history-item:hover { background: #ecf5ff; }
.history-item.active { background: #d9ecff; color: #409eff; }
.history-id { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chat-main { flex: 1; display: flex; flex-direction: column; }
.chat-topbar { padding: 12px 16px; border-bottom: 1px solid #e4e7ed; display: flex; align-items: center; gap: 16px; }
.messages-area { flex: 1; overflow-y: auto; padding: 20px; }
.typing-indicator { color: #909399; font-size: 13px; display: flex; align-items: center; gap: 6px; }
.image-preview { display: flex; gap: 8px; padding: 8px 16px; flex-wrap: wrap; }
.preview-item { position: relative; }
.remove-img { position: absolute; top: -6px; right: -6px; background: #f56c6c; color: #fff; border-radius: 50%; cursor: pointer; font-size: 14px; }
.chat-input { display: flex; align-items: flex-end; gap: 8px; padding: 12px 16px; border-top: 1px solid #e4e7ed; }
.chat-input .el-textarea { flex: 1; }
</style>
