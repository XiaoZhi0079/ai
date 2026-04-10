import request from './request'
import type { AiSqlQueryResult, ChatEntity, MessageVO, ModelOption, ImagesResponse, ConversationItem } from '@/types'

export function sendChat(data: ChatEntity): Promise<string> {
  return request.post('/ai/chat', data)
}

export async function streamChat(
  data: ChatEntity,
  handlers: {
    onStart?: (chatId: string) => void
    onChunk: (chunk: string) => void
    onDone?: () => void
    onError?: (message: string) => void
  }
): Promise<void> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream'
  }

  try {
    const auth = localStorage.getItem('user_auth')
    if (auth) {
      const { token } = JSON.parse(auth)
      if (token) headers.token = token
    }
  } catch {
    localStorage.removeItem('user_auth')
  }

  const response = await fetch('/ai/chat/stream', {
    method: 'POST',
    headers,
    body: JSON.stringify(data)
  })

  if (!response.ok || !response.body) {
    const message = await response.text()
    throw new Error(message || `Request failed (${response.status})`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const processEventBlock = (event: string) => {
    if (!event.trim()) {
      return
    }

    const lines = event.split(/\r?\n/)
    const eventNames = lines
      .filter((line) => line.startsWith('event:'))
      .map((line) => line.slice(6).trim())
    const eventName = eventNames.length > 0 ? eventNames[eventNames.length - 1] : 'message'
    const dataLines = lines
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.slice(5).trimStart())
    const payload = dataLines.join('\n')

    if (eventName === 'start') {
      handlers.onStart?.(payload)
    } else if (eventName === 'chunk') {
      handlers.onChunk(payload)
    } else if (eventName === 'done') {
      handlers.onDone?.()
    } else if (eventName === 'error') {
      handlers.onError?.(payload)
      throw new Error(payload || 'Stream failed')
    } else if (dataLines.length > 0) {
      handlers.onChunk(payload)
    }
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }

    buffer += decoder.decode(value, { stream: true })
    const events = buffer.split(/\r?\n\r?\n/)
    buffer = events.pop() || ''

    for (const event of events) {
      processEventBlock(event)
    }
  }

  const tail = decoder.decode()
  if (tail) {
    buffer += tail
  }

  const remainingEvents = buffer.split(/\r?\n\r?\n/)
  for (const event of remainingEvents) {
    processEventBlock(event)
  }
}

export function getHistoryList(): Promise<ConversationItem[]> {
  return request.get('/ai/history')
}

export function getChatHistory(chatId: string): Promise<MessageVO[]> {
  return request.get(`/ai/history/chat/${chatId}`)
}

export function deleteChatHistory(chatId: string): Promise<void> {
  return request.delete(`/ai/history/chat/${chatId}`)
}

export function generateImage(prompt: string, chatId?: string): Promise<ImagesResponse> {
  return request.get('/image/chat', { params: { prompt, chatId } })
}

export function getModelOptions(): Promise<ModelOption[]> {
  return request.get('/ai/models')
}

export function queryData(question: string, model?: string): Promise<AiSqlQueryResult> {
  return request.post('/ai/data-query', { question, model })
}
