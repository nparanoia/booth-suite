import axios from 'axios'
import type { ApiResult } from '@/types/api'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/gw',
  timeout: 20000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('ql_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>
    if (body && typeof body.code === 'number' && body.code !== 0) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response
  },
  (error) => {
    const message =
      error.response?.data?.message ||
      error.message ||
      '网络异常'
    return Promise.reject(new Error(message))
  },
)

export async function getData<T>(url: string, params?: object): Promise<T> {
  const { data } = await http.get<ApiResult<T>>(url, { params })
  return data.data
}

export async function postData<T>(url: string, body?: unknown): Promise<T> {
  const { data } = await http.post<ApiResult<T>>(url, body)
  return data.data
}

export async function putData<T>(url: string, body?: unknown): Promise<T> {
  const { data } = await http.put<ApiResult<T>>(url, body)
  return data.data
}

export async function deleteData<T>(url: string, params?: object): Promise<T> {
  const { data } = await http.delete<ApiResult<T>>(url, {
    params,
    paramsSerializer: {
      // Spring @RequestParam List 需要 ids=1&ids=2
      indexes: null,
    },
  })
  return data.data
}

export default http
