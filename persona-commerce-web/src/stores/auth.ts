import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { login as loginApi, type LoginRequest, type LoginUser } from '@/api/auth'
import { clearToken, getToken, loadUser, saveToken, saveUser } from '@/api/http'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken())
  const user = ref<LoginUser | null>(loadUser<LoginUser>())

  const isLoggedIn = computed(() => Boolean(token.value))
  const displayName = computed(() => user.value?.displayName || user.value?.username || '未登录')

  async function login(payload: LoginRequest) {
    const result = await loginApi(payload)
    token.value = result.accessToken
    user.value = result.user
    saveToken(result.accessToken)
    saveUser(result.user)
  }

  function logout() {
    token.value = null
    user.value = null
    clearToken()
  }

  return {
    token,
    user,
    isLoggedIn,
    displayName,
    login,
    logout,
  }
})
