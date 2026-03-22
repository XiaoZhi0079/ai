import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '',
  timeout: 60000
})

request.interceptors.request.use((config) => {
  try {
    const auth = localStorage.getItem('user_auth')
    if (auth) {
      const { token } = JSON.parse(auth)
      if (token) config.headers['token'] = token
    }
  } catch {
    localStorage.removeItem('user_auth')
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const data = response.data
    // LeeResult wrapper has code field
    if (data !== null && typeof data === 'object' && 'code' in data) {
      if (data.code === 0) {
        return data.data
      }
      if (data.code === 401) {
        localStorage.removeItem('user_auth')
        router.push('/login')
      }
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    // Raw response (e.g. /ai/chat returns plain string)
    return data
  },
  (error) => {
    const resp = error.response
    if (resp) {
      if (resp.status === 401) {
        localStorage.removeItem('user_auth')
        ElMessage.warning('登录已过期，请重新登录')
        router.push('/login')
        return Promise.reject(error)
      }
      const data = resp.data
      // Backend may return LeeResult even on 500
      if (data && typeof data === 'object' && data.message) {
        ElMessage.error(data.message)
      } else if (typeof data === 'string' && data) {
        ElMessage.error(data.substring(0, 200))
      } else {
        ElMessage.error(`请求失败 (${resp.status})`)
      }
    } else {
      ElMessage.error('无法连接到服务器，请确认后端已启动')
    }
    return Promise.reject(error)
  }
)

export default request
