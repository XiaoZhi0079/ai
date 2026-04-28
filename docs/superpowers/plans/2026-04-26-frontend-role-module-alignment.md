# Frontend Role Module Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make frontend module titles and CRUD buttons match the backend role permissions already implemented for users, teachers, students, courses, and grades.

**Architecture:** Extract role-to-module title and CRUD permission rules into a small pure TypeScript module so layout, dashboard, and CRUD pages consume a single source of truth. Keep page-level row restrictions local where backend is more specific than route-level permission, such as teachers editing only their own teacher record.

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vite, Node `--experimental-strip-types`

---

### Task 1: Add a pure permission/title rules module

**Files:**
- Create: `frontend/src/access/moduleRules.ts`
- Test: `frontend/src/access/moduleRules.spec.ts`

- [ ] **Step 1: Write the failing test**

```ts
import assert from 'node:assert/strict'
import { getCrudPermissions, getModuleTitle } from './moduleRules.ts'

assert.equal(getModuleTitle('teachers', 'ADMIN'), '教师管理')
assert.equal(getModuleTitle('teachers', 'TEACHER'), '教师信息')
assert.equal(getModuleTitle('students', 'TEACHER'), '学生管理')
assert.equal(getModuleTitle('students', 'STUDENT'), '学生信息')
assert.deepEqual(getCrudPermissions('grades', 'TEACHER'), {
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node --experimental-strip-types src/access/moduleRules.spec.ts`
Expected: FAIL because `moduleRules.ts` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

```ts
export function getModuleTitle(moduleKey, role) {
  // return 管理 or 信息 based on role and module
}

export function getCrudPermissions(moduleKey, role) {
  // return canCreate/canEdit/canDelete/isReadOnly
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `node --experimental-strip-types src/access/moduleRules.spec.ts`
Expected: PASS with no output.

### Task 2: Apply rules to layout and dashboard titles

**Files:**
- Modify: `frontend/src/layouts/MainLayout.vue`
- Modify: `frontend/src/views/dashboard/DashboardView.vue`

- [ ] **Step 1: Replace hard-coded managed module titles**

Use `getModuleTitle(...)` for `/teachers`, `/students`, `/courses`, `/grades` in sidebar menu and current page title resolution.

- [ ] **Step 2: Update dashboard cards to use role-appropriate labels**

Use the same rule module so cards say `学生管理/课程管理/成绩管理` for roles with management rights and `学生信息/课程信息/成绩信息` otherwise.

### Task 3: Make CRUD pages declare backend-aligned permissions explicitly

**Files:**
- Modify: `frontend/src/views/teacher/TeacherView.vue`
- Modify: `frontend/src/views/student/StudentView.vue`
- Modify: `frontend/src/views/course/CourseView.vue`
- Modify: `frontend/src/views/grade/GradeView.vue`

- [ ] **Step 1: Wire module permissions into each CRUD page**

Use `getCrudPermissions(...)` to set `readonly`, `allow-create`, `allow-edit`, and `allow-delete` explicitly instead of relying on implicit defaults.

- [ ] **Step 2: Preserve page-specific row restrictions**

Keep teacher page row-level logic so teachers can only edit their own record, matching backend behavior.

- [ ] **Step 3: Keep student role read-only where backend is read-only**

Do not expose edit/delete/create buttons for students in student, course, or grade pages unless backend already allows them.

### Task 4: Verify

**Files:**
- Verify: `frontend/src/access/moduleRules.spec.ts`
- Verify: `frontend/package.json`

- [ ] **Step 1: Run the new permission/title test**

Run: `node --experimental-strip-types src/access/moduleRules.spec.ts`
Expected: PASS.

- [ ] **Step 2: Run the frontend build**

Run: `npm run build`
Expected: TypeScript check and Vite build both PASS.
