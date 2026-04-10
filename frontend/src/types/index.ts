export type Role = 'ADMIN' | 'TEACHER' | 'STUDENT'
export type ChatMode = 'DIRECT' | 'KNOWLEDGE_BASE' | 'INTERNET_SEARCH' | 'DATA_QUERY'

export interface LeeResult<T> {
  code: number
  message: string
  data: T
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email: string
  role?: Role
  registrationKey?: string
}

export interface AuthResponse {
  id: number
  username: string
  role: Role
  email: string
  status: number
  token: string
}

export interface ChatEntity {
  userName: string
  chatId: string
  userInput: string
  chatMode: ChatMode
  model: string
  imageFiles?: ImagesResponse[] | null
}

export interface MessageVO {
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  images?: string[]
  kind?: 'chat' | 'data-query'
  dataQuery?: AiSqlQueryResult
}

export interface AiSqlQueryResult {
  sql: string
  rowCount: number
  rows: Array<Record<string, unknown>>
}

export interface ImagesResponse {
  imageUrl: string
  previewUrl: string
  mimeType: string
}

export interface ModelOption {
  value: string
  label: string
  platform?: string
}

export interface ConversationItem {
  chatId: string
  title: string | null
  localOnly?: boolean
}

export interface UserView {
  id: number
  username: string
  role: Role
  email: string
  status: number
  createdTime: string
  updatedTime: string
}

export interface UserForm {
  username: string
  password?: string
  role: Role
  email: string
  status: number
}

export interface Teacher {
  id?: number
  userId?: number
  name: string
  gender: string
  phone: string
  department: string
  title: string
  researchField: string
  officeAddress: string
  createdAt?: string
  updatedAt?: string
}

export interface Student {
  id?: number
  userId?: number
  name: string
  gender: string
  grade: number
  major: string
  className: string
  dormitory: string
  guardianPhone: string
  createdAt?: string
  updatedAt?: string
}

export interface Course {
  id?: number
  courseName: string
  teacherId: number
  credit: number
  beginDate: string
  endDate: string
  schedule: string
  description: string
  createdAt?: string
  updatedAt?: string
}

export interface Grade {
  id?: number
  studentId: number
  courseId: number
  score: number
  semester: number
  createdAt?: string
  updatedAt?: string
}

export interface OperationLog {
  id: number
  operator: string
  action: string
  createdAt: string
}
