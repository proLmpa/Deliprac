import client from './client'

export interface RegisterRequest {
  email: string
  password: string
  role: 'CUSTOMER' | 'OWNER'
  phone?: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface TokenResponse {
  accessToken: string
  tokenType: string
}

export const signup = (data: RegisterRequest) =>
  client.post<{ id: number }>('/api/users/signup', data)

export const signin = (data: LoginRequest) =>
  client.post<TokenResponse>('/api/users/signin', data)
