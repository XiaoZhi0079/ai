import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi } from '@/api/auth'
import type { LoginRequest, RegisterRequest, Role } from '@/types'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const id = ref<number | null>(null)
  const username = ref('')
  const role = ref<Role | ''>('')
  const email = ref('')
  const token = ref('')

  const isLoggedIn = computed(() => token.value !== '')
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isTeacher = computed(() => role.value === 'TEACHER')

  function persist() {
    localStorage.setItem('user_auth', JSON.stringify({
      id: id.value, username: username.value, role: role.value,
      email: email.value, token: token.value
    }))
  }

  function restore() {
    const raw = localStorage.getItem('user_auth')
    if (raw) {
      const data = JSON.parse(raw)
      id.value = data.id
      username.value = data.username
      role.value = data.role
      email.value = data.email
      token.value = data.token
    }
  }

  async function login(req: LoginRequest) {
    const res = await loginApi(req)
    id.value = res.id
    username.value = res.username
    role.value = res.role
    email.value = res.email
    token.value = res.token
    persist()
  }

  async function register(req: RegisterRequest) {
    const res = await registerApi(req)
    id.value = res.id
    username.value = res.username
    role.value = res.role
    email.value = res.email
    token.value = res.token || ''
    persist()
  }

  function logout() {
    id.value = null
    username.value = ''
    role.value = ''
    email.value = ''
    token.value = ''
    localStorage.removeItem('user_auth')
    router.push('/login')
  }

  return { id, username, role, email, token, isLoggedIn, isAdmin, isTeacher, login, register, logout, restore }
})
