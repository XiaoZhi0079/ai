<template>
  <div class="chat-container">
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <el-button type="primary" size="small" @click="newChat">
          <el-icon><Plus /></el-icon> {{ texts.newChat }}
        </el-button>
      </div>

      <div class="history-list">
        <div
          v-for="item in historyItems"
          :key="item.chatId"
          :class="['history-item', { active: item.chatId === currentChatId }]"
          @click="loadChat(item.chatId)"
        >
          <el-icon><ChatLineSquare /></el-icon>
          <span class="history-id">{{ item.title || item.chatId.substring(0, 16) + '...' }}</span>
        </div>
        <el-empty v-if="historyItems.length === 0" :description="texts.noHistory" :image-size="60" />
      </div>
    </div>

    <div class="chat-main">
      <div class="chat-topbar">
        <el-radio-group v-model="chatMode" size="small">
          <el-radio-button value="DIRECT">{{ texts.direct }}</el-radio-button>
          <el-radio-button value="KNOWLEDGE_BASE">{{ texts.knowledgeBase }}</el-radio-button>
          <el-radio-button value="INTERNET_SEARCH">{{ texts.internetSearch }}</el-radio-button>
        </el-radio-group>

        <el-select
          v-model="model"
          size="small"
          style="width: 220px"
          :placeholder="texts.selectModel"
          :loading="modelsLoading"
          :disabled="modelsLoading || modelOptions.length === 0"
        >
          <el-option v-for="opt in modelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </div>

      <div class="messages-area" ref="messagesRef">
        <ChatMessage v-for="(msg, index) in messages" :key="index" :msg="msg" />
        <div v-if="sending || generatingImage" class="typing-indicator">
          <el-icon class="is-loading"><Loading /></el-icon>
          {{ sending ? texts.aiThinking : texts.imageGenerating }}
        </div>
      </div>

      <div v-if="uploadedImages.length" class="image-preview">
        <div v-for="(img, index) in uploadedImages" :key="index" class="preview-item">
          <el-image :src="img.previewUrl" fit="cover" style="width: 60px; height: 60px; border-radius: 6px" />
          <el-icon class="remove-img" @click="uploadedImages.splice(index, 1)"><Close /></el-icon>
        </div>
      </div>

      <div class="chat-input">
        <el-upload :show-file-list="false" :before-upload="handleImageUpload" accept="image/*" multiple>
          <el-button :icon="Picture" circle size="small" />
        </el-upload>

        <el-button size="small" :loading="generatingImage" :disabled="!userInput.trim() || sending || generatingImage" @click="handleGenerateImage">
          {{ texts.generateImage }}
        </el-button>

        <el-input
          v-model="userInput"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          :placeholder="texts.inputPlaceholder"
          @keydown.enter.exact.prevent="sendMessage"
        />

        <el-button type="primary" :icon="Promotion" circle :disabled="!userInput.trim() || sending || generatingImage" @click="sendMessage" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { ChatLineSquare, Close, Loading, Picture, Plus, Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ChatMessage from '@/components/ChatMessage.vue'
import { generateImage, getChatHistory, getHistoryList, getModelOptions, sendChat } from '@/api/chat'
import { uploadImages } from '@/api/upload'
import { useUserStore } from '@/stores/user'
import type { ChatMode, ConversationItem, ImagesResponse, MessageVO, ModelOption } from '@/types'

defineOptions({ name: 'ChatView' })

const texts = {
  newChat: '\u65b0\u5bf9\u8bdd',
  noHistory: '\u6682\u65e0\u5386\u53f2',
  direct: '\u76f4\u63a5\u5bf9\u8bdd',
  knowledgeBase: '\u77e5\u8bc6\u5e93',
  internetSearch: '\u8054\u7f51\u641c\u7d22',
  selectModel: '\u9009\u62e9\u6a21\u578b',
  aiThinking: 'AI \u6b63\u5728\u601d\u8003...',
  imageGenerating: '\u6b63\u5728\u751f\u6210\u56fe\u7247...',
  generateImage: '\u751f\u56fe',
  inputPlaceholder: '\u8f93\u5165\u6d88\u606f...',
  selectModelFirst: '\u8bf7\u5148\u9009\u62e9\u6a21\u578b',
  uploadImageFailed: '\u56fe\u7247\u4e0a\u4f20\u5931\u8d25',
  chatFailed: '\u8bf7\u6c42\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5\u3002',
  modelLoadFailed: '\u6a21\u578b\u5217\u8868\u52a0\u8f7d\u5931\u8d25',
  generatedImage: '\u5df2\u751f\u6210\u56fe\u7247',
  imageFailed: '\u56fe\u7247\u751f\u6210\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5\u3002',
  imagePromptPrefix: '\u3010\u6587\u751f\u56fe\u3011'
} as const

const userStore = useUserStore()
const messagesRef = ref<HTMLElement>()
const messages = ref<MessageVO[]>([])
const userInput = ref('')
const sending = ref(false)
const generatingImage = ref(false)
const chatMode = ref<ChatMode>('DIRECT')
const model = ref('')
const modelOptions = ref<ModelOption[]>([])
const modelsLoading = ref(false)
const currentChatId = ref('')
const historyItems = ref<ConversationItem[]>([])
const uploadedImages = ref<ImagesResponse[]>([])

function generateChatId() {
  return 'chat_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8)
}

function newChat() {
  currentChatId.value = generateChatId()
  messages.value = []
  uploadedImages.value = []
  userInput.value = ''
}

async function loadHistoryList() {
  try {
    historyItems.value = (await getHistoryList()) || []
  } catch {
  }
}

async function loadChat(chatId: string) {
  currentChatId.value = chatId
  try {
    messages.value = (await getChatHistory(chatId)) || []
    scrollToBottom()
  } catch {
  }
}

async function handleImageUpload(file: File) {
  try {
    const response = await uploadImages([file])
    if (response && response.length) {
      uploadedImages.value.push(...response)
    }
  } catch {
    ElMessage.error(texts.uploadImageFailed)
  }
  return false
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

function ensureCurrentChat() {
  if (!currentChatId.value) {
    currentChatId.value = generateChatId()
  }
}

function appendNewHistoryTitle(title: string) {
  if (!historyItems.value.some((item) => item.chatId === currentChatId.value)) {
    historyItems.value.unshift({ chatId: currentChatId.value, title: title.substring(0, 50) || null })
  }
}

async function sendMessage() {
  const input = userInput.value.trim()
  if (!input || sending.value || generatingImage.value) return
  if (!model.value) {
    ElMessage.warning(texts.selectModelFirst)
    return
  }

  ensureCurrentChat()

  const currentImages = uploadedImages.value.map((img) => img.previewUrl || img.imageUrl)
  messages.value.push({ role: 'USER', content: input, images: currentImages.length ? currentImages : undefined })
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
    messages.value.push({ role: 'ASSISTANT', content: reply || '(empty)' })
    uploadedImages.value = []
    appendNewHistoryTitle(input)
  } catch {
    messages.value.push({ role: 'ASSISTANT', content: texts.chatFailed })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

async function handleGenerateImage() {
  const prompt = userInput.value.trim()
  if (!prompt || generatingImage.value || sending.value) return

  ensureCurrentChat()
  generatingImage.value = true
  messages.value.push({ role: 'USER', content: `${texts.imagePromptPrefix}${prompt}` })
  userInput.value = ''
  scrollToBottom()

  try {
    const image = await generateImage(prompt, currentChatId.value)
    const imageUrl = image.previewUrl || image.imageUrl
    messages.value.push({
      role: 'ASSISTANT',
      content: texts.generatedImage,
      images: imageUrl ? [imageUrl] : undefined
    })
    appendNewHistoryTitle(prompt)
  } catch {
    messages.value.push({ role: 'ASSISTANT', content: texts.imageFailed })
  } finally {
    generatingImage.value = false
    scrollToBottom()
  }
}

async function loadModelOptions() {
  modelsLoading.value = true
  try {
    modelOptions.value = (await getModelOptions()) || []
    if (modelOptions.value.length > 0 && !model.value) {
      model.value = modelOptions.value[0].value
    }
  } catch {
    ElMessage.error(texts.modelLoadFailed)
  } finally {
    modelsLoading.value = false
  }
}

onMounted(() => {
  newChat()
  loadHistoryList()
  loadModelOptions()
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
