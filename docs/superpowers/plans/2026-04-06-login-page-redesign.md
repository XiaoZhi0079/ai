# Login Page Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the login page into an academic-tech dual-column experience while preserving existing login and registration behavior.

**Architecture:** Keep all business logic in `frontend/src/views/login/LoginView.vue`, replacing the current single-card structure with a responsive two-column layout. Preserve the existing form models, validation, and submit handlers while upgrading template structure and scoped styles only.

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vite, scoped CSS

---

## File Map

- Modify: `frontend/src/views/login/LoginView.vue` — restructure template, keep script logic intact, replace scoped styles with the new academic-tech visual system
- Verify: `frontend/package.json` — confirm build command remains `npm run build`

### Task 1: Baseline review and safety check

**Files:**
- Modify: `frontend/src/views/login/LoginView.vue`
- Verify: `frontend/package.json`

- [ ] **Step 1: Confirm the current component owns both layout and styles**

Read `frontend/src/views/login/LoginView.vue` and verify that login/register logic, template, and scoped CSS are contained in this single file.

- [ ] **Step 2: Confirm no extra dependency is needed**

Read `frontend/package.json` and verify the redesign can be implemented with existing `vue`, `element-plus`, and scoped CSS only.

- [ ] **Step 3: Keep the script contract unchanged**

Preserve these script symbols and behaviors during implementation:

```ts
const activeTab = ref('login')
const loading = ref(false)
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive<{ username: string; password: string; email: string; role: Role; registrationKey: string }>({
  username: '',
  password: '',
  email: '',
  role: 'STUDENT',
  registrationKey: ''
})
```

### Task 2: Replace template with dual-column structure

**Files:**
- Modify: `frontend/src/views/login/LoginView.vue`

- [ ] **Step 1: Replace the root layout markup**

Rewrite the template so the page uses a shell with a decorative background and a responsive content grid:

```vue
<div class="login-page">
  <div class="login-page__backdrop"></div>
  <div class="login-shell">
    <section class="login-showcase">
      <!-- brand content -->
    </section>
    <section class="login-panel">
      <!-- auth card -->
    </section>
  </div>
</div>
```

- [ ] **Step 2: Add left showcase content**

Include these content blocks in the showcase section:

```vue
<div class="showcase-badge">Campus AI Platform</div>
<h1 class="showcase-title">校园知识问答助手</h1>
<p class="showcase-subtitle">
  面向校园场景的智能问答入口，整合知识检索、文档理解与教务数据查询能力。
</p>
<div class="showcase-metrics">
  <div class="metric-card">
    <span class="metric-card__label">智能能力</span>
    <strong>知识问答</strong>
    <p>面向校园文档与常见问题提供自然语言问答支持。</p>
  </div>
  <div class="metric-card">
    <span class="metric-card__label">数据能力</span>
    <strong>教务查询</strong>
    <p>通过自然语言快速访问课程、成绩、教师与学生信息。</p>
  </div>
  <div class="metric-card">
    <span class="metric-card__label">统一入口</span>
    <strong>多角色访问</strong>
    <p>支持学生、教师等角色在统一入口完成身份访问与使用。</p>
  </div>
</div>
```

- [ ] **Step 3: Wrap existing forms in an auth card**

Place the existing `el-tabs`, login form, and register form inside a right-side card with supporting copy:

```vue
<div class="auth-card">
  <div class="auth-card__header">
    <span class="auth-card__eyebrow">Welcome Back</span>
    <h2>登录校园智能平台</h2>
    <p>请使用账号登录，或完成注册后进入系统工作台。</p>
  </div>
  <!-- existing tabs and forms -->
</div>
```

- [ ] **Step 4: Preserve all form controls and behavior**

Keep these controls unchanged in meaning and bindings:

```vue
<el-input v-model="loginForm.username" />
<el-input v-model="loginForm.password" type="password" show-password />
<el-input v-model="registerForm.username" />
<el-input v-model="registerForm.password" type="password" show-password />
<el-input v-model="registerForm.email" />
<el-select v-model="registerForm.role">
  <el-option label="学生" value="STUDENT" />
  <el-option label="教师" value="TEACHER" />
</el-select>
<el-input v-if="registerForm.role === 'TEACHER'" v-model="registerForm.registrationKey" />
```

### Task 3: Implement academic-tech visual system

**Files:**
- Modify: `frontend/src/views/login/LoginView.vue`

- [ ] **Step 1: Replace the old page-level CSS**

Remove the current styles for `.login-container`, `.login-card`, `.login-title`, and `.login-btn`, and introduce a new scoped CSS system built around these selectors:

```css
.login-page
.login-page__backdrop
.login-shell
.login-showcase
.login-panel
.auth-card
.showcase-badge
.showcase-title
.showcase-subtitle
.showcase-metrics
.metric-card
.login-tabs
.login-btn
```

- [ ] **Step 2: Use a restrained academic-tech palette**

Apply a palette centered on deep navy, blue-cyan, cool white, and blue-gray, for example:

```css
background: linear-gradient(135deg, #0b1f3a 0%, #12345b 45%, #0f6b78 100%);
color: #ecf4ff;
border: 1px solid rgba(255, 255, 255, 0.14);
box-shadow: 0 24px 80px rgba(3, 16, 37, 0.28);
```

- [ ] **Step 3: Add lightweight decorative layers**

Use pure CSS pseudo-elements or extra divs for soft glow and grid texture, similar to:

```css
.login-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 20%, rgba(74, 163, 255, 0.28), transparent 30%),
    radial-gradient(circle at 80% 15%, rgba(74, 222, 255, 0.18), transparent 24%);
}

.login-page__backdrop {
  background-image:
    linear-gradient(rgba(255,255,255,0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.05) 1px, transparent 1px);
  background-size: 32px 32px;
}
```

- [ ] **Step 4: Refine form component presentation locally**

Use scoped `:deep()` selectors only inside this view to style Element Plus subparts that must visually match the page:

```css
:deep(.el-tabs__item) { font-weight: 600; }
:deep(.el-input__wrapper) { min-height: 46px; border-radius: 14px; }
:deep(.el-select__wrapper) { min-height: 46px; border-radius: 14px; }
:deep(.el-button--primary) { min-height: 46px; border-radius: 14px; }
```

### Task 4: Make layout responsive and readable

**Files:**
- Modify: `frontend/src/views/login/LoginView.vue`

- [ ] **Step 1: Add desktop-first two-column sizing**

Use grid sizing that gives the showcase more room than the auth card:

```css
.login-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 460px);
  gap: 32px;
}
```

- [ ] **Step 2: Add tablet breakpoint**

Collapse to one column around tablet widths and reduce spacing:

```css
@media (max-width: 1100px) {
  .login-shell {
    grid-template-columns: 1fr;
  }
}
```

- [ ] **Step 3: Add mobile breakpoint**

Reduce padding, compact cards, and simplify showcase spacing for small screens:

```css
@media (max-width: 640px) {
  .login-page {
    padding: 20px 14px;
  }

  .auth-card,
  .metric-card {
    border-radius: 20px;
  }

  .showcase-title {
    font-size: 32px;
  }
}
```

- [ ] **Step 4: Ensure action priority on small screens**

Keep the auth card visible early in the viewport by ordering the login panel before or immediately after the compacted showcase content in responsive CSS if needed.

### Task 5: Verify the redesign

**Files:**
- Verify: `frontend/src/views/login/LoginView.vue`
- Verify: `frontend/package.json`

- [ ] **Step 1: Run the frontend build**

Run: `npm run build`
Expected: Vite production build completes successfully with no TypeScript errors.

- [ ] **Step 2: Sanity-check preserved behavior in code**

Verify these conditions still exist in `frontend/src/views/login/LoginView.vue`:

```ts
await userStore.login(loginForm)
await userStore.register(registerForm)
router.push('/dashboard')
registerForm.role === 'TEACHER'
```

- [ ] **Step 3: Review against the approved spec**

Confirm the final component satisfies these approved outcomes:

```md
- 双栏门户型布局
- 学术科技风配色
- 左侧品牌展示与能力点
- 右侧登录/注册悬浮卡片
- 保留全部原有认证行为
- 中小屏幕下可正常使用
```
