import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

// Module-level variable to hold the token
let authToken: string | null = null

/**
 * Sets the authentication token for all subsequent API requests.
 * This function is called by the auth store to update the token.
 * @param {string | null} token - The new JWT token, or null to clear it.
 */
export function setRequestToken(token: string | null) {
  authToken = token
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
})

// Request interceptor for adding the auth token
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (authToken && config.headers) {
      config.headers['Authorization'] = `Bearer ${authToken}`
    }
    return config
  },
  (error: AxiosError) => {
    console.error('Request Error:', error)
    return Promise.reject(error)
  }
)

// Response interceptor for handling responses globally
service.interceptors.response.use(
  response => {
    const res = response.data

    // Check if the response is a standard HttpResult wrapper
    if (typeof res === 'object' && res !== null && 'success' in res) {
      if (res.success) {
        // For successful wrapped responses, return the 'data' payload
        return res
      } else {
        // For failed wrapped responses, show a message and reject the promise
        ElMessage({
          message: res.message || 'Error',
          type: 'error',
          duration: 5 * 1000,
        })
        return Promise.reject(new Error(res.message || 'Error'))
      }
    }

    // If it's not a wrapped response, return the data directly
    return res
  },
  (error: AxiosError) => {
    const { response } = error

    // Handle 401 Unauthorized errors
    if (response?.status === 401) {
      ElMessage.error('认证失败或令牌已过期，请重新登录。')
      // Clear the token from this module
      setRequestToken(null)
      // Reload the page to force a fresh state and redirect to the login page via the router guard.
      // This effectively logs the user out from the client-side.
      location.reload()
    } else {
      // For all other errors, just display a generic error message
      console.error('Response Error:', error)
      ElMessage({
        message: error.message || '服务器发生未知错误',
        type: 'error',
        duration: 5 * 1000,
      })
    }

    return Promise.reject(error)
  }
)

export default service
