import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// Shared axios instance for all frontend API modules.
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

    // Unwrap the common LeeResult envelope returned by most backend endpoints.
    if (data !== null && typeof data === 'object' && 'code' in data) {
      if (data.code === 0) {
        return data.data
      }
      if (data.code === 401) {
        localStorage.removeItem('user_auth')
        router.push('/login')
      }
      ElMessage.error(data.message || 'Request failed')
      return Promise.reject(new Error(data.message))
    }

    // Some endpoints return raw values, such as plain chat text.
    return data
  },
  (error) => {
    const resp = error.response
    if (resp) {
      if (resp.status === 401) {
        localStorage.removeItem('user_auth')
        ElMessage.warning('Login expired, please sign in again')
        router.push('/login')
        return Promise.reject(error)
      }

      const data = resp.data
      // Even when HTTP status is not 200, the backend may still return a structured error body.
      if (data && typeof data === 'object' && data.message) {
        ElMessage.error(data.message)
      } else if (typeof data === 'string' && data) {
        ElMessage.error(data.substring(0, 200))
      } else {
        ElMessage.error(`Request failed (${resp.status})`)
      }
    } else {
      ElMessage.error('Cannot reach the server, please verify the backend is running')
    }
    return Promise.reject(error)
  }
)

export default request
