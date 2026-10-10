import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'

const baseURL = import.meta.env.VITE_API_URL ?? '/api/v1'
export const api = axios.create({ baseURL, withCredentials: true })

let accessToken: string | null = null
let refreshPromise: Promise<string | null> | null = null
let onSessionExpired: (() => void) | null = null

const cookie = (name: string) => document.cookie.split('; ').find((part) => part.startsWith(`${name}=`))?.split('=')[1]
export const setAccessToken = (token: string | null) => { accessToken = token }
export const setSessionExpiredHandler = (handler: () => void) => { onSessionExpired = handler }

api.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  const xsrf = cookie('XSRF-TOKEN')
  if (xsrf) config.headers['X-CSRF-TOKEN'] = decodeURIComponent(xsrf)
  return config
})

api.interceptors.response.use((response) => response, async (error: AxiosError) => {
  const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined
  if (error.response?.status !== 401 || !original || original._retried || original.url?.includes('/auth/refresh') || original.url?.includes('/auth/login')) throw error
  original._retried = true
  refreshPromise ??= api.post('/auth/refresh').then(({ data }) => {
    setAccessToken(data.accessToken)
    return data.accessToken as string
  }).catch(() => { setAccessToken(null); onSessionExpired?.(); return null }).finally(() => { refreshPromise = null })
  const token = await refreshPromise
  if (!token) throw error
  original.headers.Authorization = `Bearer ${token}`
  return api(original)
})

export const errorMessage = (error: unknown) => {
  if (axios.isAxiosError(error)) return error.response?.data?.message ?? 'The request could not be completed.'
  return 'Something went wrong.'
}
