import request from './request'

export function uploadRagDocument(file: File): Promise<any> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/rag/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getRagDocuments(): Promise<any[]> {
  return request.get('/rag/documents')
}

export function deleteRagDocument(id: number): Promise<any> {
  return request.delete(`/rag/documents/${id}`)
}
