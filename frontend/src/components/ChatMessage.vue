<template>
  <div :class="['chat-message', msg.role === 'USER' ? 'user' : 'assistant']">
    <div class="avatar">
      <el-icon v-if="msg.role === 'USER'" size="20"><User /></el-icon>
      <el-icon v-else size="20"><Monitor /></el-icon>
    </div>
    <div class="bubble">
      <template v-if="msg.kind === 'data-query' && msg.dataQuery">
        <div class="query-summary">{{ msg.content }}</div>
        <div class="query-section">
          <div class="query-title">SQL</div>
          <pre class="query-sql"><code>{{ msg.dataQuery.sql }}</code></pre>
        </div>
        <div class="query-section">
          <div class="query-title">查询结果</div>
          <el-empty v-if="!msg.dataQuery.rows.length" description="没有匹配结果" :image-size="60" />
          <el-table v-else :data="normalizedRows" size="small" border style="width: 100%">
            <el-table-column
              v-for="column in queryColumns"
              :key="column"
              :prop="column"
              :label="column"
              min-width="120"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                {{ formatCell(row[column]) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>
      <div v-else v-html="renderedContent"></div>
      <div v-if="msg.images && msg.images.length" class="message-images">
        <img v-for="(url, i) in msg.images" :key="i" :src="url" alt="image" />
      </div>
    </div>
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

const normalizedRows = computed(() => props.msg.dataQuery?.rows ?? [])

const queryColumns = computed(() => {
  const firstRow = normalizedRows.value[0]
  return firstRow ? Object.keys(firstRow) : []
})

function formatCell(value: unknown) {
  if (value == null) return ''
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}
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
.query-summary { margin-bottom: 10px; }
.query-section + .query-section { margin-top: 12px; }
.query-title { margin-bottom: 6px; font-weight: 600; }
.query-sql { background: #1e1e1e; color: #d4d4d4; padding: 12px; border-radius: 6px; overflow-x: auto; margin: 0; }
.query-sql code { font-family: 'Consolas', monospace; font-size: 13px; }
.message-images { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.message-images img { width: 120px; height: 120px; object-fit: cover; border-radius: 6px; border: 1px solid #e4e7ed; }
</style>
