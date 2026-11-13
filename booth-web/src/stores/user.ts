import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as memberApi from '@/api/member'
import type { MemberProfile } from '@/types/api'

const TOKEN_KEY = 'ql_token'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const profile = ref<MemberProfile | null>(null)
  const loading = ref(false)

  const loggedIn = computed(() => Boolean(token.value))
  const displayName = computed(
    () => profile.value?.nickname || profile.value?.username || '本店顾客',
  )

  function setToken(value: string) {
    token.value = value
    if (value) localStorage.setItem(TOKEN_KEY, value)
    else localStorage.removeItem(TOKEN_KEY)
  }

  async function login(username: string, password: string) {
    const data = await memberApi.login(username, password)
    setToken(data.token)
    await refreshProfile()
  }

  async function register(payload: {
    username: string
    password: string
    phone: string
    nickname?: string
  }) {
    await memberApi.register(payload)
    await login(payload.username, payload.password)
  }

  async function refreshProfile() {
    if (!token.value) {
      profile.value = null
      return
    }
    loading.value = true
    try {
      profile.value = await memberApi.fetchMe()
    } catch {
      setToken('')
      profile.value = null
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    try {
      if (token.value) await memberApi.logout()
    } catch {
      /* ignore */
    }
    setToken('')
    profile.value = null
    localStorage.removeItem('ql_support_cid')
  }

  return {
    token,
    profile,
    loading,
    loggedIn,
    displayName,
    login,
    register,
    refreshProfile,
    logout,
    setToken,
  }
})
