import request from './request'
import type { ImagesResponse } from '@/types'

export function uploadImages(files: File[]): Promise<ImagesResponse[]> {
  const formData = new FormData()
  files.forEach((f) => formData.append('files', f))
  return request.post('/upload/images', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
