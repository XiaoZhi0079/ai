<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="login-title">校园知识问答助手</h2>
      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" @keyup.enter="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" prefix-icon="User" placeholder="用户名" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" prefix-icon="Lock" type="password" placeholder="密码" show-password />
            </el-form-item>
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
                <el-option label="管理员" value="ADMIN" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="login-btn" :loading="loading" @click="handleRegister">注 册</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('login')
const loading = ref(false)
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', email: '', role: 'STUDENT' as const })

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 4, message: '密码至少4位', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email' as const, message: '邮箱格式不正确', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

async function handleLogin() {
  await loginFormRef.value?.validate()
  loading.value = true
  try {
    await userStore.login(loginForm)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  await registerFormRef.value?.validate()
  loading.value = true
  try {
    await userStore.register(registerForm)
    ElMessage.success('注册成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.login-title {
  text-align: center;
  margin-bottom: 24px;
  color: #303133;
  font-size: 24px;
}
.login-btn {
  width: 100%;
}
</style>
