import axios from 'axios'
import { useAuthStore } from '../store/auth'

const client = axios.create({ baseURL: '/' })

client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      useAuthStore.getState().logout()
      window.location.href = '/signin'
    }
    return Promise.reject(error)
  }
)

export default client
