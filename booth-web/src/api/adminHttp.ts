import axios from 'axios'
import type { ApiResult } from '@/types/api'

const adminHttp = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/gw',
  timeout: 20000,
})

adminHttp.interceptors.request.use((config) => {
  const token = localStorage.getItem('ql_admin_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

adminHttp.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>
    if (body && typeof body.code === 'number' && body.code !== 0) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络异常'
    return Promise.reject(new Error(message))
  },
)

export async function adminGet<T>(url: string, params?: object): Promise<T> {
  const { data } = await adminHttp.get<ApiResult<T>>(url, { params })
  return data.data
}

export async function adminPost<T>(url: string, body?: unknown): Promise<T> {
  const { data } = await adminHttp.post<ApiResult<T>>(url, body)
  return data.data
}

export async function adminPut<T>(url: string, body?: unknown): Promise<T> {
  const { data } = await adminHttp.put<ApiResult<T>>(url, body)
  return data.data
}

export async function adminDelete<T>(url: string, params?: object): Promise<T> {
  const { data } = await adminHttp.delete<ApiResult<T>>(url, { params })
  return data.data
}

export default adminHttp
