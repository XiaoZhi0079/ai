import request from './request'
import type { Role } from '@/types'

export interface UserOption {
  id: number
  username: string
  role: Role
}

export function getUserOptions(role?: Role): Promise<UserOption[]> {
  return request.get('/api/users/options', {
    params: role ? { role } : undefined
  })
}
