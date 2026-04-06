<template>
  <div class="login-page">
    <div class="login-page__backdrop"></div>
    <div class="login-shell">
      <section class="login-showcase">
        <div class="showcase-badge">Campus AI Platform</div>
        <h1 class="showcase-title">校园知识问答助手</h1>
        <p class="showcase-subtitle">
          面向校园场景的智能问答入口，整合知识检索、文档理解与教务数据查询能力。
        </p>

        <div class="showcase-highlight">
          <span class="showcase-highlight__label">统一智能入口</span>
          <p>
            为学生、教师与管理场景提供稳定、高效、易理解的 AI 交互体验。
          </p>
        </div>

        <div class="showcase-metrics">
          <div class="metric-card">
            <span class="metric-card__label">智能能力</span>
            <strong>知识问答</strong>
            <p>面向校园文档与常见问题提供自然语言问答支持。</p>
          </div>
          <div class="metric-card">
            <span class="metric-card__label">检索能力</span>
            <strong>网络检索</strong>
            <p>支持结合联网信息进行问题分析与结果补充，扩展校园场景之外的知识获取能力。</p>
          </div>
          <div class="metric-card">
            <span class="metric-card__label">数据能力</span>
            <strong>教务查询</strong>
            <p>通过自然语言快速访问课程、成绩、教师与学生信息。</p>
          </div>
        </div>
      </section>

      <section class="login-panel">
        <div class="auth-card">
          <div class="auth-card__header">
            <span class="auth-card__eyebrow">Welcome Back</span>
            <h2>登录校园智能平台</h2>
            <p>请使用账号登录，或完成注册后进入系统工作台。</p>
          </div>

          <el-tabs v-model="activeTab" class="login-tabs">
            <el-tab-pane label="登录" name="login">
              <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" @keyup.enter="handleLogin">
                <el-form-item prop="username">
                  <el-input v-model="loginForm.username" prefix-icon="User" placeholder="用户名" />
                </el-form-item>
                <el-form-item prop="password">
                  <el-input v-model="loginForm.password" prefix-icon="Lock" type="password" placeholder="密码" show-password />
                </el-form-item>
                <p class="form-hint">请输入已注册账号信息以访问校园智能服务。</p>
                <el-form-item>
                  <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">登 录</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="注册" name="register">
              <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" @keyup.enter="handleRegister">
                <el-form-item prop="username">
                  <el-input v-model="registerForm.username" prefix-icon="User" placeholder="用户名" />
                </el-form-item>
                <el-form-item prop="password">
                  <el-input v-model="registerForm.password" prefix-icon="Lock" type="password" placeholder="密码" show-password />
                </el-form-item>
                <el-form-item prop="email">
                  <el-input v-model="registerForm.email" prefix-icon="Message" placeholder="邮箱" />
                </el-form-item>
                <el-form-item prop="role">
                  <el-select v-model="registerForm.role" placeholder="选择角色" style="width: 100%">
                    <el-option label="学生" value="STUDENT" />
                    <el-option label="教师" value="TEACHER" />
                  </el-select>
                </el-form-item>
                <el-form-item v-if="registerForm.role === 'TEACHER'" prop="registrationKey">
                  <el-input v-model="registerForm.registrationKey" prefix-icon="Key" placeholder="注册密钥" />
                </el-form-item>
                <p class="form-hint">教师账号注册需提供注册密钥，学生账号可直接创建。</p>
                <el-form-item>
                  <el-button type="primary" class="login-btn" :loading="loading" @click="handleRegister">注 册</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { Role } from '@/types'

const router = useRouter()
const userStore = useUserStore()
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

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 4, message: '密码至少4位', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email' as const, message: '邮箱格式不正确', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  registrationKey: [{ required: true, message: '请输入注册密钥', trigger: 'blur' }]
}

async function handleLogin() {
  try {
    await loginFormRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await userStore.login(loginForm)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch {
    // 错误已由全局拦截器处理
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  try {
    await registerFormRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await userStore.register(registerForm)
    ElMessage.success('注册成功')
    router.push('/dashboard')
  } catch {
    // 错误已由全局拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100dvh;
  overflow: hidden;
  padding: 32px;
  background:
    radial-gradient(circle at top left, rgba(87, 163, 255, 0.24), transparent 28%),
    radial-gradient(circle at 85% 18%, rgba(91, 224, 255, 0.18), transparent 22%),
    linear-gradient(135deg, #08172d 0%, #0f2748 45%, #0b5561 100%);
}

.login-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 18% 24%, rgba(74, 163, 255, 0.26), transparent 0, transparent 32%),
    radial-gradient(circle at 74% 18%, rgba(74, 222, 255, 0.14), transparent 0, transparent 26%);
  pointer-events: none;
}

.login-page__backdrop {
  position: absolute;
  inset: 0;
  opacity: 0.3;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 32px 32px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.7), transparent 90%);
  pointer-events: none;
}

.login-shell {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 460px);
  align-items: center;
  gap: 32px;
  width: min(1200px, 100%);
  min-height: calc(100dvh - 64px);
  margin: 0 auto;
}

.login-showcase {
  color: #ecf4ff;
  padding: 24px 12px 24px 0;
}

.showcase-badge {
  display: inline-flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 8px 14px;
  border: 1px solid rgba(188, 225, 255, 0.24);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: #d9efff;
  font-size: 13px;
  letter-spacing: 0.08em;
  backdrop-filter: blur(10px);
}

.showcase-title {
  margin: 0;
  font-size: clamp(40px, 5vw, 60px);
  font-weight: 700;
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.showcase-subtitle {
  max-width: 620px;
  margin: 22px 0 0;
  color: rgba(236, 244, 255, 0.8);
  font-size: 17px;
  line-height: 1.8;
}

.showcase-highlight {
  max-width: 560px;
  margin-top: 32px;
  padding: 20px 22px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.05));
  box-shadow: 0 20px 50px rgba(4, 14, 34, 0.18);
  backdrop-filter: blur(10px);
}

.showcase-highlight__label {
  display: inline-block;
  margin-bottom: 10px;
  color: #79d8f6;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.showcase-highlight p {
  margin: 0;
  color: rgba(236, 244, 255, 0.88);
  line-height: 1.75;
}

.showcase-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 28px;
}

.metric-card {
  min-height: 190px;
  padding: 22px 20px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.05));
  box-shadow: 0 24px 60px rgba(4, 14, 34, 0.18);
  backdrop-filter: blur(10px);
}

.metric-card__label {
  display: inline-block;
  margin-bottom: 12px;
  color: #8edff7;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.metric-card strong {
  display: block;
  margin-bottom: 10px;
  color: #ffffff;
  font-size: 22px;
  font-weight: 700;
}

.metric-card p {
  margin: 0;
  color: rgba(236, 244, 255, 0.78);
  font-size: 14px;
  line-height: 1.7;
}

.login-panel {
  display: flex;
  justify-content: flex-end;
}

.auth-card {
  width: 100%;
  padding: 30px;
  border: 1px solid rgba(186, 208, 235, 0.7);
  border-radius: 28px;
  background: rgba(247, 251, 255, 0.96);
  box-shadow: 0 24px 80px rgba(3, 16, 37, 0.28);
}

.auth-card__header {
  margin-bottom: 24px;
}

.auth-card__eyebrow {
  display: inline-block;
  margin-bottom: 10px;
  color: #1a7fa0;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.auth-card__header h2 {
  margin: 0;
  color: #14243a;
  font-size: 30px;
  line-height: 1.2;
}

.auth-card__header p {
  margin: 12px 0 0;
  color: #5e6d82;
  font-size: 14px;
  line-height: 1.7;
}

.form-hint {
  margin: -4px 0 18px;
  color: #708198;
  font-size: 13px;
  line-height: 1.6;
}

.login-btn {
  width: 100%;
}

:deep(.el-tabs__header) {
  margin-bottom: 24px;
}

:deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: rgba(31, 73, 123, 0.1);
}

:deep(.el-tabs__item) {
  height: 42px;
  color: #60738c;
  font-size: 15px;
  font-weight: 600;
}

:deep(.el-tabs__item.is-active) {
  color: #0f658f;
}

:deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, #1271b9, #19a6b6);
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  min-height: 48px;
  border-radius: 14px;
  box-shadow: 0 0 0 1px rgba(28, 73, 123, 0.08) inset;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px rgba(21, 124, 190, 0.35) inset, 0 0 0 4px rgba(74, 163, 255, 0.14);
}

:deep(.el-input__prefix-inner),
:deep(.el-input__icon),
:deep(.el-select__caret) {
  color: #6c87a4;
}

:deep(.el-button--primary) {
  min-height: 48px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #1065ab 0%, #0f86a7 100%);
  box-shadow: 0 18px 36px rgba(16, 101, 171, 0.22);
  font-size: 15px;
  font-weight: 600;
}

:deep(.el-button--primary:hover) {
  transform: translateY(-1px);
  box-shadow: 0 20px 40px rgba(16, 101, 171, 0.28);
}

:deep(.el-button--primary:active) {
  transform: scale(0.98);
}

@media (max-width: 1100px) {
  .login-shell {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .login-showcase {
    padding: 12px 0 0;
  }

  .login-panel {
    justify-content: flex-start;
  }

  .auth-card {
    max-width: 520px;
  }

  .showcase-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .login-page {
    padding: 20px 16px;
  }

  .login-shell {
    min-height: auto;
  }

  .login-showcase {
    order: 1;
  }

  .login-panel {
    order: 2;
  }

  .showcase-subtitle {
    font-size: 15px;
  }

  .showcase-metrics {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 18px 14px;
  }

  .showcase-title {
    font-size: 32px;
  }

  .showcase-highlight,
  .metric-card,
  .auth-card {
    border-radius: 20px;
  }

  .auth-card {
    padding: 22px 18px;
  }

  .auth-card__header h2 {
    font-size: 24px;
  }
}
</style>
