import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";
import { ElMessage } from "element-plus";
import { useAuthStore } from "../store/auth";
import { refreshToken as apiRefreshToken } from "../api/auth";

// Module-level variable to hold the token
let authToken: string | null = null;

export function setRequestToken(token: string | null) {
  authToken = token;
  if (token) {
    service.defaults.headers.common["Authorization"] = "Bearer " + token;
  } else {
    delete service.defaults.headers.common["Authorization"];
  }
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
});

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (authToken) {
      if (config.headers && typeof config.headers.set === 'function') {
        config.headers.set('Authorization', 'Bearer ' + authToken);
      } else if (config.headers) {
        config.headers["Authorization"] = 'Bearer ' + authToken;
      }
    }
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  },
);

let isRefreshing = false;
let failedQueue: Array<(_token: string | null) => void> = [];

const processQueue = (error: Error | null, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom(error as unknown as string);
    } else {
      prom(token as string);
    }
  });
  failedQueue = [];
};

service.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (typeof res === "object" && res !== null && "success" in res) {
      if (res.success) {
        return res.data !== undefined ? res.data : res;
      } else {
        ElMessage({
          message: res.message || "Error",
          type: "error",
          duration: 5 * 1000,
        });
        return Promise.reject(new Error(res.message || "Error"));
      }
    }
    return res;
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise(function (resolve) {
          failedQueue.push((token) => {
            if (originalRequest.headers && typeof originalRequest.headers.set === 'function') {
              originalRequest.headers.set("Authorization", "Bearer " + token);
            } else if (originalRequest.headers) {
              originalRequest.headers["Authorization"] = "Bearer " + token;
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
        ElMessage.error("Session expired, please login again.");
        location.reload();
        return Promise.reject(error);
      }
      try {
        const newTokens = await apiRefreshToken(currentRefreshToken);
        authStore.setTokens(newTokens);
        setRequestToken(newTokens.id_token);
        if (originalRequest.headers && typeof originalRequest.headers.set === 'function') {
          originalRequest.headers.set("Authorization", "Bearer " + newTokens.id_token);
        } else if (originalRequest.headers) {
          originalRequest.headers["Authorization"] = "Bearer " + newTokens.id_token;
        }
        processQueue(null, newTokens.id_token);
        return service(originalRequest);
      } catch (refreshError) {
        authStore.logout();
        ElMessage.error("Session expired, please login again.");
        location.reload();
        processQueue(refreshError as Error, null);
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }
    console.error("Response Error:", error);
    if (error.response?.status !== 401) {
      ElMessage({
        message: (error.response?.data as { message?: string })?.message || error.message || "Unknown server error",
        type: "error",
        duration: 5 * 1000,
      });
    }
    return Promise.reject(error);
  },
);

export default service;