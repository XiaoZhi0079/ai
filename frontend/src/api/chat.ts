import request from './request'
import type { ChatEntity, MessageVO } from '@/types'

export function sendChat(data: ChatEntity): Promise<string> {
  return request.post('/ai/chat', data)
}

export function getHistoryTypes(type: string): Promise<string[]> {
  return request.get(`/ai/history/type/${type}`)
}

export function getChatHistory(chatId: string): Promise<MessageVO[]> {
  return request.get(`/ai/history/chat/${chatId}`)
}

export function generateImage(prompt: string): Promise<string> {
  return request.get('/image/chat', { params: { prompt } })
}
