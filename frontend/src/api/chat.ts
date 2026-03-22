import request from './request'
import type { ChatEntity, MessageVO, ModelOption, ImagesResponse, ConversationItem } from '@/types'

export function sendChat(data: ChatEntity): Promise<string> {
  return request.post('/ai/chat', data)
}

export function getHistoryList(): Promise<ConversationItem[]> {
  return request.get('/ai/history')
}

export function getChatHistory(chatId: string): Promise<MessageVO[]> {
  return request.get(`/ai/history/chat/${chatId}`)
}

export function generateImage(prompt: string, chatId?: string): Promise<ImagesResponse> {
  return request.get('/image/chat', { params: { prompt, chatId } })
}

export function getModelOptions(): Promise<ModelOption[]> {
  return request.get('/ai/models')
}
