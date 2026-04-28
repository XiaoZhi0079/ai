import request from './request'

export interface RagOcrConfig {
  baseUrl: string
  apiKey: string
  model: string
}

export interface RagParsePreview {
  fileName: string
  extractedText: string
  structuredText: string
  ocrText: string
  ocrUsed: boolean
  charCount: number
  knowledgeScope: 'PUBLIC' | 'PRIVATE'
}

export interface RagDocumentInfo {
  id: number
  fileName: string
  ossUrl: string
  uploadedBy?: number | null
  uploadedByName?: string
  ownerUserId?: number | null
  ownerUserName?: string
  knowledgeScope: 'PUBLIC' | 'PRIVATE'
  chunkCount?: number
  createdAt?: string
  updatedAt?: string
}

export interface RagDocumentDetail extends RagDocumentInfo {
  extractedText?: string
}

export function parseRagDocument(file: File, scope = 'PRIVATE', ocrConfig?: RagOcrConfig): Promise<RagParsePreview> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('scope', scope)
  if (ocrConfig?.baseUrl) formData.append('ocrBaseUrl', ocrConfig.baseUrl)
  if (ocrConfig?.apiKey) formData.append('ocrApiKey', ocrConfig.apiKey)
  if (ocrConfig?.model) formData.append('ocrModel', ocrConfig.model)
  return request.post('/rag/parse', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getRagOcrSettings(): Promise<RagOcrConfig> {
  return request.get('/rag/ocr-settings')
}

export function saveRagOcrSettings(config: RagOcrConfig): Promise<RagOcrConfig> {
  const formData = new FormData()
  formData.append('baseUrl', config.baseUrl || '')
  formData.append('apiKey', config.apiKey || '')
  formData.append('model', config.model || '')
  return request.post('/rag/ocr-settings', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function confirmRagDocument(file: File, text: string, scope = 'PRIVATE'): Promise<any> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('text', text)
  formData.append('scope', scope)
  return request.post('/rag/confirm', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadRagDocument(file: File, scope = 'PRIVATE'): Promise<any> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('scope', scope)
  return request.post('/rag/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getRagDocuments(): Promise<RagDocumentInfo[]> {
  return request.get('/rag/documents')
}

export function getRagDocumentDetail(id: number): Promise<RagDocumentDetail> {
  return request.get(`/rag/documents/${id}`)
}

export function renameRagDocument(id: number, fileName: string): Promise<any> {
  const formData = new FormData()
  formData.append('fileName', fileName)
  return request.post(`/rag/documents/${id}/rename`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function reOcrRagDocumentPreview(id: number): Promise<RagParsePreview> {
  return request.post(`/rag/documents/${id}/re-ocr-preview`)
}

export function reOcrRagDocumentApply(id: number, text: string): Promise<RagDocumentDetail> {
  const formData = new FormData()
  formData.append('text', text)
  return request.post(`/rag/documents/${id}/re-ocr-apply`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteRagDocument(id: number): Promise<any> {
  return request.delete(`/rag/documents/${id}`)
}
