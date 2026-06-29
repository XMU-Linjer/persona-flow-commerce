import http from './http'

export interface LoginRequest {
  identityType: 'USERNAME'
  identifier: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  displayName: string
}

export interface LoginUser {
  id: number
  username: string
  displayName: string
  roles: string[]
}

export interface LoginResult {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: LoginUser
}

export interface RegisterResult {
  userId: number
  username: string
  displayName: string
}

export function login(data: LoginRequest) {
  return http.post<LoginResult, LoginResult>('/api/auth/login', data)
}

export function register(data: RegisterRequest) {
  return http.post<RegisterResult, RegisterResult>('/api/auth/register', data)
}
