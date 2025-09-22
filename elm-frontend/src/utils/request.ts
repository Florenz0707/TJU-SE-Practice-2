import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '../store/auth';
import { refreshToken as apiRefreshToken } from '../api/auth';

// Module-level variable to hold the token
let authToken: string | null = null;

/**
 * Sets the authentication token for all subsequent API requests.
 * This function is called by the auth store to update the token.
 * @param {string | null} token - The new JWT token, or null to clear it.
 */
export function setRequestToken(token: string | null) {
  authToken = token;
  if (token) {
    // Also update the default header for the original axios instance
    service.defaults.headers.common['Authorization'] = `Bearer ${token}`;
  } else {
    delete service.defaults.headers.common['Authorization'];
  }
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
});

// Request interceptor for adding the auth token
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // The token is now set on the default headers,
    // but we can also ensure it's here for any custom axios instances if needed.
    if (authToken && config.headers && !config.headers['Authorization']) {
      config.headers['Authorization'] = `Bearer ${authToken}`;
    }
    return config;
  },
  (error: AxiosError) => {
    console.error('Request Error:', error);
    return Promise.reject(error);
  }
);

// Variables to manage the token refresh state
let isRefreshing = false;
let failedQueue: Array<(token: string) => void> = [];

const processQueue = (error: Error | null, token: string | null = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom(error as any);
    } else {
      prom(token as string);
    }
  });
  failedQueue = [];
};

// Response interceptor for handling responses globally
service.interceptors.response.use(
  (response) => {
    const res = response.data;
    // Check if the response is a standard HttpResult wrapper
    if (typeof res === 'object' && res !== null && 'success' in res) {
      if (res.success) {
        // For successful wrapped responses, return the 'data' payload
        return res;
      } else {
        // For failed wrapped responses, show a message and reject the promise
        ElMessage({
          message: res.message || 'Error',
          type: 'error',
          duration: 5 * 1000,
        });
        return Promise.reject(new Error(res.message || 'Error'));
      }
    }
    // If it's not a wrapped response, return the data directly
    return res;
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise(function (resolve, _reject) {
          failedQueue.push((token) => {
            if (originalRequest.headers) {
              originalRequest.headers['Authorization'] = 'Bearer ' + token;
            }
            resolve(axios(originalRequest));
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const authStore = useAuthStore();
      const currentRefreshToken = authStore.refreshToken;

      if (!currentRefreshToken) {
        authStore.logout();
        ElMessage.error('会话已过期，请重新登录。');
        location.reload();
        return Promise.reject(error);
      }

      try {
        const newTokens = await apiRefreshToken(currentRefreshToken);
        authStore.setTokens(newTokens);
        setRequestToken(newTokens.id_token);

        if (originalRequest.headers) {
          originalRequest.headers['Authorization'] = `Bearer ${newTokens.id_token}`;
        }

        processQueue(null, newTokens.id_token);
        return service(originalRequest);
      } catch (refreshError) {
        authStore.logout();
        ElMessage.error('会话已过期，请重新登录。');
        location.reload();
        processQueue(refreshError as Error, null);
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    // For all other errors, just display a generic error message
    console.error('Response Error:', error);
    if(error.response?.status !== 401) {
        ElMessage({
            message: (error.response?.data as any)?.message || error.message || '服务器发生未知错误',
            type: 'error',
            duration: 5 * 1000,
        });
    }


    return Promise.reject(error);
  }
);

export default service;
