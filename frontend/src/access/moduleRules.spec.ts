import assert from 'node:assert/strict'
import { getCrudPermissions, getCrudTableFlags, getModuleTitle } from './moduleRules.ts'

assert.equal(getModuleTitle('teachers', 'ADMIN'), '教师管理')
assert.equal(getModuleTitle('teachers', 'TEACHER'), '教师信息')
assert.equal(getModuleTitle('students', 'TEACHER'), '学生管理')
assert.equal(getModuleTitle('students', 'STUDENT'), '学生信息')
assert.equal(getModuleTitle('courses', 'ADMIN'), '课程管理')
assert.equal(getModuleTitle('courses', 'STUDENT'), '课程信息')
assert.equal(getModuleTitle('grades', 'TEACHER'), '成绩管理')
assert.equal(getModuleTitle('grades', 'STUDENT'), '成绩信息')

assert.deepEqual(getCrudPermissions('teachers', 'ADMIN'), {
  canCreate: true,
  canEdit: true,
  canDelete: true,
  isReadOnly: false
})
assert.deepEqual(getCrudPermissions('teachers', 'TEACHER'), {
  canCreate: false,
  canEdit: true,
  canDelete: false,
  isReadOnly: false
})
assert.deepEqual(getCrudPermissions('teachers', 'STUDENT'), {
  canCreate: false,
  canEdit: false,
  canDelete: false,
  isReadOnly: true
})
assert.deepEqual(getCrudPermissions('students', 'TEACHER'), {
  canCreate: true,
  canEdit: true,
  canDelete: true,
  isReadOnly: false
})
assert.deepEqual(getCrudPermissions('students', 'STUDENT'), {
  canCreate: false,
  canEdit: false,
  canDelete: false,
  isReadOnly: true
})
assert.deepEqual(getCrudPermissions('courses', 'TEACHER'), {
  canCreate: true,
  canEdit: true,
  canDelete: true,
  isReadOnly: false
})
assert.deepEqual(getCrudPermissions('grades', 'STUDENT'), {
  canCreate: false,
  canEdit: false,
  canDelete: false,
  isReadOnly: true
})

assert.deepEqual(getCrudTableFlags('users', 'ADMIN'), {
  readonly: false,
  allowCreate: true,
  allowEdit: true,
  allowDelete: true
})
