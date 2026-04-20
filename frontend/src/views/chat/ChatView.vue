<template>
  <div class="chat-container">
    <aside class="chat-sidebar">
      <div class="sidebar-header">
        <div>
          <span class="sidebar-header__eyebrow">Conversation</span>
          <h3>历史会话</h3>
        </div>

        <el-button type="primary" class="new-chat-btn" @click="newChat(true)">
          <el-icon><Plus /></el-icon>
          {{ texts.newChat }}
        </el-button>
      </div>

      <div class="history-list">
        <div
          v-for="item in historyItems"
          :key="item.chatId"
          :class="['history-item', { active: item.chatId === currentChatId }]"
          role="button"
          tabindex="0"
          @click="loadChat(item.chatId)"
          @keydown.enter.prevent="loadChat(item.chatId)"
          @keydown.space.prevent="loadChat(item.chatId)"
        >
          <div class="history-item__icon">
            <el-icon><ChatLineSquare /></el-icon>
          </div>
          <div class="history-item__body">
            <div class="history-title-row">
              <span class="history-id">{{ item.title || texts.newChatPlaceholder }}</span>
            </div>
            <span class="history-meta">{{ item.chatId === currentChatId ? texts.currentConversation : texts.clickToOpen }}</span>
          </div>
          <button class="history-delete-btn" type="button" @click.stop="handleDeleteChat(item.chatId)">
            <el-icon><Delete /></el-icon>
          </button>
        </div>

        <el-empty v-if="historyItems.length === 0" :description="texts.noHistory" :image-size="60" class="history-empty" />
      </div>
    </aside>

    <section class="chat-main">
      <div class="chat-topbar">
        <div class="chat-topbar__left">
          <div>
            <span class="chat-topbar__eyebrow">Interaction Mode</span>
            <h3>智能对话工作区</h3>
          </div>

          <el-radio-group v-model="chatMode" class="mode-switcher">
            <el-radio-button value="DIRECT">{{ texts.direct }}</el-radio-button>
            <el-radio-button value="KNOWLEDGE_BASE">{{ texts.knowledgeBase }}</el-radio-button>
            <el-radio-button value="INTERNET_SEARCH">{{ texts.internetSearch }}</el-radio-button>
            <el-radio-button value="DATA_QUERY">{{ texts.dataQuery }}</el-radio-button>
          </el-radio-group>
        </div>

        <div class="chat-topbar__right">
          <span class="model-label">{{ texts.modelLabel }}</span>
          <el-select
            v-model="model"
            style="width: 240px"
            :placeholder="texts.selectModel"
            :loading="modelsLoading"
            :disabled="modelsLoading || modelOptions.length === 0"
          >
            <el-option v-for="opt in modelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </div>
      </div>

      <div class="messages-area" ref="messagesRef">
        <div v-if="messages.length === 0" class="chat-empty-state">
          <span class="chat-empty-state__eyebrow">Ready to Ask</span>
          <h3>开始新的智能对话</h3>
          <p>{{ emptyStateText }}</p>
        </div>

        <ChatMessage v-for="(msg, index) in messages" :key="index" :msg="msg" />

        <div v-if="sending || generatingImage" class="typing-indicator">
          <el-icon class="is-loading"><Loading /></el-icon>
          {{ sending ? texts.aiThinking : texts.imageGenerating }}
        </div>
      </div>

        <div v-if="chatMode !== 'DATA_QUERY' && uploadedImages.length" class="image-preview">
          <div v-for="(img, index) in uploadedImages" :key="index" class="preview-item">
            <el-image :src="img.previewUrl" fit="cover" style="width: 60px; height: 60px; border-radius: 10px" />
            <el-icon class="remove-img" @click="removeUploadedImage(index)"><Close /></el-icon>
          </div>
        </div>

      <div class="chat-input">
        <div class="chat-input__toolbar">
          <el-upload v-if="chatMode !== 'DATA_QUERY'" :show-file-list="false" :before-upload="handleImageUpload" accept="image/*" multiple>
            <el-button class="tool-btn" :icon="Picture" circle />
          </el-upload>
        </div>

        <div class="chat-input__editor">
          <el-input
            v-model="userInput"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 5 }"
            :placeholder="inputPlaceholder"
            @keydown.enter.exact.prevent="sendMessage"
          />

          <el-button type="primary" class="send-btn" :disabled="!userInput.trim() || sending || generatingImage" @click="sendMessage">
            <el-icon><Promotion /></el-icon>
            发送
          </el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ChatLineSquare, Close, Delete, Loading, Picture, Plus, Promotion } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ChatMessage from '@/components/ChatMessage.vue'
import { deleteChatHistory, generateImage, getChatHistory, getHistoryList, getModelOptions, queryData, streamChat } from '@/api/chat'
import { uploadImages } from '@/api/upload'
import { useUserStore } from '@/stores/user'
import type { AiSqlQueryResult, ChatMode, ConversationItem, ImagesResponse, MessageVO, ModelOption } from '@/types'
import { appendSessionChunk, beginSessionRequest, ensureChatSession, finishSessionRequest, isChatSessionBusy, type ChatSessionMap } from './chatSessions'

defineOptions({ name: 'ChatView' })

const texts = {
  newChat: '新对话',
  noHistory: '暂无历史',
  direct: '直接对话',
  knowledgeBase: '知识库',
  internetSearch: '联网搜索',
  dataQuery: '数据查询',
  selectModel: '选择模型',
  modelLabel: '当前模型',
  aiThinking: 'AI 正在思考...',
  imageGenerating: '正在生成图片...',
  generateImage: '生图',
  inputPlaceholder: '输入消息...',
  selectModelFirst: '请先选择模型',
  uploadImageFailed: '图片上传失败',
  chatFailed: '请求失败，请重试。',
  dataQueryFailed: '数据查询失败，请重试。',
  modelLoadFailed: '模型列表加载失败',
  generatedImage: '已生成图片',
  imageFailed: '图片生成失败，请重试。',
  imagePromptPrefix: '【文生图】',
  dataQueryPlaceholder: '输入你想查询的数据问题...',
  currentConversation: '当前会话',
  clickToOpen: '点击查看',
  newChatPlaceholder: '新对话',
  deleteSuccess: '历史会话已删除',
  deleteFailed: '删除历史会话失败',
  deleteConfirmTitle: '删除会话',
  deleteConfirmText: '确定删除这条历史会话吗？此操作不可恢复。',
  deleteConfirmButton: '删除',
  deleteCancelButton: '取消'
} as const

const userStore = useUserStore()
const messagesRef = ref<HTMLElement>()
const userInput = ref('')
const chatMode = ref<ChatMode>('DIRECT')
const model = ref('')
const modelOptions = ref<ModelOption[]>([])
const modelsLoading = ref(false)
const currentChatId = ref('')
const historyItems = ref<ConversationItem[]>([])
const chatSessions = reactive<ChatSessionMap<MessageVO, ImagesResponse>>({})
let requestSeed = 0

const currentSession = computed(() => currentChatId.value ? chatSessions[currentChatId.value] : undefined)
const messages = computed(() => currentSession.value?.messages ?? [])
const sending = computed(() => Boolean(currentSession.value?.sending))
const generatingImage = computed(() => Boolean(currentSession.value?.generatingImage))
const uploadedImages = computed(() => currentSession.value?.uploadedImages ?? [])

const inputPlaceholder = computed(() => chatMode.value === 'DATA_QUERY' ? texts.dataQueryPlaceholder : texts.inputPlaceholder)
const emptyStateText = computed(() => {
  const map: Record<ChatMode, string> = {
    DIRECT: '可直接提出校园问题、学习问题或一般性问题，系统会为你生成自然语言回复。',
    KNOWLEDGE_BASE: '适合围绕知识库文档进行提问，帮助你更快检索并理解校园资料内容。',
    INTERNET_SEARCH: '适合需要结合联网信息进行分析、补充事实或扩展知识背景的场景。',
    DATA_QUERY: '输入自然语言即可查询课程、成绩、教师或学生相关数据。'
  }

  return map[chatMode.value]
})

function generateChatId() {
  return 'chat_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8)
}

function getChatSession(chatId: string) {
  return ensureChatSession(chatSessions, chatId)
}

function resetChatSession(chatId: string, loaded = true) {
  const session = getChatSession(chatId)
  session.messages = []
  session.uploadedImages = []
  session.sending = false
  session.generatingImage = false
  session.loaded = loaded
  session.activeRequestId = undefined
  return session
}

function upsertHistoryItem(chatId: string, title?: string | null, localOnly = false) {
  const existingIndex = historyItems.value.findIndex((item) => item.chatId === chatId)
  const nextItem = { chatId, title: title || null, localOnly }

  if (existingIndex === 0) {
    historyItems.value[0] = nextItem
    return
  }

  if (existingIndex > 0) {
    historyItems.value.splice(existingIndex, 1)
  }

  historyItems.value.unshift(nextItem)
}

function newChat(addPlaceholder = false) {
  const chatId = generateChatId()
  currentChatId.value = chatId
  resetChatSession(chatId)
  userInput.value = ''

  if (addPlaceholder) {
    upsertHistoryItem(chatId, texts.newChatPlaceholder, true)
  }
}

async function loadHistoryList() {
  try {
    const remoteItems = ((await getHistoryList()) || []).map((item) => ({ ...item, localOnly: false }))
    const placeholderItems = historyItems.value.filter((item) => item.localOnly && !remoteItems.some((remote) => remote.chatId === item.chatId))
    historyItems.value = [...placeholderItems, ...remoteItems]
  } catch {
  }
}

async function loadChat(chatId: string) {
  currentChatId.value = chatId
  const session = getChatSession(chatId)
  if (session.loaded || isChatSessionBusy(session)) {
    scrollToBottom()
    return
  }

  try {
    const history = (await getChatHistory(chatId)) || []
    const targetSession = chatSessions[chatId]
    if (!targetSession || targetSession.loaded || isChatSessionBusy(targetSession) || targetSession.messages.length > 0) {
      return
    }

    targetSession.messages = history
    targetSession.loaded = true
    if (currentChatId.value === chatId) {
      scrollToBottom()
    }
  } catch {
  }
}

function clearCurrentWorkspace() {
  currentChatId.value = ''
  userInput.value = ''
}

async function handleDeleteChat(chatId: string) {
  const targetItem = historyItems.value.find((item) => item.chatId === chatId)

  try {
    await ElMessageBox.confirm(
      texts.deleteConfirmText,
      texts.deleteConfirmTitle,
      {
        type: 'warning',
        confirmButtonText: texts.deleteConfirmButton,
        cancelButtonText: texts.deleteCancelButton
      }
    )
  } catch {
    return
  }

  try {
    if (!targetItem?.localOnly) {
      await deleteChatHistory(chatId)
    }

    historyItems.value = historyItems.value.filter((item) => item.chatId !== chatId)
    delete chatSessions[chatId]
    const nextItem = historyItems.value[0]

    if (currentChatId.value === chatId) {
      if (nextItem) {
        if (nextItem.localOnly) {
          currentChatId.value = nextItem.chatId
          getChatSession(nextItem.chatId)
          userInput.value = ''
          scrollToBottom()
        } else {
          await loadChat(nextItem.chatId)
        }
      } else {
        clearCurrentWorkspace()
      }
    }

    ElMessage.success(texts.deleteSuccess)
  } catch {
    ElMessage.error(texts.deleteFailed)
  }
}

async function handleImageUpload(file: File) {
  try {
    ensureCurrentChat()
    const session = getChatSession(currentChatId.value)
    const response = await uploadImages([file])
    if (response && response.length) {
      session.uploadedImages.push(...response)
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
    resetChatSession(currentChatId.value)
    upsertHistoryItem(currentChatId.value, texts.newChatPlaceholder, true)
    return
  }

  getChatSession(currentChatId.value)
}

function appendNewHistoryTitle(chatId: string, title: string) {
  upsertHistoryItem(chatId, title.substring(0, 50) || texts.newChatPlaceholder, false)
}

function removeUploadedImage(index: number) {
  currentSession.value?.uploadedImages.splice(index, 1)
}

async function sendMessage() {
  const input = userInput.value.trim()
  if (!input || sending.value || generatingImage.value) return
  if (!model.value) {
    ElMessage.warning(texts.selectModelFirst)
    return
  }

  ensureCurrentChat()
  const chatId = currentChatId.value
  const session = getChatSession(chatId)
  const requestMode = chatMode.value

  const currentImages = session.uploadedImages.map((img) => img.previewUrl || img.imageUrl)
  session.messages.push({ role: 'USER', content: input, images: currentImages.length ? currentImages : undefined })
  userInput.value = ''
  session.loaded = true
  const requestId = ++requestSeed
  beginSessionRequest(session, 'sending', requestId)
  scrollToBottom()

  try {
    if (requestMode === 'DATA_QUERY') {
      const result = await queryData(input, model.value)
      const targetSession = chatSessions[chatId]
      if (!targetSession || targetSession.activeRequestId !== requestId) {
        return
      }

      targetSession.messages.push(buildDataQueryMessage(result))
      appendNewHistoryTitle(chatId, input)
    } else {
      session.messages.push({ role: 'ASSISTANT', content: '' })
      const assistantIndex = session.messages.length - 1

      await streamChat({
        userName: userStore.username,
        chatId,
        userInput: input,
        chatMode: requestMode,
        model: model.value,
        imageFiles: session.uploadedImages.length ? session.uploadedImages : null
      }, {
        onStart: () => {
          if (currentChatId.value === chatId) {
            scrollToBottom()
          }
        },
        onChunk: (chunk) => {
          const updated = appendSessionChunk(chatSessions[chatId], assistantIndex, chunk, requestId)
          if (updated && currentChatId.value === chatId) {
            scrollToBottom()
          }
        },
        onDone: () => {
          const targetSession = chatSessions[chatId]
          if (!targetSession || targetSession.activeRequestId !== requestId) {
            return
          }

          const assistantMessage = targetSession.messages[assistantIndex]
          if (assistantMessage && !assistantMessage.content) {
            assistantMessage.content = '(empty)'
          }
        },
        onError: (message) => {
          const targetSession = chatSessions[chatId]
          if (!targetSession || targetSession.activeRequestId !== requestId) {
            return
          }

          const assistantMessage = targetSession.messages[assistantIndex]
          if (assistantMessage && message) {
            assistantMessage.content = message
          }
        }
      })

      const targetSession = chatSessions[chatId]
      if (targetSession?.activeRequestId === requestId) {
        targetSession.uploadedImages = []
      }
      appendNewHistoryTitle(chatId, input)
    }
  } catch {
    const targetSession = chatSessions[chatId]
    if (!targetSession || targetSession.activeRequestId !== requestId) {
      return
    }

    const lastMessage = targetSession.messages[targetSession.messages.length - 1]
    if (lastMessage?.role === 'ASSISTANT' && !lastMessage.content) {
      targetSession.messages.pop()
    } else if (lastMessage?.role === 'ASSISTANT' && lastMessage.content) {
      return
    }
    targetSession.messages.push({
      role: 'ASSISTANT',
      content: requestMode === 'DATA_QUERY' ? texts.dataQueryFailed : texts.chatFailed
    })
  } finally {
    finishSessionRequest(chatSessions[chatId], 'sending', requestId)
    if (currentChatId.value === chatId) {
      scrollToBottom()
    }
  }
}

function buildDataQueryMessage(result: AiSqlQueryResult): MessageVO {
  const summary = result.rowCount > 0 ? `已查询到 ${result.rowCount} 条结果。` : '查询已完成，没有匹配结果。'
  return {
    role: 'ASSISTANT',
    kind: 'data-query',
    content: summary,
    dataQuery: result
  }
}

async function handleGenerateImage() {
  const prompt = userInput.value.trim()
  if (!prompt || generatingImage.value || sending.value) return

  ensureCurrentChat()
  const chatId = currentChatId.value
  const session = getChatSession(chatId)
  const requestId = ++requestSeed

  beginSessionRequest(session, 'generatingImage', requestId)
  session.loaded = true
  session.messages.push({ role: 'USER', content: `${texts.imagePromptPrefix}${prompt}` })
  userInput.value = ''
  scrollToBottom()

  try {
    const image = await generateImage(prompt, chatId)
    const imageUrl = image.previewUrl || image.imageUrl
    const targetSession = chatSessions[chatId]
    if (!targetSession || targetSession.activeRequestId !== requestId) {
      return
    }

    targetSession.messages.push({
      role: 'ASSISTANT',
      content: texts.generatedImage,
      images: imageUrl ? [imageUrl] : undefined
    })
    appendNewHistoryTitle(chatId, prompt)
  } catch {
    const targetSession = chatSessions[chatId]
    if (!targetSession || targetSession.activeRequestId !== requestId) {
      return
    }

    targetSession.messages.push({ role: 'ASSISTANT', content: texts.imageFailed })
  } finally {
    finishSessionRequest(chatSessions[chatId], 'generatingImage', requestId)
    if (currentChatId.value === chatId) {
      scrollToBottom()
    }
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
  newChat(true)
  loadHistoryList()
  loadModelOptions()
})

watch(chatMode, (mode) => {
  if (mode === 'DATA_QUERY') {
    if (currentSession.value) {
      currentSession.value.uploadedImages = []
    }
  }
})
</script>

<style scoped>
.chat-container {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 18px;
  height: 100%;
  min-height: 0;
}

.chat-sidebar,
.chat-main {
  min-height: 0;
  border: 1px solid rgba(193, 210, 227, 0.8);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 18px 40px rgba(20, 44, 74, 0.07);
}

.chat-sidebar {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(180deg, #f9fbff 0%, #f4f8fc 100%);
}

.sidebar-header {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 22px;
  border-bottom: 1px solid rgba(223, 232, 241, 0.94);
}

.sidebar-header__eyebrow,
.chat-topbar__eyebrow,
.chat-empty-state__eyebrow {
  display: inline-block;
  color: #1b7ea3;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.sidebar-header h3,
.chat-topbar__left h3,
.chat-empty-state h3 {
  margin: 8px 0 0;
  color: #17314d;
}

.new-chat-btn {
  justify-content: center;
  min-height: 44px;
  border-radius: 14px;
}

.history-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 14px;
}

.history-item {
  display: flex;
  gap: 12px;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
  margin-bottom: 8px;
  padding: 14px;
  border: 1px solid transparent;
  border-radius: 18px;
  background: transparent;
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease, border-color 0.2s ease;
}

.history-item:hover {
  background: #edf4fb;
  border-color: rgba(193, 210, 227, 0.92);
}

.history-item.active {
  background: linear-gradient(135deg, rgba(22, 103, 171, 0.12), rgba(24, 154, 173, 0.12));
  border-color: rgba(78, 145, 208, 0.34);
}

.history-item__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: #e9f2fb;
  color: #1a6eaf;
}

.history-item__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 3px;
  text-align: left;
}

.history-title-row {
  display: flex;
  align-items: center;
  min-height: 22px;
  overflow: hidden;
}

.history-delete-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: #8ba0b5;
  opacity: 0;
  cursor: pointer;
  transition: opacity 0.2s ease, background-color 0.2s ease, color 0.2s ease;
}

.history-item:hover .history-delete-btn,
.history-item:focus-within .history-delete-btn {
  opacity: 1;
}

.history-delete-btn:hover {
  background: rgba(239, 107, 107, 0.12);
  color: #d65353;
}

.history-id {
  display: block;
  flex: 1;
  max-width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  color: #18304b;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.5;
  padding-bottom: 6px;
  margin-bottom: -6px;
  white-space: nowrap;
  scrollbar-width: thin;
  scrollbar-color: transparent transparent;
}

.history-id:hover {
  scrollbar-color: rgba(137, 156, 176, 0.7) transparent;
}

.history-id::-webkit-scrollbar {
  height: 6px;
}

.history-id::-webkit-scrollbar-track {
  background: transparent;
}

.history-id::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: transparent;
}

.history-id:hover::-webkit-scrollbar-thumb {
  background: rgba(137, 156, 176, 0.7);
}

.history-meta {
  color: #7b8ea2;
  font-size: 12px;
  white-space: nowrap;
}

.history-empty {
  margin-top: 32px;
}

.chat-main {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 24px 24px 18px;
  border-bottom: 1px solid rgba(223, 232, 241, 0.94);
}

.chat-topbar__left,
.chat-topbar__right {
  display: flex;
}

.chat-topbar__left {
  flex: 1;
  flex-direction: column;
  gap: 16px;
}

.chat-topbar__right {
  flex-direction: column;
  gap: 8px;
  min-width: 240px;
  align-self: center;
}

.model-label {
  color: #71859b;
  font-size: 13px;
  font-weight: 600;
}

.messages-area {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.65), rgba(243, 248, 253, 0.38));
}

.chat-empty-state {
  max-width: 640px;
  margin: 40px auto 24px;
  padding: 28px;
  border: 1px solid rgba(193, 210, 227, 0.84);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.86);
  text-align: center;
  box-shadow: 0 14px 30px rgba(20, 44, 74, 0.06);
}

.chat-empty-state p {
  margin: 14px 0 0;
  color: #6a7d92;
  line-height: 1.8;
}

.typing-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 10px 14px;
  border-radius: 14px;
  background: #edf4fb;
  color: #6e8299;
  font-size: 13px;
}

.image-preview {
  display: flex;
  gap: 10px;
  padding: 10px 24px 0;
  flex-wrap: wrap;
}

.preview-item {
  position: relative;
}

.remove-img {
  position: absolute;
  top: -6px;
  right: -6px;
  border-radius: 50%;
  background: #ef6b6b;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.chat-input {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px 24px 24px;
  border-top: 1px solid rgba(223, 232, 241, 0.94);
  background: rgba(255, 255, 255, 0.9);
}

.chat-input__toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tool-btn {
  min-height: 40px;
  border-radius: 12px;
}

.tool-btn--text {
  padding: 0 14px;
  border: 1px solid rgba(194, 210, 227, 0.88);
  background: #fff;
  color: #21415f;
}

.chat-input__editor {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.chat-input__editor :deep(.el-textarea) {
  flex: 1;
}

.send-btn {
  min-height: 48px;
  padding: 0 18px;
  border-radius: 14px;
}

:deep(.mode-switcher.el-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.mode-switcher .el-radio-button__inner) {
  border-left: 1px solid var(--el-border-color) !important;
}

:deep(.el-radio-button__inner) {
  min-height: 40px;
  padding: 0 16px;
  line-height: 38px;
  border-radius: 12px !important;
}

:deep(.el-select__wrapper),
:deep(.el-textarea__inner) {
  border-radius: 14px;
  box-shadow: 0 0 0 1px rgba(28, 73, 123, 0.08) inset;
}

:deep(.chat-topbar__right .el-select__wrapper) {
  min-height: 44px;
}

:deep(.el-textarea__inner) {
  min-height: 92px !important;
  padding-top: 14px;
}

@media (max-width: 1100px) {
  .chat-container {
    grid-template-columns: 1fr;
    height: auto;
    min-height: 100%;
  }

  .chat-topbar {
    flex-direction: column;
  }

  .chat-topbar__right {
    width: 100%;
    min-width: 0;
  }

  .chat-sidebar {
    max-height: 320px;
  }
}

@media (max-width: 640px) {
  .chat-sidebar,
  .chat-main {
    border-radius: 20px;
  }

  .sidebar-header,
  .chat-topbar,
  .messages-area,
  .chat-input {
    padding-left: 16px;
    padding-right: 16px;
  }

  .chat-input__editor {
    flex-direction: column;
    align-items: stretch;
  }

  .send-btn {
    width: 100%;
  }
}
</style>
