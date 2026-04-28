import type { Role } from '@/types'

export type ManagedModuleKey = 'users' | 'teachers' | 'students' | 'courses' | 'grades'

export interface CrudPermissions {
  canCreate: boolean
  canEdit: boolean
  canDelete: boolean
  isReadOnly: boolean
}

export interface CrudTableFlags {
  readonly: boolean
  allowCreate: boolean
  allowEdit: boolean
  allowDelete: boolean
}

const CRUD_PERMISSIONS: Record<ManagedModuleKey, Record<Role, CrudPermissions>> = {
  users: {
    ADMIN: { canCreate: true, canEdit: true, canDelete: true, isReadOnly: false },
    TEACHER: { canCreate: false, canEdit: false, canDelete: false, isReadOnly: true },
    STUDENT: { canCreate: false, canEdit: false, canDelete: false, isReadOnly: true }
  },
  teachers: {
    ADMIN: { canCreate: true, canEdit: true, canDelete: true, isReadOnly: false },
    TEACHER: { canCreate: false, canEdit: true, canDelete: false, isReadOnly: false },
    STUDENT: { canCreate: false, canEdit: false, canDelete: false, isReadOnly: true }
  },
  students: {
    ADMIN: { canCreate: true, canEdit: true, canDelete: true, isReadOnly: false },
    TEACHER: { canCreate: true, canEdit: true, canDelete: true, isReadOnly: false },
    STUDENT: { canCreate: false, canEdit: false, canDelete: false, isReadOnly: true }
  },
  courses: {
    ADMIN: { canCreate: true, canEdit: true, canDelete: true, isReadOnly: false },
    TEACHER: { canCreate: true, canEdit: true, canDelete: true, isReadOnly: false },
    STUDENT: { canCreate: false, canEdit: false, canDelete: false, isReadOnly: true }
  },
  grades: {
    ADMIN: { canCreate: true, canEdit: true, canDelete: true, isReadOnly: false },
    TEACHER: { canCreate: true, canEdit: true, canDelete: true, isReadOnly: false },
    STUDENT: { canCreate: false, canEdit: false, canDelete: false, isReadOnly: true }
  }
}

export function getCrudPermissions(moduleKey: ManagedModuleKey, role: Role): CrudPermissions {
  return CRUD_PERMISSIONS[moduleKey][role]
}

export function getCrudTableFlags(moduleKey: ManagedModuleKey, role: Role): CrudTableFlags {
  const permissions = getCrudPermissions(moduleKey, role)
  return {
    readonly: permissions.isReadOnly,
    allowCreate: permissions.canCreate,
    allowEdit: permissions.canEdit,
    allowDelete: permissions.canDelete
  }
}

export function getModuleTitle(moduleKey: ManagedModuleKey, role: Role): string {
  switch (moduleKey) {
    case 'users':
      return '用户管理'
    case 'teachers':
      return role === 'ADMIN' ? '教师管理' : '教师信息'
    case 'students':
      return role === 'ADMIN' || role === 'TEACHER' ? '学生管理' : '学生信息'
    case 'courses':
      return role === 'ADMIN' || role === 'TEACHER' ? '课程管理' : '课程信息'
    case 'grades':
      return role === 'ADMIN' || role === 'TEACHER' ? '成绩管理' : '成绩信息'
  }
}
