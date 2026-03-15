<template>
  <div :class="['chat-message', msg.role === 'USER' ? 'user' : 'assistant']">
    <div class="avatar">
      <el-icon v-if="msg.role === 'USER'" size="20"><User /></el-icon>
      <el-icon v-else size="20"><Monitor /></el-icon>
    </div>
    <div class="bubble" v-html="renderedContent"></div>
  </div>
</template>

<script lang="ts">
import MarkdownIt from 'markdown-it'
// 模块级单例，所有 ChatMessage 组件共享一个实例
const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
</script>

<script setup lang="ts">
import { computed } from 'vue'
import type { MessageVO } from '@/types'

const props = defineProps<{ msg: MessageVO }>()

const renderedContent = computed(() => {
  if (props.msg.role === 'USER') return props.msg.content
  return md.render(props.msg.content || '')
})
</script>

<style scoped>
.chat-message { display: flex; gap: 12px; margin-bottom: 16px; max-width: 80%; }
.chat-message.user { flex-direction: row-reverse; margin-left: auto; }
.chat-message.assistant { margin-right: auto; }
.avatar {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.user .avatar { background: #409eff; color: #fff; }
.assistant .avatar { background: #67c23a; color: #fff; }
.bubble {
  padding: 10px 14px; border-radius: 10px; line-height: 1.6;
  font-size: 14px; word-break: break-word;
}
.user .bubble { background: #ecf5ff; color: #303133; }
.assistant .bubble { background: #f4f4f5; color: #303133; }
.bubble :deep(pre) { background: #1e1e1e; color: #d4d4d4; padding: 12px; border-radius: 6px; overflow-x: auto; }
.bubble :deep(code) { font-family: 'Consolas', monospace; font-size: 13px; }
.bubble :deep(p) { margin: 0 0 8px; }
.bubble :deep(p:last-child) { margin-bottom: 0; }
</style>
